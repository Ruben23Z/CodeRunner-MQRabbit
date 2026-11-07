package runserver;

import redis.clients.jedis.RedisClient;

import java.util.Map;

/**
 * Acesso ao REDIS (ligação d). Hash pedido:<uuid> com os campos:
 * estado, servidor (ip:porto), codigoSaida, resultado (DISPONIVEL|ENTREGUE|EXPIRADO), detalhe.
 * Nunca guarda stdout/stderr.
 */
public class RedisPedidos {

    private static final long TTL_FINAL_SEGUNDOS = 7L * 24 * 3600; // 7 dias após ENTREGUE/EXPIRADO
    private static final int MAX_DETALHE = 200;

    // RedisClient é thread-safe e tem pool interno: uma instância partilhada por todos os handlers/workers
    private final RedisClient redis;

    public RedisPedidos(String ip, int porto) {
        this.redis = RedisClient.create(ip, porto);
    }

    private static String chave(String id) {
        return "pedido:" + id;
    }

    /**
     * Falha (exceção) se o REDIS estiver inacessível: usar no arranque.
     *
     * @return
     */
    public void ping() {
        String resposta = redis.ping();
        if (!"PONG".equals(resposta)) {
            throw new IllegalStateException("REDIS respondeu: " + resposta);
        }
    }

    /**
     * Novo pedido PENDENTE; o dono (servidor) fica gravado desde o início, num único HSET.
     * Sem TTL enquanto o pedido está ativo.
     */
    public void criar(String id, String ip, int porto) {
        redis.hset(chave(id), Map.of("estado", "PENDENTE", "servidor", ip + ":" + porto));
    }

    public void atualizarEstado(String id, String estado) {
        redis.hset(chave(id), "estado", estado);
    }

    /**
     * Estado final, código de saída e resultado=DISPONIVEL num único HSET (atómico).
     */
    public void concluir(String id, String estado, int codigoSaida) {
        concluir(id, estado, codigoSaida, null);
    }

    /**
     * Igual, com um detalhe curto de falha interna (ex.: container não arrancou, watchdog).
     */
    public void concluir(String id, String estado, int codigoSaida, String detalhe) {
        if (detalhe != null && !detalhe.isBlank()) {
            String d = detalhe.length() > MAX_DETALHE ? detalhe.substring(0, MAX_DETALHE) : detalhe;
            redis.hset(chave(id), Map.of("estado", estado, "codigoSaida", String.valueOf(codigoSaida),
                    "resultado", "DISPONIVEL", "detalhe", d));
        } else {
            redis.hset(chave(id), Map.of("estado", estado, "codigoSaida", String.valueOf(codigoSaida),
                    "resultado", "DISPONIVEL"));
        }
    }

    /**
     * Chamar só depois do onCompleted da entrega.
     */
    public void marcarEntregue(String id) {
        redis.hset(chave(id), "resultado", "ENTREGUE");
        redis.expire(chave(id), TTL_FINAL_SEGUNDOS);
    }

    /**
     * Só passa a EXPIRADO se ainda estava DISPONIVEL (se já era ENTREGUE, fica como está).
     */
    public void marcarExpirado(String id) {
        if ("DISPONIVEL".equals(redis.hget(chave(id), "resultado"))) {
            redis.hset(chave(id), "resultado", "EXPIRADO");
            redis.expire(chave(id), TTL_FINAL_SEGUNDOS);
        }
    }

    /**
     * Devolve os campos do pedido; mapa vazio se não existir.
     */
    public Map<String, String> obter(String id) {
        return redis.hgetAll(chave(id));
    }

    /**
     * Usado quando a fila está cheia: o pedido nunca chegou a existir.
     */
    public void apagar(String id) {
        redis.del(chave(id));
    }

    public void fechar() {
        redis.close();
    }
}