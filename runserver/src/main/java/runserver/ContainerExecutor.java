package runserver;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class ContainerExecutor {

    private static final String IMAGEM = "python:3.12-slim";
    private static final long MEMORIA = 256L * 1024 * 1024;
    private static final String COMANDO = "timeout 10 python /work/prog.py < /work/input.txt > /work/out.txt 2> /work/err.txt;" + " echo $? > /work/exit.txt";

    private final DockerClient docker;
    private final long CPU = 500_000_000L;// 0,5 CPU
    public ContainerExecutor(DockerClient docker) {
        this.docker = docker;
    }

    public Resultado executar(Path pasta, int porto, String uuid) {
        String nome = "run-" + porto + "-" + uuid;
        String containerId = null;
        String detalhe = null;
        try {

            HostConfig hc = HostConfig.newHostConfig().withBinds(new Bind(pasta.toAbsolutePath().toString(), new Volume("/work"))).withNetworkMode("none").withMemory(MEMORIA).withMemorySwap(MEMORIA).withNanoCPUs(CPU);

            var create = docker.createContainerCmd(IMAGEM).withName(nome).withHostConfig(hc).withCmd("sh", "-c", COMANDO);

            String user = utilizadorAtual();
            if (user != null) create.withUser(user);

            containerId = create.exec().getId();
            docker.startContainerCmd(containerId).exec();

            boolean terminou = docker.waitContainerCmd(containerId).exec(new WaitContainerResultCallback()).awaitCompletion(30, TimeUnit.SECONDS);
            if (!terminou) detalhe = "container não terminou em 30 s";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            detalhe = "interrompido";
            System.err.println("[" + uuid + "] " + detalhe);
        } catch (Exception e) {
            detalhe = "falha no container: " + e.getMessage();
            System.err.println("[" + uuid + "] " + detalhe);
        } finally {
            if (containerId != null) {
                try {
                    docker.removeContainerCmd(containerId).withForce(true).exec(); // force mata se ainda corre
                } catch (Exception e) {
                    System.err.println("[" + uuid + "] erro a remover container: " + e.getMessage());
                }
            }
        }

        int codigo = EstadoExecucao.lerCodigo(pasta.resolve("exit.txt"));
        return new Resultado(EstadoExecucao.decCodigo(codigo), codigo, detalhe);


    }

    /**
     * uid:gid em Linux/macOS; null em Windows.
     */
    private static String utilizadorAtual() {
        if (System.getProperty("os.name").startsWith("Windows")) return null;
        var unix = new com.sun.security.auth.module.UnixSystem();
        return unix.getUid() + ":" + unix.getGid();
    }

}
