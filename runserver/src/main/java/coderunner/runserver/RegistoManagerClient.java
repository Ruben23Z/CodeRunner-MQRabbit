package coderunner.runserver;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import registostubs.RedisInfo;
import registostubs.RegistoServiceGrpc;
import registostubs.RunServerInfo;

import java.util.concurrent.TimeUnit;

public class RegistoManagerClient {

    private final long PRAZO_TOTAL_MS = 60_000;
    private static final long PAUSA_MS = 2_000;
    private final ManagedChannel canal;
    private final RegistoServiceGrpc.RegistoServiceBlockingStub stub;

    public RegistoManagerClient(String managerip, int managerPorto) {
        canal = ManagedChannelBuilder.forAddress(managerip, managerPorto).usePlaintext().build();
        stub = RegistoServiceGrpc.newBlockingStub(canal);

    }


    public RedisInfo registar(String ip, int porto) throws InterruptedException {
        RunServerInfo info = RunServerInfo.newBuilder().setIp(ip).setPorto(porto).build();
        long inicio = System.currentTimeMillis();
        while (true) {
            try {
                return stub.withDeadlineAfter(5, TimeUnit.SECONDS).registar(info);

            } catch (StatusRuntimeException e) {
                Status.Code codigo = e.getStatus().getCode();
                if (codigo != Status.Code.UNAVAILABLE && codigo != Status.Code.DEADLINE_EXCEEDED)
                    throw new IllegalStateException("Manager recusou o registo: " + e.getStatus(), e);
                if (System.currentTimeMillis() - inicio > PRAZO_TOTAL_MS)
                    throw new IllegalStateException("Manager indisponível durante 60 s", e);

                System.err.println("Manager indisponível, nova tentativa em 2 s");
                Thread.sleep(PAUSA_MS);
            }
        }

    }

    public void fechar() {
        canal.shutdownNow();
    }
}
