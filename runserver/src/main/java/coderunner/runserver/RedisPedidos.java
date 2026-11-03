package coderunner.runserver;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

public class RedisPedidos {
    private final JedisPool pool;

    public RedisPedidos(String ip, int porto) {
        pool = new JedisPool(ip, porto);
    }

    private static String chave(String id) {
        return "pedido:" + id;
    }

    /** Novo pedido com estado PENDENTE; ip:porto é o RunServer que o vai executar. */
    public void criar(String id, String ip, int porto) {
        try (Jedis j = pool.getResource()) {
            j.hset(chave(id), Map.of(
                    "estado", "PENDENTE",
                    "ip", ip,
                    "servidor", ip + ":" + porto));
            j.expire(chave(id), 86400);

        }

    }

    public void atualizarEstado(String id, String estado) {
        try (Jedis j = pool.getResource()) {
            j.hset(chave(id), "estado", estado);
        }
    }

    /** Devolve os campos do pedido; mapa vazio se não existir. */
    public Map<String, String> obter(String id) {
        try (Jedis j = pool.getResource()) {
            return j.hgetAll(chave(id));
        }
    }

    public void fechar() {
        pool.close();
    }
    public void concluir(String id, String estado, String codigoSaida){
        try (Jedis j = pool.getResource()) {
            j.hset(chave(id), Map.of(
                    "estado", estado,
                    "codigoSaida", String.valueOf(codigoSaida)));
        }
    }


}