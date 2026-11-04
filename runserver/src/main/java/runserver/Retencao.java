package runserver;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.*;

public class Retencao {

    public void iniciar(Path raiz, Duration maxIdade) {
        var agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);            // não impede o servidor de terminar
            return t;
        });
        agendador.scheduleAtFixedRate(() -> limpar(raiz, maxIdade), 1, 1, TimeUnit.MINUTES);
    }

    private void limpar(Path raiz, Duration maxIdade) {
        if (!Files.isDirectory(raiz)) return;
        long limite = System.currentTimeMillis() - maxIdade.toMillis();
        try (var pastas = Files.list(raiz)) {
            pastas.filter(p -> p.toFile().lastModified() < limite).forEach(this::apagar);
        } catch (IOException e) {
            System.err.println("Erro na retenção: " + e.getMessage());
        }
    }

    public void apagar(Path pasta) {
        try (var arvore = Files.walk(pasta)) {
            arvore.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException e) {
            System.err.println("Erro a apagar " + pasta + ": " + e.getMessage());
        }
    }
}