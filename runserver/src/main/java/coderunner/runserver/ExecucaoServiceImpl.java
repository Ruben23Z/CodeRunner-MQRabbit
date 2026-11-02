package coderunner.runserver;

import execucaostubs.ExecucaoServiceGrpc;
import io.grpc.Status;

import java.util.concurrent.CountDownLatch;

public class ExecucaoServiceImpl extends ExecucaoServiceGrpc.ExecucaoServiceImplBase {


    private final CountDownLatch pronto = new CountDownLatch(1);
    private volatile RedisPedidos redis;

    public void libertar(RedisPedidos redis) {
        this.redis = redis;
        pronto.countDown();
    }

    private void esperarPronto() {
        try {
            pronto.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Status.UNAVAILABLE.withDescription("servidor a arrancar").asRuntimeException();
        }
    }


}