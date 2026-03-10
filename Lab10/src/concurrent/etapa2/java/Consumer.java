class Consumer {
    private final Buffer buffer;
    private final int sleepTime;
    private final int id;
    
    public Consumer(int id, Buffer buffer, int sleepTime) {
        this.id = id;
        this.buffer = buffer;
        this.sleepTime = sleepTime;
    }
    
    public void process() {
        while (true) {
            Integer item = buffer.remove();
            if (item == null) {
                System.out.println("Consumer " + id + " tried to consume item but did not find any after 600ms. EXITING");
                break;
            };

            System.out.println("Consumer " + id + " consumed item " + item);

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}