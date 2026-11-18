package costco;

// CheckoutQueueTest.java
// Standalone concurrency test for CheckoutQueue. Uses a bare producer
// thread (stands in for Customer) and a bare consumer thread (stands in
// for Cashier) so it does not depend on those classes.
//
// Sleep ranges are chosen so both blocking cases actually happen:
//   * Customers 1-4 arrive quickly while the cashier is slow, so the
//     2-person line fills up and the producer must block (line full).
//   * Customers 5-6 arrive slowly (3+ s apart), so the cashier empties
//     the line and must block waiting for the next customer (line empty).
// The cashier takes a customer first, then spends time ringing them up.
// A put/take that takes noticeably long is reported as BLOCKED.
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckoutQueueTest {
    private static final SecureRandom generator = new SecureRandom();
    private static final int CAPACITY = 2;
    private static final int CUSTOMERS = 6;
    private static final long BLOCKED_THRESHOLD_MS = 50; // longer = it blocked

    public static void main(String[] args) throws InterruptedException {
        CheckoutQueue queue = new CheckoutQueue(CAPACITY);
        List<Integer> served = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger producerBlocks = new AtomicInteger();
        AtomicInteger consumerBlocks = new AtomicInteger();

        System.out.printf("Checkout line capacity: %d, customers: %d%n%n",
                queue.capacity(), CUSTOMERS);

        Runnable producer = () -> {
            Thread.currentThread().setName("Customers");
            try {
                for (int id = 1; id <= CUSTOMERS; id++) {
                    // fast arrivals first, then slow arrivals
                    int sleep = id <= 4 ? generator.nextInt(200)
                            : 3000 + generator.nextInt(500);
                    Thread.sleep(sleep); // "shopping"
                    long start = System.nanoTime();
                    queue.joinQueue(id);
                    long waited = elapsedMs(start);
                    if (waited > BLOCKED_THRESHOLD_MS) {
                        producerBlocks.incrementAndGet();
                        System.out.printf("%-12s BLOCKED %d ms: line was full "
                                + "before Customer %d could join%n",
                                "Customers", waited, id);
                    }
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable consumer = () -> {
            Thread.currentThread().setName("Cashier");
            try {
                for (int i = 1; i <= CUSTOMERS; i++) {
                    long start = System.nanoTime();
                    int id = queue.serveNextCustomer();
                    long waited = elapsedMs(start);
                    served.add(id);
                    if (waited > BLOCKED_THRESHOLD_MS) {
                        consumerBlocks.incrementAndGet();
                        System.out.printf("%-12s BLOCKED %d ms: line was empty "
                                + "until Customer %d arrived%n",
                                "Cashier", waited, id);
                    }
                    Thread.sleep(800 + generator.nextInt(400)); // "ringing up"
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        ExecutorService executor = Executors.newCachedThreadPool();
        executor.execute(producer);
        executor.execute(consumer);
        executor.shutdown();
        boolean finished = executor.awaitTermination(1, TimeUnit.MINUTES);

        // verify results
        List<Integer> expected = new ArrayList<>();
        for (int id = 1; id <= CUSTOMERS; id++) {
            expected.add(id);
        }
        boolean fifo = served.equals(expected);
        boolean ok = finished && fifo && queue.isEmpty()
                && producerBlocks.get() > 0 && consumerBlocks.get() > 0;

        System.out.printf("%nFinished in time:         %b%n", finished);
        System.out.printf("Served order:             %s%n", served);
        System.out.printf("All served in FIFO order: %b%n", fifo);
        System.out.printf("Line empty at end:        %b (size %d)%n",
                queue.isEmpty(), queue.size());
        System.out.printf("Producer blocked (full):  %d time(s)%n",
                producerBlocks.get());
        System.out.printf("Consumer blocked (empty): %d time(s)%n",
                consumerBlocks.get());
        System.out.println(ok ? "RESULT: PASS" : "RESULT: FAIL");

        if (!ok) {
            System.exit(1);
        }
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
