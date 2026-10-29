/**
 * CPU figures for display: the model, number of cores, cpu0's caches, a load bar
 * for each core and for the average, and a graph of the average load over time.
 *
 * The load is worked out from two readings of the busy and idle tick counts in
 * /proc/stat taken half a second apart: busy ticks / (busy + idle ticks) over
 * that half second. A core that cannot be read has a load of -1.
 */
public class cpuReport {
    private static final int BAR_WIDTH = 40;
    private static final int SAMPLES = 10;
    private static final int SAMPLE_GAP_MS = 1000;

    /** The CPU model name, or the ARM implementer and part if there is no model name. */
    public static String getModel() {
        String model = cpuInfo.getModel();
        if (!model.equals("unknown"))
            return model;

        // Arm64 machines have no "model name" line, so use the implementer and part number instead
        String implementer = cpuInfo.getField("CPU implementer");
        String part = cpuInfo.getField("CPU part");
        if (implementer.isEmpty() && part.isEmpty())
            return "not available";
        if (implementer.isEmpty())
            implementer = "not available";
        if (part.isEmpty())
            part = "not available";
        return "ARM implementer=" + implementer + ", part=" + part;
    }

    /**
     * Takes a reading every half second and redraws the CPU details, a load bar for
     * each core and a graph of the average load over time. Stops after SAMPLES readings.
     */
    public static void printWithHistory() {
        int cores = cpuInfo.getCount();
        if (cores == 0) {
            print(new double[0]);
            return;
        }

        long[] previousBusy = new long[cores];
        long[] previousIdle = new long[cores];
        for (int core = 0; core < cores; core++) {
            previousBusy[core] = cpuInfo.getBusy(core);
            previousIdle[core] = cpuInfo.getIdle(core);
        }

        double[] history = new double[SAMPLES];
        for (int taken = 1; taken <= SAMPLES; taken++) {
            textChart.pause(SAMPLE_GAP_MS);

            double[] loads = new double[cores];
            for (int core = 0; core < cores; core++) {
                long busy = cpuInfo.getBusy(core);
                long idle = cpuInfo.getIdle(core);
                loads[core] = cpuInfo.calculateLoad(previousBusy[core], previousIdle[core], busy, idle);
                previousBusy[core] = busy;
                previousIdle[core] = idle;
            }
            history[taken - 1] = averagePercent(loads);

            textChart.clearScreen();
            print(loads);
            System.out.println();
            textChart.graph("Average load over time (one reading every " + (SAMPLE_GAP_MS / 1000.0) + " s)",
                    history, taken, SAMPLES);
            if (taken < SAMPLES)
                System.out.println(textChart.CYAN + "  Reading " + taken + " of " + SAMPLES + "..." + textChart.RESET);
        }
    }

    /** Prints the model, core count, caches and a load bar for each core and the average. */
    private static void print(double[] loads) {
        System.out.println(textChart.GREEN + "CPU" + textChart.RESET);
        System.out.println();
        System.out.println("  Model:   " + getModel());
        System.out.println("  Cores:   " + cpuInfo.getCount());
        System.out.println();
        printCaches();
        System.out.println();

        if (loads.length == 0) {
            System.out.println("  Load:    not available");
            return;
        }
        for (int core = 0; core < loads.length; core++) {
            String label = String.format("  Core %-3d", core);
            if (loads[core] < 0)
                System.out.println(label + "not available");
            else
                System.out.println(label + textChart.percentBar(loads[core] * 100, BAR_WIDTH));
        }

        double average = averagePercent(loads);
        System.out.println();
        if (average < 0)
            System.out.println("  Average not available");
        else
            System.out.println("  Average " + textChart.percentBar(average, BAR_WIDTH));
    }

    /** One line per cache of cpu0 (level, type, size and line size), or a note if there are none. */
    private static void printCaches() {
        System.out.println("  Caches (core 0):");
        int index = 0;
        cpuInfo.CacheInfo cache = cpuInfo.getCacheInfo(index);
        if (cache == null) {
            System.out.println("    not available");
            return;
        }

        while (cache != null) {
            String name = "L" + orUnknown(cache.getLevel()) + " " + orUnknown(cache.getType());
            String line = String.format("    %-16s %8s", name, orUnknown(cache.getSize()));
            if (!cache.getCoherencyLineSize().isEmpty())
                line = line + "   " + cache.getCoherencyLineSize() + "-byte lines";
            System.out.println(line);

            index++;
            cache = cpuInfo.getCacheInfo(index);
        }
    }

    /**
     * All of cpu0's caches on one line, with d for data and i for instruction,
     * or "not available" if there is no cache information.
     */
    public static String cacheSummary() {
        String summary = "";
        int index = 0;
        cpuInfo.CacheInfo cache = cpuInfo.getCacheInfo(index);
        while (cache != null) {
            String type = "";
            if (cache.getType().equals("Data"))
                type = "d";
            if (cache.getType().equals("Instruction"))
                type = "i";

            if (!summary.isEmpty())
                summary = summary + " · ";
            summary = summary + "L" + orUnknown(cache.getLevel()) + type + " " + orUnknown(cache.getSize());

            index++;
            cache = cpuInfo.getCacheInfo(index);
        }

        if (summary.isEmpty())
            return "not available";
        return summary;
    }

    /** The average of the cores that could be read, as a percentage, or -1 if none could. */
    private static double averagePercent(double[] loads) {
        double total = 0;
        int readable = 0;
        for (double load : loads) {
            if (load >= 0) {
                total += load;
                readable++;
            }
        }
        if (readable == 0)
            return -1;
        return total / readable * 100;
    }

    private static String orUnknown(String text) {
        if (text.isEmpty())
            return "?";
        return text;
    }
}
