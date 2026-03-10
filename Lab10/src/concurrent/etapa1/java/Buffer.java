import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class Buffer {

    private final BlockingQueue<Integer> data = new ArrayBlockingQueue<>(1000);

    public void put(int value) {
        try {
            data.put(value);
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int remove() {
        try {
            int value = data.take();
            System.out.println("Removed: " + value + " | Buffer size: " + data.size());
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return -1;
    }
}