package manager;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import registostubs.RedisInfo;
import registostubs.RegistoServiceGrpc;
import registostubs.RunServerInfo;

public class RegistoServiceImpl extends RegistoServiceGrpc.RegistoServiceImplBase {
    private final ServerRegistry registry;
    private final String redisIp;
    private final int redisPorto;


    public RegistoServiceImpl(ServerRegistry registry, String redisIp, int redisPorto) {
        this.registry = registry;
        this.redisIp = redisIp;
        this.redisPorto = redisPorto;
    }


    @Override
    public void registar(RunServerInfo request, StreamObserver<RedisInfo> responseObserver) {

        String ip = request.getIp();
        int porto = request.getPorto();

        if (ip.isBlank() || porto < 1 || porto > 65535) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ip vazio ou porto fora de 1-65535").asRuntimeException());
            return;
        }

        if (registry.add(ip, porto))
            System.out.println(("Registado: " + ip + ":" + porto + " (total " + registry.size() + ")"));
        else System.out.println(("Já registado: " + ip + ":" + porto + " (total " + registry.size() + ")"));

        RedisInfo ri = RedisInfo.newBuilder().setIp(redisIp).setPorto(redisPorto).build();

        responseObserver.onNext(ri);
        responseObserver.onCompleted();
    }
}