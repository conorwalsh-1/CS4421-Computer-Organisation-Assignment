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

    private static final int BAR_WIDTH = 40;
    private static final int SAMPLES = 20;
    private static final int SAMPLE_GAP_MS = 500;

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

        System.out.println(textChart.GREEN + "Memory" + textChart.RESET);
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

        System.out.println("  In use    " + textChart.percentBar(percent, BAR_WIDTH));
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

            textChart.clearScreen();
            print();
            System.out.println();
            textChart.graph("Memory use over time (one reading every " + (SAMPLE_GAP_MS / 1000.0) + " s)",
                    history, taken, SAMPLES);
            if (taken < SAMPLES) {
                System.out.println(textChart.CYAN + "  Reading " + taken + " of " + SAMPLES + "..." + textChart.RESET);
                textChart.pause(SAMPLE_GAP_MS);
            }
        }
    }

    /** One bar split into used, reclaimable cache and free, with a key underneath. */
    public static void printBreakdown() {
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

        System.out.println("  Breakdown [" + textChart.RED + textChart.repeat('█', usedWidth)
                + textChart.YELLOW + textChart.repeat('▓', cacheWidth)
                + textChart.RESET + textChart.repeat('░', freeWidth) + "]");
        System.out.println("            " + textChart.RED + "█" + textChart.RESET + " used " + formatKiB(used)
                + "   " + textChart.YELLOW + "▓" + textChart.RESET + " cache " + formatKiB(cache)
                + "   ░ free " + formatKiB(free));
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
        System.out.println("  Swap      " + textChart.percentBar(percent, BAR_WIDTH)
                + "  (" + formatKiB(swapUsed) + " of " + formatKiB(swapTotal) + ")");
    }
}
