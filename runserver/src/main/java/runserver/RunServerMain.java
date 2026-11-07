package runserver;

import com.github.dockerjava.api.DockerClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import registostubs.RedisInfo;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RunServerMain {
    private static final String IMAGEM = "python:3.12-slim";

    public static void main(String[] args) throws Exception {
        RunServerConfig cfg;
        RunServerMain rsm = new RunServerMain();
        try {
            cfg = RunServerConfig.fromArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
            System.err.println("Uso: RunServerMain <ip> <porto> <managerIp> <managerPorto> [N=2] [Q=100]");
            System.exit(1);
            return;
        }


        // Docker e imagem têm de estar OK antes de aceitar pedidos
        DockerUtil dockerUtil = new DockerUtil();
        DockerClient docker = dockerUtil.criarCliente();

        try {
            dockerUtil.verificar(docker, IMAGEM);
        } catch (IllegalStateException e) {
            rsm.terminarComErro(e.getMessage(), null);
        }


        // Começar a ouvir (os pedidos esperam até o servidor estar pronto)
        ExecucaoServiceImpl servico = new ExecucaoServiceImpl();
        Server server = ServerBuilder.forPort(cfg.getPorto()).addService(servico).build().start();
        System.out.println("RunServer a ouvir em " + cfg.endereco());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            try {
                server.awaitTermination();
            } catch (InterruptedException ignored) {
                server.shutdown();
            }
        }));


        // Registar no Manager e receber o endereço do REDIS
        RegistoManagerClient registo = new RegistoManagerClient(cfg.getManagerIp(), cfg.getManagerPorto());
        // Ligar ao REDIS
        RedisInfo redis;
        try {
            redis = registo.registar(cfg.getIp(), cfg.getPorto());
            System.out.println("Registado no Manager. REDIS em " + redis.getIp() + ":" + redis.getPorto());

        } catch (IllegalStateException e) {
            rsm.terminarComErro(e.getMessage(), server);
            return;
        } finally {
            registo.fechar();
        }


        // REDIS, órfãos, retenção, pool
        RedisPedidos redisPedidos = new RedisPedidos(redis.getIp(), redis.getPorto());
        try {
            redisPedidos.ping();
            System.out.println("REDIS ligado");

        } catch (Exception e) {
            rsm.terminarComErro("REDIS inacessível em " + redis.getIp() + ":" + redis.getPorto() + " (" + e.getMessage() + ")", server);
            return;
        }

        dockerUtil.removerOrfaos(docker, cfg.getPorto());
        Path pastaPedidos = Path.of("pedidos", String.valueOf(cfg.getPorto())).toAbsolutePath();
        Retencao retencao = new Retencao(pastaPedidos, Duration.ofHours(24), Duration.ofHours(1), servico, redisPedidos);
        retencao.iniciar();


        AtomicInteger n = new AtomicInteger();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(cfg.getWorkers(), cfg.getWorkers(), 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(cfg.getFilaMax()), r -> new Thread(r, "worker-" + n.incrementAndGet()), new ThreadPoolExecutor.AbortPolicy());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            pool.shutdownNow();
            try {
                pool.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            redisPedidos.fechar();
            try {
                docker.close();
            } catch (Exception ignored) {
            }
        }));

        servico.arrancar(redisPedidos, docker, pool, pastaPedidos, cfg);
        System.out.println("RunServer pronto (N=" + cfg.getWorkers() + ", Q=" + cfg.getFilaMax() + ")");
        server.awaitTermination();
    }

    private void terminarComErro(String mensagem, Server server) {
        System.err.println("Erro: " + mensagem);
        if (server != null) server.shutdownNow();
        System.exit(1);
    }

}
