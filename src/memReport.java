/**
 * Memory figures for display: total, used and available memory converted from
 * KiB to a readable unit, and the percentage of memory in use. Any value that
 * memInfo could not read (-1) is shown as "unavailable" instead of a number.
 *
 * "Used" is MemTotal - MemAvailable, not MemTotal - MemFree. Linux keeps spare
 * RAM full of disk cache, so MemFree is always small; MemAvailable counts that
 * cache as usable because the kernel can reclaim it when a program needs it.
 *
 * The breakdown bar splits the total into three parts that add up exactly:
 * used, reclaimable cache (MemAvailable - MemFree) and completely free.
 */
public class memReport {
    private static final long KiB_PER_MiB = 1024;
    private static final long KiB_PER_GiB = 1024 * 1024;

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private static final int BAR_WIDTH = 40;
    private static final int SAMPLES = 20;
    private static final int SAMPLE_GAP_MS = 500;
    private static final int GRAPH_ROWS = 10;

    /** Percentage of memory in use (0 to 100), or -1 if it cannot be worked out. */
    public static double getPercentUsed() {
        long total = memInfo.getTotal();
        long used = memInfo.getUsed();
        if (total <= 0 || used < 0)
            return -1;
        return used * 100.0 / total;
    }

    /** A size in KiB as text in the largest sensible unit, or "unavailable". */
    public static String formatKiB(long kib) {
        if (kib < 0)
            return "unavailable";
        if (kib >= KiB_PER_GiB)
            return String.format("%.2f GiB", (double) kib / KiB_PER_GiB);
        if (kib >= KiB_PER_MiB)
            return String.format("%.1f MiB", (double) kib / KiB_PER_MiB);
        return kib + " KiB";
    }

    /** Prints the memory figures with a usage bar, a breakdown bar and a swap bar. */
    public static void print() {
        double percent = getPercentUsed();

        System.out.println(GREEN + "Memory" + RESET);
        System.out.println();
        System.out.println("  Total:     " + formatKiB(memInfo.getTotal()));
        System.out.println("  Used:      " + formatKiB(memInfo.getUsed()));
        System.out.println("  Available: " + formatKiB(memInfo.getAvailable()));
        if (percent < 0) {
            System.out.println("  In use:    unavailable");
            return;
        }
        System.out.printf("  In use:    %.1f%%%n", percent);
        System.out.println();

        System.out.println("  In use    [" + colourFor(percent) + bar(percent, BAR_WIDTH) + RESET + "] "
                + String.format("%.1f%%", percent));
        System.out.println();
        printBreakdown();
        System.out.println();
        printSwap();
    }

    /**
     * Prints the memory figures, then takes a reading every half second and draws
     * a graph of memory use over time. Stops after SAMPLES readings.
     */
    public static void printWithHistory() {
        if (getPercentUsed() < 0) {
            print();
            return;
        }

        double[] history = new double[SAMPLES];
        for (int taken = 1; taken <= SAMPLES; taken++) {
            history[taken - 1] = getPercentUsed();

            clearScreen();
            print();
            System.out.println();
            printGraph(history, taken);
            if (taken < SAMPLES) {
                System.out.println(CYAN + "  Reading " + taken + " of " + SAMPLES + "..." + RESET);
                pause(SAMPLE_GAP_MS);
            }
        }
    }

    /** One bar split into used, reclaimable cache and free, with a key underneath. */
    private static void printBreakdown() {
        long total = memInfo.getTotal();
        long used = memInfo.getUsed();
        long free = memInfo.field("MemFree");
        long cache = memInfo.getAvailable() - free;
        if (total <= 0 || used < 0 || free < 0)
            return;
        if (cache < 0)
            cache = 0;

        int usedWidth = (int) Math.round((double) used / total * BAR_WIDTH);
        int cacheWidth = (int) Math.round((double) cache / total * BAR_WIDTH);
        if (usedWidth + cacheWidth > BAR_WIDTH)
            cacheWidth = BAR_WIDTH - usedWidth;
        int freeWidth = BAR_WIDTH - usedWidth - cacheWidth;

        System.out.println("  Breakdown [" + RED + repeat('█', usedWidth) + YELLOW + repeat('▓', cacheWidth)
                + GREEN + repeat('░', freeWidth) + RESET + "]");
        System.out.println("            " + RED + "█" + RESET + " used " + formatKiB(used)
                + "   " + YELLOW + "▓" + RESET + " cache " + formatKiB(cache)
                + "   " + GREEN + "░" + RESET + " free " + formatKiB(free));
    }

    /** A bar for swap space, or a note if the machine has none. */
    private static void printSwap() {
        long swapTotal = memInfo.field("SwapTotal");
        long swapFree = memInfo.field("SwapFree");
        if (swapTotal < 0 || swapFree < 0) {
            System.out.println("  Swap      unavailable");
            return;
        }
        if (swapTotal == 0) {
            System.out.println("  Swap      none set up");
            return;
        }

        long swapUsed = swapTotal - swapFree;
        double percent = swapUsed * 100.0 / swapTotal;
        System.out.println("  Swap      [" + colourFor(percent) + bar(percent, BAR_WIDTH) + RESET + "] "
                + String.format("%.1f%%", percent) + "  (" + formatKiB(swapUsed) + " of "
                + formatKiB(swapTotal) + ")");
    }

    /**
     * A graph of memory use over time, one column per reading, oldest on the left.
     * Each row is 10%, so a column reaching the 30% row means about 30% in use.
     */
    private static void printGraph(double[] history, int count) {
        System.out.println("  Memory use over time (one reading every " + (SAMPLE_GAP_MS / 1000.0) + " s)");
        System.out.println();

        for (int row = GRAPH_ROWS; row >= 1; row--) {
            int level = row * 100 / GRAPH_ROWS;
            String line = String.format("  %3d%% │", level);
            for (int i = 0; i < count; i++) {
                int height = (int) Math.round(history[i] / (100.0 / GRAPH_ROWS));
                if (height >= row) {
                    line = line + colourFor(history[i]) + "██" + RESET;
                } else {
                    line = line + "  ";
                }
            }
            System.out.println(line);
        }
        System.out.println("    0% └" + repeat('─', SAMPLES * 2));
        System.out.println("        oldest" + repeat(' ', SAMPLES * 2 - 12) + "newest");
    }

    /** A bar of 'width' characters, filled in proportion to 'percent'. */
    private static String bar(double percent, int width) {
        int filled = (int) Math.round(percent / 100 * width);
        if (filled > width)
            filled = width;
        if (filled < 0)
            filled = 0;
        return repeat('█', filled) + repeat('░', width - filled);
    }

    /** Green under 60%, yellow under 85%, red otherwise. */
    private static String colourFor(double percent) {
        if (percent < 60)
            return GREEN;
        if (percent < 85)
            return YELLOW;
        return RED;
    }

    private static String repeat(char c, int count) {
        String s = "";
        for (int i = 0; i < count; i++)
            s = s + c;
        return s;
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
