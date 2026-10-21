/**
 * Timing loops that show how the memory hierarchy (caches and RAM) affects how
 * fast a program can read memory. Times are measured with System.nanoTime() and
 * reported as nanoseconds per array access.
 *
 * Working-set test: reads an int[] from 4 KiB up to 64 MiB, many times over,
 * jumping around the array so the CPU can't guess what comes next.
 * Small arrays fit in the caches and are fast; once an array is bigger than a
 * cache level, reads have to come from the next, slower level.
 *
 * Stride test: reads the same 64 MiB array with strides of 1, 2, 4 ... 64 ints
 * (4 to 256 bytes). Every element is still read once, so the amount of work is
 * the same, but bigger strides use less of each cache line that is loaded.
 *
 * Run:  java -cp out memExplorer   (best on alderaan or a Linux VM)
 */
public class memExplorer {
    private static final int KiB = 1024;
    private static final int MiB = 1024 * 1024;

    /** Roughly how many reads each working-set measurement does, whatever the array size. */
    private static final long TOTAL_ACCESSES = 64L * MiB;

    /**
     * How far the working-set test jumps between reads, in ints (about 64 KiB).
     * It's odd, so after array.length jumps every element has been read exactly once.
     */
    private static final int JUMP = 16411;

    /** Each test is run this many times and the fastest time is kept. */
    private static final int REPEATS = 5;

    /** The sums are added here so Java can't skip the loops as unused work. */
    private static long sink = 0;

    public static void main(String[] args) {
        // the first runs are slower while Java compiles the loops, so do a few runs first and ignore them
        System.out.println("Warming up...");
        int[] warmUp = new int[MiB];
        for (int i = 0; i < 3; i++) {
            timeWorkingSet(warmUp);
            timeStride(warmUp, 4);
        }

        System.out.println();
        System.out.println("Working-set test (ns per access)");
        for (int bytes = 4 * KiB; bytes <= 64 * MiB; bytes = bytes * 2) {
            int[] array = makeArray(bytes / 4); // an int is 4 bytes
            double ns = timeWorkingSet(array);
            System.out.printf("  %10s  %6.3f ns%n", memReport.formatKiB(bytes / KiB), ns);
        }

        System.out.println();
        System.out.println("Stride test on a 64 MiB array (ns per access)");
        int[] big = makeArray(64 * MiB / 4);
        for (int stride = 1; stride <= 64; stride = stride * 2) {
            double ns = timeStride(big, stride);
            System.out.printf("  stride %2d ints (%3d bytes)  %6.3f ns%n", stride, stride * 4, ns);
        }
    }

    /**
     * Reads every element of the array, enough times to make about TOTAL_ACCESSES
     * reads, and returns the time per read in nanoseconds.
     *
     * The array length must be a power of 2. Instead of going from start to end
     * it jumps JUMP elements each time and wraps around at the end. Going from start
     * to end is too easy: the CPU spots the pattern and loads the next cache lines
     * before they're needed (prefetching), which hides how big the caches are.
     */
    public static double timeWorkingSet(int[] array) {
        long passes = TOTAL_ACCESSES / array.length;
        if (passes < 1) {
            passes = 1;
        }

        // array.length is a power of 2, so "& mask" wraps an index back into the array
        // (it works like "% array.length" but is faster)
        int mask = array.length - 1;

        long best = Long.MAX_VALUE;
        for (int r = 0; r < REPEATS; r++) {
            long sum = 0;
            int index = 0;
            long start = System.nanoTime();
            for (long p = 0; p < passes; p++) {
                for (int i = 0; i < array.length; i++) {
                    sum += array[index];
                    index = (index + JUMP) & mask;
                }
            }
            long time = System.nanoTime() - start;

            sink += sum;
            if (time < best) {
                best = time;
            }
        }
        return (double) best / (passes * array.length);
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

    /** An array of this many ints, filled with numbers so every part of it is really in memory. */
    private static int[] makeArray(int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = i;
        }
        return array;
    }
}
