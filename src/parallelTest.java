/**
 * Load and parallelism test. Counts the prime numbers below LIMIT, a CPU-heavy task
 * that needs almost no memory, using 1, 2, 4 ... threads, and times each run with
 * System.nanoTime(). The total amount of work is the same for every thread count,
 * so if the threads really run on different cores at the same time, the time drops.
 *
 * The numbers are split into blocks of BLOCK numbers, and the blocks are dealt out
 * to the threads in turn like cards: with 4 threads, thread 0 gets blocks 0, 4, 8 ...,
 * thread 1 gets blocks 1, 5, 9 ... and so on. Big numbers take longer to check than
 * small ones, so giving each thread one long range (0 to N/4, N/4 to N/2 ...) would
 * leave the last thread with far more work than the first. Taking turns gives every
 * thread about the same mix of small and big numbers.
 *
 * Speedup = time with 1 thread / time with n threads. Perfect would be n, up to the
 * number of cores. Every run must find the same number of primes, which checks that
 * the work was split correctly.
 *
 * Started from the main menu (option 7). Best run on alderaan or a Linux VM.
 */
public class parallelTest {
    /** Count the primes below this number. */
    private static final int LIMIT = 5000000;

    /** How many numbers are in each block that is handed to a thread. */
    private static final int BLOCK = 10000;

    /** Each thread count is timed this many times and the fastest time is kept. */
    private static final int REPEATS = 3;

    /** Longest bar in the speedup chart. */
    private static final int BAR_WIDTH = 30;

    /**
     * Runs the test for each thread count, showing progress while it runs, then
     * clears the screen and shows the times, speedups and a speedup chart.
     */
    public static void run() {
        int cores = cpuInfo.getCount();
        if (cores < 1) {
            // no /proc/cpuinfo (not Linux), so ask Java how many processors it can use instead
            cores = Runtime.getRuntime().availableProcessors();
        }
        int[] threadCounts = threadCounts(cores);

        title();
        System.out.println("  " + textChart.DIM + "Counting the primes below " + String.format("%,d", LIMIT)
                + " on 1 to " + threadCounts[threadCounts.length - 1] + " threads (" + cores + " cores)."
                + textChart.RESET);
        System.out.println();

        // the first runs are slower while Java compiles the code, so do a few small runs first and ignore them
        System.out.println("  Warming up...");
        for (int i = 0; i < 3; i++) {
            countPrimes(1, LIMIT / 10);
            countPrimes(cores, LIMIT / 10);
        }

        double[] seconds = new double[threadCounts.length];
        long[] primes = new long[threadCounts.length];
        for (int i = 0; i < threadCounts.length; i++) {
            System.out.printf("  %2d of %d   %2d threads  ", i + 1, threadCounts.length, threadCounts[i]);

            long best = Long.MAX_VALUE;
            for (int r = 0; r < REPEATS; r++) {
                long start = System.nanoTime();
                primes[i] = countPrimes(threadCounts[i], LIMIT);
                long time = System.nanoTime() - start;
                if (time < best) {
                    best = time;
                }
            }
            seconds[i] = best / 1e9;
            System.out.printf("%7.3f s%n", seconds[i]);
        }

        textChart.clearScreen();
        title();
        printResults(threadCounts, seconds, primes, cores);
    }

    /**
     * The thread counts to test: 1, 2, 4 ... up to the number of cores, then the
     * number of cores itself if that wasn't a power of 2, then twice the number of
     * cores to see what happens with more threads than cores.
     */
    private static int[] threadCounts(int cores) {
        int count = 0;
        for (int n = 1; n < cores; n = n * 2) {
            count++;
        }
        count = count + 2; // the number of cores, and twice the number of cores

        int[] counts = new int[count];
        int i = 0;
        for (int n = 1; n < cores; n = n * 2) {
            counts[i] = n;
            i++;
        }
        counts[i] = cores;
        counts[i + 1] = cores * 2;
        return counts;
    }

    /**
     * Counts the primes below 'limit' using 'threads' threads. Starts every thread,
     * waits for all of them to finish, then adds up what each one found.
     */
    public static long countPrimes(int threads, int limit) {
        PrimeCounter[] workers = new PrimeCounter[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new PrimeCounter(t, threads, limit);
            workers[t].start();
        }

        long total = 0;
        for (int t = 0; t < threads; t++) {
            try {
                workers[t].join(); // wait for this thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            total += workers[t].getCount();
        }
        return total;
    }

    /** True if n is a prime number, found by trying to divide it by 2 and every odd number up to its square root. */
    private static boolean isPrime(int n) {
        if (n < 2)
            return false;
        if (n % 2 == 0)
            return n == 2;
        for (int d = 3; (long) d * d <= n; d += 2) {
            if (n % d == 0)
                return false;
        }
        return true;
    }

    /**
     * One thread's share of the work. Thread number 'first' out of 'step' threads
     * checks blocks first, first + step, first + 2 * step ... and counts the primes in them.
     */
    private static class PrimeCounter extends Thread {
        private final int first;
        private final int step;
        private final int limit;
        private long count = 0;

        PrimeCounter(int first, int step, int limit) {
            this.first = first;
            this.step = step;
            this.limit = limit;
        }

        @Override
        public void run() {
            for (int blockStart = first * BLOCK; blockStart < limit; blockStart += step * BLOCK) {
                int blockEnd = Math.min(blockStart + BLOCK, limit);
                for (int n = blockStart; n < blockEnd; n++) {
                    if (isPrime(n)) {
                        count++;
                    }
                }
            }
        }

        long getCount() {
            return count;
        }
    }

    /** The results table, the speedup chart and whether every run found the same number of primes. */
    private static void printResults(int[] threadCounts, double[] seconds, long[] primes, int cores) {
        subheading("Results");
        System.out.println("  " + textChart.DIM + "speedup = time on 1 thread / time on n threads (perfect = n, up to "
                + cores + " cores)" + textChart.RESET);
        System.out.println();
        System.out.println("  " + textChart.BOLD + "Threads     Time   Speedup  Efficiency   Speedup chart" + textChart.RESET);

        for (int i = 0; i < threadCounts.length; i++) {
            double speedup = seconds[0] / seconds[i];
            double efficiency = speedup / threadCounts[i] * 100;

            // a full bar is perfect speedup on every core
            int length = (int) Math.round(speedup / cores * BAR_WIDTH);
            if (length > BAR_WIDTH)
                length = BAR_WIDTH;
            if (length < 1)
                length = 1;

            System.out.printf("  %7d  %6.3f s  %6.2fx  %9.0f%%   %s%s%s%n", threadCounts[i], seconds[i], speedup,
                    efficiency, textChart.colourFor(100 - efficiency), textChart.repeat('█', length), textChart.RESET);
        }

        System.out.println();
        boolean allSame = true;
        for (long p : primes) {
            if (p != primes[0])
                allSame = false;
        }
        if (allSame) {
            System.out.println("  " + textChart.GREEN + "Every run found " + String.format("%,d", primes[0])
                    + " primes, so the work was split correctly." + textChart.RESET);
        } else {
            System.out.println("  " + textChart.RED + "The runs found different numbers of primes, so the split is wrong:"
                    + textChart.RESET);
            for (int i = 0; i < threadCounts.length; i++)
                System.out.printf("    %2d threads: %,d%n", threadCounts[i], primes[i]);
        }
    }

    /** The title at the top of the screen. */
    private static void title() {
        System.out.println();
        System.out.println("  " + textChart.BOLD + textChart.GREEN + "Parallelism test" + textChart.RESET);
        System.out.println();
    }

    /** A cyan heading inside the screen. */
    private static void subheading(String text) {
        System.out.println("  " + textChart.BOLD + textChart.CYAN + text + textChart.RESET);
    }
}
