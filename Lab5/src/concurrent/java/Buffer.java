import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

class Buffer {
    private final List<Integer> data = new ArrayList<>();

    private final Semaphore put_tickets = new Semaphore(50); // Limite de 50 pedido no pdf
    private final Semaphore remove_tickets = new Semaphore(0);
    
    public void put(int value) {
        try {
            put_tickets.acquire();
            data.add(value);
            remove_tickets.release();
            System.out.println("Inserted: " + value + " | Buffer size: " + data.size());
        } catch (InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE PUT LOCK");
        }
    }
    
    public int remove() {
        System.out.println(">> Consumer is awaiting producers");
        try {
            remove_tickets.acquire();
        } catch(InterruptedException e) {
            System.out.println("FAILED TO ACQUIRE REMOVE LOCK");
        }
        if (!data.isEmpty()) {
            int value = data.remove(0);
            put_tickets.release();
            System.out.println("Removed: " + value + " | Buffer size: " + data.size());
            return value;
        }
        return -1;
    }
}
