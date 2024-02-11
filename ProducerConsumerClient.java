import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import producerconsumer.ProducerConsumerGrpc;
import producerconsumer.ProduceRequest;
import producerconsumer.ProduceResponse;
import producerconsumer.ConsumeRequest;
import producerconsumer.ConsumeResponse;

import java.util.concurrent.TimeUnit;

public class ProducerConsumerClient {
    private final ManagedChannel channel;
    private final ProducerConsumerGrpc.ProducerConsumerBlockingStub blockingStub;

    public ProducerConsumerClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = ProducerConsumerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void produce(String item) {
        ProduceRequest request = ProduceRequest.newBuilder().setItem(item).build();
        ProduceResponse response = blockingStub.produce(request);
        System.out.println("Server response: " + response.getMessage());
    }

    public void consume() {
        ConsumeRequest request = ConsumeRequest.newBuilder().build();
        ConsumeResponse response = blockingStub.consume(request);
        System.out.println("Consumed item: " + response.getItem());
    }

    public static void main(String[] args) throws Exception {
        ProducerConsumerClient client = new ProducerConsumerClient("localhost", 50051);
        try {
            for (int i = 0; i < 5; i++) {
                client.produce("Item " + i);
                Thread.sleep(1000);
            }
            for (int i = 0; i < 5; i++) {
                client.consume();
                Thread.sleep(1000);
            }
        } finally {
            client.shutdown();
        }
    }
}
