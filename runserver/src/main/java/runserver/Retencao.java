package runserver;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

public class Retencao {

    private final Path raiz;
    private final Duration maxIdade;
    private final Duration periodo;
    private Set<String> claims;
    private RedisPedidos redisPedidos;

    public Retencao(Path raiz, Duration maxIdade, Duration periodo, Set<String> claims, RedisPedidos redis) {


        this.raiz = raiz;
        this.maxIdade = maxIdade;
        this.periodo = periodo;
        this.claims = claims;
        this.redisPedidos = redis;

    }


    public void iniciar() {
        var agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        agendador.scheduleWithFixedDelay(() -> {
            try {
                limpar();
            } catch (Throwable t) {
                System.err.println("Retenção: " + t);
            }
        }, periodo.toMillis(), periodo.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void limpar() throws IOException {
        if (!Files.isDirectory(raiz)) return;
        long limite = System.currentTimeMillis() - maxIdade.toMillis();
        try (var pastas = Files.list(raiz)) {
            for (Path pasta : pastas.toList()) {
                if (pasta.toFile().lastModified() >= limite) continue;
                String id = pasta.getFileName().toString();
                try {
                    UUID.fromString(id);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!claims.add(id)) continue;          // entrega em curso: salta
                try {
                    apagar(pasta);
                    redisPedidos.marcarExpirado(id);
                } finally {
                    claims.remove(id);
                }
            }
        }
    }

    public static void apagar(Path pasta) {
        if (!Files.exists(pasta)) return;
        try (var arvore = Files.walk(pasta)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            System.err.println("Erro a apagar " + pasta + ": " + e.getMessage());
        }
    }

}