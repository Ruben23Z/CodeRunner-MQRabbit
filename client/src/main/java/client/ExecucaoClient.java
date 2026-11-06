package client;

import com.google.protobuf.ByteString;
import execucaostubs.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ExecucaoClient {
    private static final int BLOCO = 64 * 1024;

    private final ManagedChannel canal;
    private final ExecucaoServiceGrpc.ExecucaoServiceBlockingStub blocking;
    private final ExecucaoServiceGrpc.ExecucaoServiceStub noBlock;

    public ExecucaoClient(String ip, int porto) {
        canal = ManagedChannelBuilder.forAddress(ip, porto).usePlaintext().build();
        blocking = ExecucaoServiceGrpc.newBlockingStub(canal);
        noBlock = ExecucaoServiceGrpc.newStub(canal);
    }

    // Observer da resposta de submeter (mesma ideia do EvenNumbersStream dos slides)
    private static class RespostaSubmeter implements StreamObserver<PedidoId> {
        final Semaphore terminou = new Semaphore(0);
        String id;
        Throwable erro;

        @Override
        public void onNext(PedidoId pedidoId) {
            id = pedidoId.getId();
            terminou.release();
        }

        @Override
        public void onError(Throwable throwable) {
            erro = throwable;
            terminou.release();
        }

        @Override
        public void onCompleted() {
        }
    }

    /**
     * Devolve o UUID do pedido. Erros do servidor saem como StatusRuntimeException.
     */
    public String submeter(byte[] programa, byte[] input) throws InterruptedException {
        RespostaSubmeter resposta = new RespostaSubmeter();
        StreamObserver<BlocoSubmissao> envio = noBlock.withDeadlineAfter(30, TimeUnit.SECONDS).submeter(resposta);

        enviarBlocos(envio, programa, true);
        if (input != null && input.length > 0) {
            enviarBlocos(envio, input, false);
        }
        envio.onCompleted();

        // espera pela resposta do servidor (onNext ou onError)
        if (!resposta.terminou.tryAcquire(30, TimeUnit.SECONDS)) {
            throw Status.DEADLINE_EXCEEDED.withDescription("sem resposta do servidor").asRuntimeException();
        }
        if (resposta.erro != null) {
            throw Status.fromThrowable(resposta.erro).asRuntimeException();
        }
        return resposta.id;
    }

    private void enviarBlocos(StreamObserver<BlocoSubmissao> envio, byte[] dados, boolean ePrograma) {
        for (int inicio = 0; inicio < dados.length; inicio += BLOCO) {
            int tamanho = Math.min(BLOCO, dados.length - inicio);
            ByteString bytes = ByteString.copyFrom(dados, inicio, tamanho);
            if (ePrograma) {
                envio.onNext(BlocoSubmissao.newBuilder().setPrograma(bytes).build());
            } else {
                envio.onNext(BlocoSubmissao.newBuilder().setInput(bytes).build());
            }
        }
    }

    /**
     * Escreve stdout/stderr nos streams recebidos e devolve o InfoPedido (1.ª mensagem).
     */
    public InfoPedido consultar(String id, OutputStream out, OutputStream err) throws IOException {
        PedidoId pedido = PedidoId.newBuilder().setId(id).build();
        Iterator<BlocoResultado> blocos = blocking.withDeadlineAfter(30, TimeUnit.SECONDS).obterResultado(pedido);

        InfoPedido info = null;
        while (blocos.hasNext()) {
            BlocoResultado bloco = blocos.next();
            if (bloco.getConteudoCase() == BlocoResultado.ConteudoCase.INFO) {
                info = bloco.getInfo();
            } else if (bloco.getConteudoCase() == BlocoResultado.ConteudoCase.OUT) {
                out.write(bloco.getOut().toByteArray());
            } else if (bloco.getConteudoCase() == BlocoResultado.ConteudoCase.ERR) {
                err.write(bloco.getErr().toByteArray());
            }
        }
        out.flush();
        err.flush();
        return info;
    }

    public void fechar() {
        canal.shutdownNow();
    }
}