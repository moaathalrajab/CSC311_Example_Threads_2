package costco;

// CheckoutQueue.java
// Shared, thread-safe checkout line of customer IDs for the Costco
// checkout simulator. Customer threads (producers) join the line and
// Cashier threads (consumers) serve from it.
//
// Follows the same pattern as ex2.BlockingBuffer: all synchronization is
// delegated to an ArrayBlockingQueue, so there is no manual
// synchronized/wait/notify here.
import java.util.concurrent.ArrayBlockingQueue;

public class CheckoutQueue {
    private final ArrayBlockingQueue<Integer> line; // shared checkout line
    private final int capacity; // maximum number of customers in line

    // create a checkout line that holds at most capacity customers
    public CheckoutQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "capacity must be at least 1, was " + capacity);
        }
        this.capacity = capacity;
        line = new ArrayBlockingQueue<Integer>(capacity, true); // fair (FIFO) waiting threads
    }

    // called by a Customer thread after shopping;
    // blocks while the line is full
    public void joinQueue(int customerId) throws InterruptedException {
        line.put(customerId); // waits for space if the line is full
        System.out.printf("%-12s %-14s %s%d/%d%n",
                Thread.currentThread().getName(),
                "Customer " + customerId, "joined line   | line size: ",
                line.size(), capacity);
    }

    // called by a Cashier thread; blocks while the line is empty;
    // returns the ID of the customer that was served
    public int serveNextCustomer() throws InterruptedException {
        int customerId = line.take(); // waits for a customer if line is empty
        System.out.printf("%-12s %-14s %s%d/%d%n",
                Thread.currentThread().getName(),
                "Customer " + customerId, "served        | line size: ",
                line.size(), capacity);
        return customerId;
    }

    // number of customers currently waiting in line
    public int size() {
        return line.size();
    }

    // true if no customers are waiting
    public boolean isEmpty() {
        return line.isEmpty();
    }

    // maximum number of customers the line can hold
    public int capacity() {
        return capacity;
    }
}
