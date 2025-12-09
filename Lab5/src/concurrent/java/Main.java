public class Main {
    public static void main(String[] args) {
        if (args.length != 5) {
            System.out.println("Use: java Main <num_producers> <max_items_per_producer> <producing_time> <num_consumers> <consuming_time>");
            return;
        }
        
        int numProducers = Integer.parseInt(args[0]);
        int maxItemsPerProducer = Integer.parseInt(args[1]);
        int producingTime = Integer.parseInt(args[2]);
        int numConsumers = Integer.parseInt(args[3]);
        int consumingTime = Integer.parseInt(args[4]);
        
        Buffer buffer = new Buffer();
        
        for (int i = 1; i <= numProducers; i++) {
            ProducerService prodService = new ProducerService(i, buffer, maxItemsPerProducer, producingTime);
            Thread tProducer = new Thread(prodService, Integer.toString(i));
            tProducer.start();
        }
        
        for (int i = 1; i <= numConsumers; i++) {
            ConsumerService consumerService = new ConsumerService(i, buffer, consumingTime);
            Thread tConsumer = new Thread(consumerService, Integer.toString(i));
            tConsumer.start();
        }
    }

    public static class ProducerService implements Runnable {
        private final int i;
        private final Buffer buffer;
        private final int maxItemsPerProducer;
        private final int producingTime;

        public ProducerService(int index, Buffer buf, int maxItems, int prodTime) {
            this.i = index;
            this.buffer = buf;
            this.maxItemsPerProducer = maxItems;
            this.producingTime = prodTime;
        }

        @Override
        public void run() {
            Producer producer = new Producer(i, buffer, maxItemsPerProducer, producingTime);
            producer.produce();
        }
    }

    public static class ConsumerService implements Runnable {
        private final int i;
        private final Buffer buffer;
        private final int consumingTime;

        public ConsumerService(int index, Buffer buf, int consTime) {
            this.i = index;
            this.buffer = buf;
            this.consumingTime = consTime;
        }

        @Override
        public void run() {
            Consumer consumer = new Consumer(i, buffer, consumingTime);
            consumer.process();
        }
    }
}
