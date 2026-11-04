package manager;

import clientemanagerstubs.ClienteManagerServiceGrpc;
import clientemanagerstubs.RunServerEndereco;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

public class ClienteManagerServiceImpl extends ClienteManagerServiceGrpc.ClienteManagerServiceImplBase {


    private final ServerRegistry registry;
    public ClienteManagerServiceImpl(ServerRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void obterServidor(Empty req, StreamObserver<RunServerEndereco> resp){
        Endereco e = registry.next();
        if (e == null) {
            resp.onError(Status.UNAVAILABLE
                    .withDescription("sem RunServers registados")
                    .asRuntimeException());
            return;
        }
        System.out.println("obterServidor -> " + e.ip() + ":" + e.porto());
        RunServerEndereco rse = RunServerEndereco.newBuilder().setIp(e.ip()).setPorto(e.porto()).build();
        resp.onNext(rse);
        resp.onCompleted();

    }
}
