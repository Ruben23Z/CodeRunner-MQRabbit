package manager;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ManagerMain {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Uso: ManagerMain <portoManager> <redisIp> <redisPorto>");
            System.exit(1);
        }
        int porto = Integer.parseInt(args[0]);
        String redisIp = args[1];
        int redisPorto = Integer.parseInt(args[2]);
        ServerRegistry registry = new ServerRegistry();
        Server server = ServerBuilder.forPort(porto)
                .addService(new RegistoServiceImpl(registry, redisIp, redisPorto))
                .addService(new ClienteManagerServiceImpl(registry))
                .build().start();

        System.out.println("ManagerServer a ouvir no porto " + porto
                + " (REDIS " + redisIp + ":" + redisPorto + ")");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("A desligar o ManagerServer");
            server.shutdown();
            try { server.awaitTermination(); } catch (InterruptedException ignored) { }

        }));
        server.awaitTermination();
    }
}