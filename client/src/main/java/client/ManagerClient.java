package client;

import clientemanagerstubs.ClienteManagerServiceGrpc;
import clientemanagerstubs.RunServerEndereco;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import registostubs.RegistoServiceGrpc;

import java.util.concurrent.TimeUnit;

public class ManagerClient {
    private final long PRAZO_TOTAL_MS = 60_000;
    private static final long PAUSA_MS = 2_000;
    private final ManagedChannel canal;
    private final ClienteManagerServiceGrpc.ClienteManagerServiceBlockingStub stub;

    public ManagerClient(String ip, int porto) {
        canal = ManagedChannelBuilder.forAddress(ip, porto).usePlaintext().build();
        stub = ClienteManagerServiceGrpc.newBlockingStub(canal);

    }

    public RunServerEndereco obterServidor() throws InterruptedException, StatusRuntimeException {
        StatusRuntimeException ultimo = null;
        for (int i = 0; i < 3; i++) {
            try {
                return stub.withDeadlineAfter(5, TimeUnit.SECONDS).obterServidor(Empty.getDefaultInstance());

            } catch (StatusRuntimeException e) {
                System.out.println("status:" + e.getStatus().getCode());
                System.out.println("tentativa nº:" + i);

                if (e.getStatus().getCode() != Status.Code.UNAVAILABLE) throw e;
                ultimo = e;
                if (i < 2) Thread.sleep(PAUSA_MS);
            }
        }
        throw ultimo;
    }


    public void fechar() {
        canal.shutdownNow();
    }


}
