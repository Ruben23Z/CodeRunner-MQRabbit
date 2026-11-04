package manager;


import clientemanagerstubs.ClienteManagerServiceGrpc;
import clientemanagerstubs.RunServerEndereco;
import com.google.protobuf.Empty;
import io.grpc.*;
import registostubs.RegistoServiceGrpc;
import registostubs.RunServerInfo;

import java.util.ArrayList;
import java.util.List;

public class ManagerF2Test {
    static int falhas = 0;

    public static void main(String[] args) throws Exception {
        ServerRegistry registry = new ServerRegistry();
        Server server = ServerBuilder.forPort(0) // porto livre escolhido pelo SO
                .addService(new RegistoServiceImpl(registry, "localhost", 6379))
                .addService(new ClienteManagerServiceImpl(registry))
                .build().start();
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", server.getPort()).usePlaintext().build();
        var registo = RegistoServiceGrpc.newBlockingStub(canal);
        var cliente = ClienteManagerServiceGrpc.newBlockingStub(canal);

        try {
            // 1. sem registos
            esperaCodigo("obterServidor sem registos", Status.Code.UNAVAILABLE,
                    () -> cliente.obterServidor(Empty.getDefaultInstance()));

            // 2. registos inválidos (o total tem de continuar a 0)
            esperaCodigo("porto 0", Status.Code.INVALID_ARGUMENT, () -> registo.registar(info("10.0.0.1", 0)));
            esperaCodigo("porto 70000", Status.Code.INVALID_ARGUMENT, () -> registo.registar(info("10.0.0.1", 70000)));
            esperaCodigo("ip vazio", Status.Code.INVALID_ARGUMENT, () -> registo.registar(info("", 8000)));
            check("inválidos não foram registados", registry.size() == 0);

            // 3. 3 registos, 6 chamadas: 2 de cada, por ordem
            registo.registar(info("10.0.0.1", 8001));
            registo.registar(info("10.0.0.2", 8002));
            registo.registar(info("10.0.0.3", 8003));
            List<String> obtidos = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                RunServerEndereco e = cliente.obterServidor(Empty.getDefaultInstance());
                obtidos.add(e.getIp() + ":" + e.getPorto());
            }
            check("round-robin " + obtidos, obtidos.equals(List.of(
                    "10.0.0.1:8001", "10.0.0.2:8002", "10.0.0.3:8003",
                    "10.0.0.1:8001", "10.0.0.2:8002", "10.0.0.3:8003")));

            // 4. registo repetido (idempotente)
            registo.registar(info("10.0.0.2", 8002));
            check("registo repetido não duplica (total=" + registry.size() + ")", registry.size() == 3);
        } finally {
            canal.shutdownNow();
            server.shutdownNow();
        }
        System.out.println(falhas == 0 ? "\nTUDO OK" : "\n" + falhas + " FALHA(S)");
        System.exit(falhas == 0 ? 0 : 1);
    }

    static RunServerInfo info(String ip, int porto) {
        return RunServerInfo.newBuilder().setIp(ip).setPorto(porto).build();
    }

    // a chamada tem de falhar com o código gRPC esperado
    static void esperaCodigo(String nome, Status.Code esperado, Runnable chamada) {
        try {
            chamada.run();
            check(nome + " (não lançou erro)", false);
        } catch (StatusRuntimeException e) {
            check(nome + " -> " + e.getStatus().getCode(), e.getStatus().getCode() == esperado);
        }
    }

    static void check(String nome, boolean ok) {
        System.out.println((ok ? "OK     " : "FALHA  ") + nome);
        if (!ok) falhas++;
    }
}