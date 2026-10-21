/**
 * Memory figures for display: total, used and available memory converted from
 * KiB to a readable unit, and the percentage of memory in use. Any value that
 * memInfo could not read (-1) is shown as "unavailable" instead of a number.
 *
 * "Used" is MemTotal - MemAvailable, not MemTotal - MemFree. Linux keeps spare
 * RAM full of disk cache, so MemFree is always small; MemAvailable counts that
 * cache as usable because the kernel can reclaim it when a program needs it.
 */
public class memReport {
    private static final long KiB_PER_MiB = 1024;
    private static final long KiB_PER_GiB = 1024 * 1024;

    /** Percentage of memory in use (0 to 100), or -1 if it cannot be worked out. */
    public static double getPercentUsed() {
        long total = memInfo.getTotal();
        long used = memInfo.getUsed();
        if (total <= 0 || used < 0)
            return -1;
        return used * 100.0 / total;
    }

    /** A size in KiB as text in the largest sensible unit, e.g. "15.53 GiB", or "unavailable". */
    public static String formatKiB(long kib) {
        if (kib < 0)
            return "unavailable";
        if (kib >= KiB_PER_GiB)
            return String.format("%.2f GiB", (double) kib / KiB_PER_GiB);
        if (kib >= KiB_PER_MiB)
            return String.format("%.1f MiB", (double) kib / KiB_PER_MiB);
        return kib + " KiB";
    }

    /** Prints the memory summary. Called from the program's main method. */
    public static void print() {
        double percent = getPercentUsed();

        System.out.println("Memory");
        System.out.println("  Total:     " + formatKiB(memInfo.getTotal()));
        System.out.println("  Used:      " + formatKiB(memInfo.getUsed()));
        System.out.println("  Available: " + formatKiB(memInfo.getAvailable()));
        if (percent < 0)
            System.out.println("  In use:    unavailable");
        else
            System.out.printf("  In use:    %.1f%%%n", percent);
    }
}
