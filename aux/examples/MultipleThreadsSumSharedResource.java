// Here, multiple threads share the same data (SharedData class)
// We need to wrap the data because java needs to be sure the data being passed is not changed.
// So, instead of using a simple int variable, that would throw a compile error due to being altered
// we use a class which points to the data, and not the data itseld. The class pointer does not change
// therefore the java compiler is happy

import java.util.concurrent.Semaphore;

public class SemaphoreCounter {
    
    // 1. The shared variable (not atomic, just a plain int)
    // We use a static class wrapper so we can modify it inside the lambda
    static class SharedData {
        int count = 0;
    }

    public static void main(String[] args) throws InterruptedException {
        SharedData data = new SharedData();

        // 2. Create a Semaphore with exactly 1 permit.
        // "true" in the constructor turns on "Fairness", meaning threads 
        // acquire the lock in the order they requested it (FIFO).
        Semaphore semaphore = new Semaphore(1, true);

        Runnable countingTask = () -> {
            for (int i = 0; i < 10_000; i++) {
                try {
                    // 3. Acquire the permit
                    // If another thread has it, this thread blocks (waits) here.
                    semaphore.acquire();

                    // --- CRITICAL SECTION START ---
                    // Only one thread can be here at a time
                    data.count++;
                    // --- CRITICAL SECTION END ---

                } catch (InterruptedException e) {
                    // Good practice to restore interrupt status
                    Thread.currentThread().interrupt();
                } finally {
                    // 4. Release the permit
                    // CRITICAL: Always release in a 'finally' block.
                    // If the code crashes above, the lock must still be released
                    // or the program will hang forever.
                    semaphore.release();
                }
            }
        };

        // Java 21: Using Thread.ofVirtual() for lightweight threads
        // You can also use 'new Thread(countingTask)' for standard OS threads
        Thread t1 = Thread.ofVirtual().start(countingTask);
        Thread t2 = Thread.ofVirtual().start(countingTask);

        t1.join();
        t2.join();

        System.out.println("Final Count: " + data.count);
    }
}