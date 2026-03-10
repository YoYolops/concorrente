class Producer {
    private final Buffer buffer;
    private final int sleepTime;
    private final int id;
    
    public Producer(int id, Buffer buffer, int sleepTime) {
        this.id = id;
        this.buffer = buffer;
        this.sleepTime = sleepTime;
    }
    
    public void produce() {
        for (int i = 0; i < 10000; i++) {
            try {
                Thread.sleep(sleepTime);
                int item = (int) (Math.random() * 100);
                System.out.println("Producer " + id + " produced item " + item);
                buffer.put(item);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
