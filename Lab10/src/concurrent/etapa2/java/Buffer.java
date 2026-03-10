import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class Buffer {

    private final BlockingQueue<Integer> data = new ArrayBlockingQueue<>(10_000);

    public void put(int value) {
        try {
            data.put(value);
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Integer remove() {
        try {
            Integer value = data.poll(600, TimeUnit.MILLISECONDS);

            if (value != null) {
                System.out.println("Removed: " + value + " | Buffer size: " + data.size());
            }

            return value;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return null;
    }
}