
    package ex4;

    import java.security.SecureRandom;

    public class Cashier implements Runnable
    {
        private static final SecureRandom generator = new SecureRandom();
        private final Buffer sharedLocation; //reference to shared object

        //constructor
        public Cashier(Buffer sharedLocation)
        {
            this.sharedLocation = sharedLocation;
        }

        //Serve the 10 customers placed in the queue.
        @Override
        public void run() {
            for (int count = 1; count <= 10; count++)
            {
                try
                {
                    int customerId = sharedLocation.blockingGet();
                    System.out.printf("Cashier serves customer %d.%n", customerId);
                    Thread.sleep(generator.nextInt(1000));
                }
                catch (InterruptedException exception)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            System.out.println("All customers have been served.");
        }
    }


