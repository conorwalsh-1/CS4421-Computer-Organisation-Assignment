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
 * After the timings, the task is run again for each thread count while the load on
 * every core is measured, from two readings of /proc/stat 1 second apart (the same
 * calculation as the CPU screen). This shows the threads landing on different cores:
 * with 1 thread about one core should be busy, with 4 threads about four.
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

    /** Characters from empty to full, used to show each core's load in one character. */
    private static final char[] LEVELS = {' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█'};

    /** A core with at least this much load (out of 1) counts as busy. */
    private static final double BUSY = 0.5;

    /**
     * Runs the test for each thread count, showing progress while it runs, then
     * clears the screen and shows the times, speedups and a speedup chart.
     */
    public static void run() {
        int cores = cpuInfo.getCount();
        if (cores < 1) {
            // no /proc/cpuinfo, so ask Java how many processors it can use instead
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

        // per-core load needs /proc/stat, so only measure it when cpuInfo can read the cores
        double[][] loads = null;
        if (cpuInfo.getCount() > 0) {
            System.out.println();
            loads = new double[threadCounts.length][];
            for (int i = 0; i < threadCounts.length; i++) {
                System.out.printf("  Measuring per-core load with %2d threads...%n", threadCounts[i]);
                loads[i] = watchLoad(threadCounts[i]);
            }
        }

        textChart.clearScreen();
        title();
        printResults(threadCounts, seconds, primes, cores);
        printLoads(threadCounts, loads);
    }

    /**
     * Keeps the prime task running on 'threads' threads in the background while the
     * load on each core is measured over 1 second. Returns the load of each core
     * from 0 to 1, or -1 for a core that couldn't be read.
     */
    private static double[] watchLoad(int threads) {
        LoadRunner runner = new LoadRunner(threads);
        runner.start();
        textChart.pause(500); // give the threads time to start and spread out over the cores

        int cores = cpuInfo.getCount();
        long[] previousBusy = new long[cores];
        long[] previousIdle = new long[cores];
        for (int core = 0; core < cores; core++) {
            previousBusy[core] = cpuInfo.getBusy(core);
            previousIdle[core] = cpuInfo.getIdle(core);
        }

        textChart.pause(1000);

        double[] loads = new double[cores];
        for (int core = 0; core < cores; core++) {
            loads[core] = cpuInfo.calculateLoad(previousBusy[core], previousIdle[core],
                    cpuInfo.getBusy(core), cpuInfo.getIdle(core));
        }

        runner.finish();
        try {
            runner.join(); // wait for the run that is going to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return loads;
    }

    /**
     * Runs the prime task over and over on 'threads' threads until finish() is called,
     * so the cores stay busy for as long as the load is being measured.
     */
    private static class LoadRunner extends Thread {
        private final int threads;

        // volatile so this thread sees the change when the main thread calls finish()
        private volatile boolean stop = false;

        LoadRunner(int threads) {
            this.threads = threads;
        }

        @Override
        public void run() {
            while (!stop) {
                countPrimes(threads, LIMIT);
            }
        }

        void finish() {
            stop = true;
        }
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

    /**
     * One row per thread count with every core's load as a bar character and a
     * percentage, and how many cores were busy. Prints a note instead if the load
     * couldn't be measured.
     */
    private static void printLoads(int[] threadCounts, double[][] loads) {
        System.out.println();
        subheading("Per-core load while the task runs");
        if (loads == null) {
            System.out.println("  not available (needs /proc/stat)");
            return;
        }
        System.out.println("  " + textChart.DIM + "measured over 1 second, a core counts as busy at "
                + Math.round(BUSY * 100) + "% or more" + textChart.RESET);
        System.out.println();

        int cores = loads[0].length;
        String header = "  Threads  ";
        for (int core = 0; core < cores; core++) {
            header = header + String.format("c%-4d", core);
        }
        System.out.println("  " + textChart.BOLD + header.substring(2) + " Busy" + textChart.RESET);

        for (int i = 0; i < threadCounts.length; i++) {
            String line = String.format("  %7d  ", threadCounts[i]);
            int busy = 0;
            for (int core = 0; core < cores; core++) {
                double load = loads[i][core];
                if (load < 0) {
                    line = line + "  -  ";
                    continue;
                }
                if (load >= BUSY) {
                    busy++;
                }
                char level = LEVELS[(int) Math.round(load * (LEVELS.length - 1))];
                line = line + textChart.colourFor(load * 100) + level + textChart.RESET
                        + String.format("%3d ", Math.round(load * 100));
            }
            System.out.println(line + String.format("%4d", busy));
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
