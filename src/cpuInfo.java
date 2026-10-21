import java.nio.file.Path;

/*JD*/
/**
 * Information about the processor, read from /proc/cpuinfo and /proc/stat.
 *
 * The busy and idle values are counts of "ticks" since the machine booted
 * (usually 100 ticks per second). To find the load, read them twice, wait about
 * a second, read them again, and work out busy / (busy + idle) from the DIFFERENCES.
 */
public class cpuInfo
{
    private static final Path CPUINFO = sysFile.path("/proc/cpuinfo");  /*pulls the Cpu info from the linux processes folder*/
    private static final Path STAT = sysFile.path("/proc/stat"); /* pulls the stat file which contains kernel activities */

    /** Number of cores (logical processors) the operating system sees. */
    public static int getCount()
    {
        int count = 0;
        for (String line : sysFile.lines(CPUINFO))  /*Iterates through each line in cpu info counting the processors */
            if (line.startsWith("processor"))
                count++;
        return count;
    }

    /**
     * The processor model, for example "Intel(R) Xeon(R) Processor @ 2.80GHz".
     * On arm64 there is no model name line, so this returns "unknown"; use
     * getField("CPU part") or getField("CPU implementer") instead.
     */
    public static String getModel()
    {
        String model = getField("model name");/*accesses cpu info through the getField method */
        return model.isEmpty() ? "unknown" : model;/*placeholder if no model is found */
    }

    /** The value of the first line in /proc/cpuinfo whose name is exactly 'name', or "". */
    public static String getField(String name)
    {
        for (String line : sysFile.lines(CPUINFO)) {    /*iterates through each line in Cpuinfo */
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).strip().equals(name))/*Checks if the text right of the colon is the desired field */
                return line.substring(colon + 1).strip();/*If so it returns the string to the right of the colon*/
        }
        return "";
    }

    /** Ticks since boot that core number 'core' (0, 1, 2 ...) has spent doing work. */
    public static long getBusy(int core)
    {
        long[] f = fields(core);/*Gets the array of fields */
        // user + nice + system + irq + softirq + steal,, all of the fields that are doing work
        return f == null ? -1 : f[0] + f[1] + f[2] + f[5] + f[6] + f[7];/*returns the sum */
    }

    /** Ticks since boot that core number 'core' has spent idle (including waiting for I/O). */
    public static long getIdle(int core)
    {
        long[] f = fields(core);
        return f == null ? -1 : f[3] + f[4];/*returns the sum of the idle and iowait fields */
    }

    /** Calculate CPU load from two readings of the cumulative tick counters. */
    public static double calculateLoad(long prevBusy, long prevIdle, long busy, long idle)
    {
        if (prevBusy < 0 || prevIdle < 0 || busy < 0 || idle < 0)/*checks all times are greater than 0 */
            return -1;

        long busyDelta = busy - prevBusy;//gets the differences
        long idleDelta = idle - prevIdle;
        long totalDelta = busyDelta + idleDelta;
        if (busyDelta < 0 || idleDelta < 0 || totalDelta <= 0)/*checks again that the totals are greater than 0 */
            return -1;

        return (double) busyDelta / totalDelta;/*returns the total busy time over the total time */
    }

    /** The numbers from the "cpuN" line of /proc/stat, or null if there is no such core. */
    private static long[] fields(int core)
    {
        String prefix = "cpu" + core + " ";
        for (String line : sysFile.lines(STAT)) {/*Iterates through each line in the Stat file */
            if (line.startsWith(prefix)) {/*Each line thats a Cpu core such as  cpu1 */
                String[] part = line.strip().split("\\s+");/*Splits the string at the whitespace */
                long[] f = new long[8];
                for (int i = 0; i < 8; i++)
                    f[i] = (i + 1 < part.length) ? Long.parseLong(part[i + 1]) : 0;/*Stores each field of the cpu core and returns them */
                return f;
            }
        }
        return null;
    }
}
