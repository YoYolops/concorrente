// Notice how the first 'for' creates a different Runnable lambda for each thread is running
// So it will only alter its corresponding array index. This is different than the previous
//  approach that used a single lambda for multiple threads running

public class PartitionedCounter {

    public static void main(String[] args) throws InterruptedException {
        int numThreads = 4;
        // 1. Create an array with one "slot" per thread.
        // No synchronization is needed on this array because 
        // threads will never touch overlapping indices.
        int[] threadLocalCounts = new int[numThreads];

        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i; // Capture index for the lambda

            Runnable task = () -> {
                for (int j = 0; j < 10_000_000; j++) {
                    // 2. Each thread modifies ONLY its own slot.
                    threadLocalCounts[threadIndex]++;
                }
            };

            // Create and start the thread
            threads[i] = new Thread(task);
            threads[i].start();
        }

        // 3. Wait for all threads to finish
        // The 'join' establishes a "happens-before" relationship, ensuring
        // the main thread sees the final values in the array.
        for (Thread t : threads) {
            t.join();
        }

        // 4. Sum the results
        // This happens in a single thread (main), so no locking is needed here either.
        long totalCount = 0;
        for (int count : threadLocalCounts) {
            totalCount += count;
        }

        System.out.println("Final Count: " + totalCount);
    }
}