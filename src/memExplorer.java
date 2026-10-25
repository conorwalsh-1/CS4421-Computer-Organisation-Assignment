import java.util.Random;

/**
 * Timing loops that show how the memory hierarchy (caches and RAM) affects how
 * fast a program can read memory. Times are measured with System.nanoTime() and
 * reported as nanoseconds per array access.
 *
 * Working-set test: reads an int[] from 4 KiB up to 64 MiB, many times over,
 * in a random order so the CPU can't guess what comes next.
 * Small arrays fit in the caches and are fast; once an array is bigger than a
 * cache level, reads have to come from the next, slower level.
 *
 * Stride test: reads the same 64 MiB array with strides of 1, 2, 4 ... 64 ints
 * (4 to 256 bytes). Every element is still read once, so the amount of work is
 * the same, but bigger strides use less of each cache line that is loaded.
 *
 * After both tests it estimates where each cache level ends and how big a cache
 * line is, from where the times jump.
 *
 * Started from the main menu (option 6). Best run on alderaan or a Linux VM.
 */
public class memExplorer {
    private static final int KiB = 1024;
    private static final int MiB = 1024 * 1024;

    /** The working-set test uses 15 sizes: 4 KiB, 8 KiB ... 64 MiB. */
    private static final int NUM_SIZES = 15;

    /** The stride test uses 7 strides: 1, 2, 4 ... 64 ints. */
    private static final int NUM_STRIDES = 7;

    /** How many reads each working-set measurement does, whatever the array size. */
    private static final long TOTAL_ACCESSES = 16L * MiB;

    /** Used to shuffle the read order. The fixed seed gives the same order every run. */
    private static final Random random = new Random(4421);

    /** Each test is run this many times and the fastest time is kept. */
    private static final int REPEATS = 5;

    /** A working-set time this many times bigger than the size before it counts as a jump. */
    private static final double LEVEL_JUMP = 1.2;

    /** A stride time this many times bigger than the stride before it counts as still rising. */
    private static final double STRIDE_JUMP = 1.3;

    /** The results are added here so Java can't skip the loops as unused work. */
    private static long sink = 0;

    /** Longest bar in the result charts. */
    private static final int BAR_WIDTH = 36;

    /**
     * Runs both tests, showing progress while they run, then clears the screen and
     * shows the results as bar charts with the estimates underneath.
     */
    public static void run() {
        title();
        System.out.println("  " + textChart.DIM + "This takes about a minute. Close other programs for steadier results."
                + textChart.RESET);
        System.out.println();

        // the first runs are slower while Java compiles the loops, so do a few runs first and ignore them
        System.out.println("  Warming up...");
        int[] warmUp = makeArray(MiB);
        for (int i = 0; i < 3; i++) {
            timeWorkingSet(warmUp);
            timeStride(warmUp, 4);
        }

        // keep the results so they can be analysed after the tests
        int[] sizes = new int[NUM_SIZES];
        double[] sizeTimes = new double[NUM_SIZES];
        int[] strides = new int[NUM_STRIDES];
        double[] strideTimes = new double[NUM_STRIDES];

        int bytes = 4 * KiB;
        for (int i = 0; i < NUM_SIZES; i++) {
            System.out.printf("  Working-set test  %2d of %d   %-10s", i + 1, NUM_SIZES, memReport.formatKiB(bytes / KiB));
            int[] array = makeArray(bytes / 4); // an int is 4 bytes
            sizes[i] = bytes;
            sizeTimes[i] = timeWorkingSet(array);
            System.out.printf("%7.2f ns%n", sizeTimes[i]);
            bytes = bytes * 2;
        }

        System.out.println("  Making the 64 MiB array for the stride test...");
        int[] big = makeArray(64 * MiB / 4);
        int stride = 1;
        for (int i = 0; i < NUM_STRIDES; i++) {
            System.out.printf("  Stride test       %2d of %d   %-10s", i + 1, NUM_STRIDES, stride + " ints");
            strides[i] = stride;
            strideTimes[i] = timeStride(big, stride);
            System.out.printf("%7.3f ns%n", strideTimes[i]);
            stride = stride * 2;
        }

        textChart.clearScreen();
        title();
        printSizeChart(sizes, sizeTimes);
        printStrideChart(strides, strideTimes);

        subheading("Estimates");
        printCacheLevels(sizes, sizeTimes);
        printLineSize(strides, strideTimes);
        System.out.println();
        System.out.println("  Reported by Linux: " + cpuReport.cacheSummary());
        System.out.println("  " + textChart.DIM + "(cache line size: " + cacheLineSize() + ")" + textChart.RESET);
    }

    /** The title at the top of the screen. */
    private static void title() {
        System.out.println();
        System.out.println("  " + textChart.BOLD + textChart.GREEN + "Memory explorer" + textChart.RESET);
        System.out.println();
    }

    /** A cyan heading inside the screen. */
    private static void subheading(String text) {
        System.out.println();
        System.out.println("  " + textChart.BOLD + textChart.CYAN + text + textChart.RESET);
    }

    /**
     * The working-set results, one bar per array size. The bars use a log scale
     * (each step up the bar is the same number of times slower) because the times
     * go from about 1 ns in L1 to around 100 ns in RAM, and on a normal scale the
     * small differences between L1, L2 and L3 would be too small to see.
     */
    private static void printSizeChart(int[] sizes, double[] times) {
        subheading("Working-set test");
        System.out.println("  " + textChart.DIM + "ns per read, random order, longer bar = slower (log scale)"
                + textChart.RESET);

        double fastest = times[0];
        double slowest = times[0];
        for (double t : times) {
            if (t < fastest)
                fastest = t;
            if (t > slowest)
                slowest = t;
        }

        for (int i = 0; i < sizes.length; i++) {
            // how far this time is from the fastest to the slowest, from 0 to 1, on a log scale
            double position = 1;
            if (slowest > fastest)
                position = Math.log(times[i] / fastest) / Math.log(slowest / fastest);
            int length = 1 + (int) Math.round(position * (BAR_WIDTH - 1));

            String bar = textChart.CYAN + textChart.repeat('█', length) + textChart.RESET
                    + textChart.repeat(' ', BAR_WIDTH - length);
            System.out.printf("  %9s  %s %7.2f ns%n", memReport.formatKiB(sizes[i] / KiB), bar, times[i]);
        }
    }

    /** The stride results, one bar per stride, on a normal scale. */
    private static void printStrideChart(int[] strides, double[] times) {
        subheading("Stride test on a 64 MiB array");
        System.out.println("  " + textChart.DIM + "ns per read, every element read once, longer bar = slower"
                + textChart.RESET);

        double slowest = 0;
        for (double t : times) {
            if (t > slowest)
                slowest = t;
        }

        for (int i = 0; i < strides.length; i++) {
            int length = (int) Math.round(times[i] / slowest * BAR_WIDTH);
            if (length < 1)
                length = 1;
            String bar = textChart.YELLOW + textChart.repeat('█', length) + textChart.RESET
                    + textChart.repeat(' ', BAR_WIDTH - length);
            System.out.printf("  %2d ints %3d B  %s %7.3f ns%n", strides[i], strides[i] * 4, bar, times[i]);
        }
    }

    /** The coherency line size of cpu0's first cache in bytes, as Linux reports it, or "not available". */
    private static String cacheLineSize() {
        cpuInfo.CacheInfo cache = cpuInfo.getCacheInfo(0);
        if (cache == null || cache.getCoherencyLineSize().isEmpty())
            return "not available";
        return cache.getCoherencyLineSize() + " bytes";
    }

    /**
     * Reads the array TOTAL_ACCESSES times and returns the time per read in nanoseconds.
     *
     * Each element holds the index of the next element to read (see makeArray), so
     * every read has to finish before the CPU knows where the next one is. That way
     * the time is how long one read really takes from wherever the array is: L1, L2,
     * L3 or RAM. If the reads didn't depend on each other the CPU could do lots of
     * them at once, which hides the difference between the levels.
     */
    public static double timeWorkingSet(int[] array) {
        long best = Long.MAX_VALUE;
        for (int r = 0; r < REPEATS; r++) {
            int index = 0;
            long start = System.nanoTime();
            for (long i = 0; i < TOTAL_ACCESSES; i++) {
                index = array[index];
            }
            long time = System.nanoTime() - start;

            sink += index;
            if (time < best) {
                best = time;
            }
        }
        return (double) best / TOTAL_ACCESSES;
    }

    /**
     * Reads every element of the array once, jumping stride elements at a time:
     * first 0, stride, 2*stride ..., then 1, 1 + stride ..., and so on.
     * Returns the time per read in nanoseconds.
     */
    public static double timeStride(int[] array, int stride) {
        long best = Long.MAX_VALUE;
        for (int r = 0; r < REPEATS; r++) {
            long sum = 0;
            long start = System.nanoTime();
            for (int first = 0; first < stride; first++) {
                for (int i = first; i < array.length; i += stride) {
                    sum += array[i];
                }
            }
            long time = System.nanoTime() - start;

            sink += sum;
            if (time < best) {
                best = time;
            }
        }
        return (double) best / array.length;
    }

    /**
     * Prints where each cache level seems to end. A level ends where the time per
     * read jumps up (by more than LEVEL_JUMP times) as the array gets bigger.
     * A jump can take a few sizes to finish, so jumps in a row count as one.
     */
    public static void printCacheLevels(int[] sizes, double[] times) {
        int level = 1;
        boolean rising = false;

        for (int i = 1; i < sizes.length; i++) {
            if (times[i] > times[i - 1] * LEVEL_JUMP) {
                // only report the first size of a jump, not every size while it's still rising
                if (!rising) {
                    System.out.printf("  L%d seems to end around %s (%.2f ns, then %.2f ns at %s)%n", level,
                            memReport.formatKiB(sizes[i - 1] / KiB), times[i - 1], times[i],
                            memReport.formatKiB(sizes[i] / KiB));
                    level++;
                }
                rising = true;
            } else {
                rising = false;
            }
        }

        if (level == 1) {
            System.out.println("  No clear jumps in the working-set times");
        }
    }

    /**
     * Prints the estimated cache line size. While the stride is smaller than a line,
     * doubling it means a new line is loaded twice as often, so the time per read
     * keeps going up. Once the stride is a whole line or more, every read loads a
     * new line anyway and the time flattens out. The line size is the stride of the
     * last big rise.
     */
    public static void printLineSize(int[] strides, double[] times) {
        int lastRise = -1;
        for (int i = 1; i < strides.length; i++) {
            if (times[i] > times[i - 1] * STRIDE_JUMP) {
                lastRise = i;
            }
        }

        if (lastRise == -1) {
            System.out.println("  Cache line: no clear rise in the stride times");
        } else if (lastRise == strides.length - 1) {
            // still rising at the biggest stride, so it never flattened
            System.out.printf("  Cache line: at least %d bytes (still rising at the biggest stride)%n",
                    strides[lastRise] * 4);
        } else {
            System.out.printf("  Cache line: about %d bytes (times flatten out after a stride of %d ints)%n",
                    strides[lastRise] * 4, strides[lastRise]);
        }
    }

    /**
     * An array of this many ints, where each element holds the index of the next
     * element to read. Following the indexes visits every element once, in a random
     * order, then goes back to the start. Filling it in also makes sure every part
     * of it is really in memory.
     */
    private static int[] makeArray(int length) {
        // order is the list of indexes 0, 1, 2 ... shuffled into a random order
        int[] order = new int[length];
        for (int i = 0; i < length; i++) {
            order[i] = i;
        }
        // shuffle: swap each place with a random place at or before it
        for (int i = length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = order[i];
            order[i] = order[j];
            order[j] = temp;
        }

        // each index in the order points to the one after it, and the last one points back to the first
        int[] array = new int[length];
        for (int i = 0; i < length - 1; i++) {
            array[order[i]] = order[i + 1];
        }
        array[order[length - 1]] = order[0];
        return array;
    }
}
