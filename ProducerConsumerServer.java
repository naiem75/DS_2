import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import producerconsumer.ProducerConsumerGrpc;
import producerconsumer.ProduceRequest;
import producerconsumer.ProduceResponse;
import producerconsumer.ConsumeRequest;
import producerconsumer.ConsumeResponse;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProducerConsumerServer {
    private Server server;
    private BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new ProducerConsumerImpl())
                .build()
                .start();
        System.out.println("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                ProducerConsumerServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private class ProducerConsumerImpl extends ProducerConsumerGrpc.ProducerConsumerImplBase {
        @Override
        public void produce(ProduceRequest request, StreamObserver<ProduceResponse> responseObserver) {
            String item = request.getItem();
            try {
                queue.put(item);
                responseObserver.onNext(ProduceResponse.newBuilder().setMessage("Item produced: " + item).build());
                responseObserver.onCompleted();
            } catch (InterruptedException e) {
                responseObserver.onError(e);
            }
        }

        @Override
        public void consume(ConsumeRequest request, StreamObserver<ConsumeResponse> responseObserver) {
            try {
                String item = queue.take();
                responseObserver.onNext(ConsumeResponse.newBuilder().setItem(item).build());
                responseObserver.onCompleted();
            } catch (InterruptedException e) {
                responseObserver.onError(e);
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final ProducerConsumerServer server = new ProducerConsumerServer();
        server.start();
        server.blockUntilShutdown();
    }
}
