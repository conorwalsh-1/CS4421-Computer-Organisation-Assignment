import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * The main program. Shows a menu and prints the CPU, memory, PCI, USB or summary
 * section that the user picks, until they choose to exit.
 */
public class demo
{
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private static final String[] TITLE = {
            "██╗  ██╗ █████╗ ██████╗ ██████╗ ██╗    ██╗ █████╗ ██████╗ ███████╗",
            "██║  ██║██╔══██╗██╔══██╗██╔══██╗██║    ██║██╔══██╗██╔══██╗██╔════╝",
            "███████║███████║██████╔╝██║  ██║██║ █╗ ██║███████║██████╔╝█████╗",
            "██╔══██║██╔══██║██╔══██╗██║  ██║██║███╗██║██╔══██║██╔══██╗██╔══╝",
            "██║  ██║██║  ██║██║  ██║██████╔╝╚███╔███╔╝██║  ██║██║  ██║███████╗",
            "╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝"
    };
    private static final String[] TITLE_COLOURS = {CYAN, CYAN, BLUE, BLUE, PURPLE, PURPLE};
    private static final int MENU_WIDTH = 66;
    private static final int SVG_HISTORY_SIZE = 30;
    private static double[][] coreLoadHistory = new double[0][];
    private static int coreLoadHistoryCount;
    private static final double[] memoryUsageHistory = new double[SVG_HISTORY_SIZE];
    private static int memoryUsageHistoryCount;

    public static void main(String[] args) throws IOException
    {
        Scanner sc = new Scanner(System.in);
        Path template = Paths.get(args.length > 0 ? args[0] : "src/template.html"); //creates 2 path objects using the supplied arguments in command line or the default names if no arguments are given
        Path output   = Paths.get(args.length > 1 ? args[1] : "src/report.html");

    Thread reportUpdater = new Thread(() -> { //makes a new Java thread
        while (!Thread.currentThread().isInterrupted()) {//loops until the thread is interupted
            try {
                writeReport(template, output, collectTemplateData());//writes a new report each sec
                Thread.sleep(1000);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
    }
});

reportUpdater.setDaemon(true);
reportUpdater.start();


        while (true) {
            clearScreen();
            printBanner();
            printMenu();

            if (!sc.hasNextInt()) {/*stops the program crashing if a letter is typed */
                if (!sc.hasNext())/*no more input at all, so stop */
                    return;
                sc.nextLine();/*throws away the whole line that was typed */
                System.out.println(RED + "Invalid choice, please pick a number." + RESET);
                waitForEnter(sc);
                continue;
            }
            int input = sc.nextInt();
            sc.nextLine();/*clears the rest of the line so waitForEnter works */

            clearScreen();
            switch (input) {
                case 1:
                    cpuMenu();
                    break;
                case 2:
                    memoryMenu();
                    break;
                case 3:
                    pciMenu();
                    break;
                case 4:
                    usbMenu();
                    break;
                case 5:
                    summaryMenu();
                    break;
                case 6:
                    memExplorer.run();
                    break;
                case 7:
                    parallelTest.run();
                    break;
                case 8:
                     
                    Map<String, String> templateData = collectTemplateData();
                    writeReport(template, output, templateData);
                    
                    break;
                case 0:
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println(RED + "Invalid option, try again." + RESET);
            }
            waitForEnter(sc);
        }
    }

    /** Prints the title at the top of the menu, with each row of letters in a different colour. */
    private static void printBanner()
    {
        System.out.println();
        for (int row = 0; row < TITLE.length; row++)
            System.out.println("  " + TITLE_COLOURS[row] + TITLE[row] + RESET);

        String subtitle = " CS4421 Computer Organisation · System Inspector ";
        int left = (MENU_WIDTH - subtitle.length()) / 2;
        int right = MENU_WIDTH - left - subtitle.length();
        System.out.println("  " + DIM + textChart.repeat('─', left) + RESET + BOLD + subtitle + RESET
                + DIM + textChart.repeat('─', right) + RESET);
        System.out.println();
    }

    /** Prints a status line with a memory bar and the number of cores, PCI and USB devices. */
    private static void printStatus()
    {
        double memory = memReport.getPercentUsed();
        String memoryText = "unavailable";
        if (memory >= 0)
            memoryText = textChart.percentBar(memory, 20);

        System.out.println("  " + BOLD + "Memory " + RESET + memoryText
                + "   " + BOLD + "Cores " + RESET + cpuInfo.getCount()
                + "   " + BOLD + "PCI " + RESET + pciInfo.getCount()
                + "   " + BOLD + "USB " + RESET + usbInfo.getCount());
        System.out.println();
    }

    /** Prints the list of options in a box. */
    private static void printMenu()
    {
        printStatus();

        int inner = MENU_WIDTH - 2;
        System.out.println("  " + PURPLE + "┌" + textChart.repeat('─', inner) + "┐" + RESET);
        menuLine(inner, "1", "CPU", "model, caches, load on each core");
        menuLine(inner, "2", "Memory", "usage, breakdown and swap");
        menuLine(inner, "3", "PCI", "devices, vendors and classes");
        menuLine(inner, "4", "USB", "connected devices");
        menuLine(inner, "5", "Summary", "the whole machine on one screen");
        menuLine(inner, "6", "Explorer", "cache timing tests (about a minute)");
        menuLine(inner, "7", "Parallel", "CPU-heavy task on 1, 2, 4 ... threads");
        menuLine(inner, "8", "HTML Report", "Live updating hardware report");
        System.out.println("  " + PURPLE + "├" + textChart.repeat('─', inner) + "┤" + RESET);
        menuLine(inner, "0", "Exit", "");
        System.out.println("  " + PURPLE + "└" + textChart.repeat('─', inner) + "┘" + RESET);
        System.out.println();
        System.out.print("  " + BOLD + "Pick an option: " + RESET);
    }

    /** One row of the menu box: the key to press, the option's name and what it shows. */
    private static void menuLine(int inner, String key, String name, String description)
    {
        // the same row without colours, so the padding to the right-hand border can be worked out
        String plain = "  [" + key + "]  " + String.format("%-9s", name) + " " + description;
        String padding = textChart.repeat(' ', inner - plain.length());

        System.out.println("  " + PURPLE + "│" + RESET
                + "  " + YELLOW + BOLD + "[" + key + "]" + RESET
                + "  " + BOLD + String.format("%-9s", name) + RESET
                + " " + DIM + description + RESET
                + padding + PURPLE + "│" + RESET);
    }

    /** Clears the console. */
    private static void clearScreen()
    {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Waits for the user to press Enter before going back to the menu. */
    private static void waitForEnter(Scanner sc)
    {
        System.out.println();
        System.out.print(CYAN + "Press Enter to return to the menu..." + RESET);
        if (sc.hasNextLine())
            sc.nextLine();
    }

    /** Prints a section heading. */
    private static void heading(String title)
    {
        System.out.println();
        System.out.println("  " + BOLD + GREEN + title + RESET + " "
                + DIM + textChart.repeat('─', MENU_WIDTH - title.length() - 1) + RESET);
        System.out.println();
    }

    /** Prints a smaller heading inside a section. */
    private static void subheading(String title)
    {
        System.out.println();
        System.out.println("  " + BOLD + CYAN + title + RESET);
    }

    /** A 16-bit ID as 4 hex digits, or "????" if it could not be read. */
    private static String hex4(int id)
    {
        if (id < 0)
            return "????";
        return String.format("%04x", id);
    }

    /** The name, or 'fallback' if the name is null. */
    private static String nameOr(String name, String fallback)
    {
        if (name == null)
            return fallback;
        return name;
    }

    /** Displays the CPU model, caches, a load bar for each core and a graph of the average load. */
    private static void cpuMenu()
    {
        cpuReport.printWithHistory();
    }

    /** Displays total, used and available memory and the percentage in use. */
    private static void memoryMenu()
    {
        memReport.printWithHistory();
    }

    /** Displays every PCI device in address order, then the counts by vendor and by class. */
    private static void pciMenu()
    {
        heading("PCI devices");
        List<PciDevice> devices = PciAnalysis.sortByAddress(PciDevice.readAll());
        if (devices.isEmpty()) {
            System.out.println("  No PCI devices found");
            return;
        }
        System.out.println("  " + DIM + devices.size() + " devices, in address order" + RESET);
        System.out.println();

        // two lines per device: address, class and IDs, then the vendor and device names
        for (PciDevice d : devices) {
            String indent = textChart.repeat(' ', d.getAddress().length());
            System.out.println("  " + YELLOW + d.getAddress() + RESET
                    + "  " + BOLD + nameOr(d.getClassName(), "Unknown class") + RESET
                    + "  " + DIM + "[" + hex4(d.getVendorId()) + ":" + hex4(d.getDeviceId()) + "]" + RESET);
            System.out.println("  " + indent + "  " + nameOr(d.getVendorName(), "Unknown vendor")
                    + DIM + " · " + RESET + nameOr(d.getDeviceName(), "Unknown device"));
        }

        subheading("Devices by vendor");
        PciAnalysis.printCountByVendor(devices);

        subheading("Devices by class");
        PciAnalysis.printCountByClass(devices);
    }

    /** Displays every USB device with its bus, device number, IDs and names. */
    private static void usbMenu()
    {
        heading("USB devices");
        List<UsbDevice> devices = UsbInspector.readDevices();
        if (devices.isEmpty()) {
            System.out.println("  No USB devices found");
            System.out.println("  " + DIM + "(virtual machines and servers often have no USB at all)" + RESET);
            return;
        }
        // two lines per device: bus and device number, product and IDs, then the vendor
        for (UsbDevice usb : devices) {
            String where = "Bus " + String.format("%03d", usb.getBusNumber())
                    + " Device " + String.format("%03d", usb.getDeviceNumber());
            String indent = textChart.repeat(' ', where.length());
            System.out.println("  " + YELLOW + where + RESET
                    + "  " + BOLD + nameOr(usb.getProductName(), "Unknown product") + RESET
                    + "  " + DIM + "[" + hex4(usb.getVendorId()) + ":" + hex4(usb.getProductId()) + "]" + RESET);
            System.out.println("  " + indent + "  " + nameOr(usb.getVendorName(), "Unknown vendor"));
        }
    }

    /**
     * Displays the most important figures for the whole machine on one screen: CPU
     * load bars, memory bars, PCI devices by vendor and the USB devices, then a
     * one-sentence summary.
     */
    private static void summaryMenu()
    {
        heading("Summary");
        System.out.println("  " + DIM + "Measuring CPU load for one second..." + RESET);
        double[] loads = measureLoads();
        List<PciDevice> pciDevices = PciDevice.readAll();
        List<UsbDevice> usbDevices = UsbInspector.readDevices();
        clearScreen();
        heading("Summary");

        summaryCpu(loads);
        summaryMemory();

        subheading("PCI  " + RESET + DIM + pciDevices.size() + " devices, by vendor");
        if (pciDevices.isEmpty())
            System.out.println("  none found");
        else
            PciAnalysis.printCountByVendor(pciDevices);

        subheading("USB  " + RESET + DIM + usbDevices.size() + " devices");
        if (usbDevices.isEmpty())
            System.out.println("  none found");
        for (UsbDevice usb : usbDevices)
            System.out.println("  " + nameOr(usb.getProductName(), "Unknown product")
                    + DIM + " · " + nameOr(usb.getVendorName(), "Unknown vendor") + RESET);

        System.out.println();
        System.out.println("  " + DIM + textChart.repeat('─', MENU_WIDTH) + RESET);
        String model = cpuReport.getModel();
        String article = "a";
        if (!model.isEmpty() && "AEIOUaeiou".indexOf(model.charAt(0)) >= 0)
            article = "an";/*"an Intel" but "a Ryzen" */

        String sentence = String.format("This computer has %s %s CPU with %d cores, an average load of %s, "
                          + "%s of memory used out of %s, and %d PCI and %d USB devices.", /*Formatted summary */
                          article, model, loads.length, averageLoadText(loads),
                          memReport.formatKiB(memInfo.getUsed()), memReport.formatKiB(memInfo.getTotal()),
                          pciDevices.size(), usbDevices.size());
        printWrapped(sentence, MENU_WIDTH);
    }

    /** Prints the text over as many lines as needed, breaking only between words. */
    private static void printWrapped(String text, int width)
    {
        String line = "";
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                System.out.println("  " + line);
                line = "";
            }
            if (line.isEmpty())
                line = word;
            else
                line = line + " " + word;
        }
        if (!line.isEmpty())
            System.out.println("  " + line);
    }

    /** The CPU part of the summary: model, cores, caches, average load and a small bar for each core. */
    private static void summaryCpu(double[] loads)
    {
        subheading("CPU");
        System.out.println("  Model     " + cpuReport.getModel());
        System.out.println("  Cores     " + loads.length);
        System.out.println("  Caches    " + cpuReport.cacheSummary());

        double average = averageLoad(loads);
        if (average < 0) {
            System.out.println("  Load      not available");
            return;
        }
        System.out.println("  Average   " + textChart.percentBar(average * 100, 40));

        // two cores per line, each with a short bar
        for (int core = 0; core < loads.length; core++) {
            String label = String.format("Core %-3d", core);
            String bar = "not available";
            int barLength = bar.length();
            if (loads[core] >= 0) {
                bar = textChart.percentBar(loads[core] * 100, 12);
                // "[" + 12 bar characters + "] " + the percentage, without the colour codes
                barLength = 15 + String.format("%.1f%%", loads[core] * 100).length();
            }

            if (core % 2 == 0)
                System.out.print("  " + DIM + label + RESET + bar + textChart.repeat(' ', 21 - barLength));
            else
                System.out.println("     " + DIM + label + RESET + bar);
        }
        if (loads.length % 2 == 1)
            System.out.println();
    }

    /** The memory part of the summary: how much is used, an in-use bar and the breakdown bar. */
    private static void summaryMemory()
    {
        subheading("Memory");
        double percent = memReport.getPercentUsed();
        if (percent < 0) {
            System.out.println("  not available");
            return;
        }
        System.out.println("  Used      " + memReport.formatKiB(memInfo.getUsed())
                + " of " + memReport.formatKiB(memInfo.getTotal()));
        System.out.println("  In use    " + textChart.percentBar(percent, 40));
        memReport.printBreakdown();
    }

    public static Map<String, String> collectTemplateData() //Collects all the data required for the html display in a map
    {
        Map<String, String> data = new LinkedHashMap<>();
        double[] loads = measureLoads();
        double averageLoad = averageLoad(loads); //uses the average load method on "loads" which is gotten by the measureloads() function which calls back to cpuinfo.java
        long memoryUsed = memInfo.getUsed();
        long memoryTotal = memInfo.getTotal();
        updateCoreLoadHistory(loads);
        updateMemoryUsageHistory(memoryUsed, memoryTotal);

        data.put("CPU_LOAD", averageLoad < 0 ? "0" : String.format(Locale.ROOT, "%.1f", averageLoad * 100));
        data.put("CPU_CORES", Integer.toString(loads.length));
        data.put("CPU_CORE_GRAPHS", coreLoadGraphs());
        data.put("CPU_MODEL", cpuReport.getModel());
        data.put("MEM_USED", gigabytes(memoryUsed, 1024L * 1024));
        data.put("MEM_TOTAL", gigabytes(memoryTotal, 1024L * 1024));
        data.put("MEM_PERCENT", memoryPercent(memoryUsed, memoryTotal));
        data.put("MEMORY_GRAPH", memoryUsageGraph());
        data.put("MEM_AVAILABLE", gigabytes(memInfo.getAvailable(), 1024L * 1024));
        data.put("OS_NAME", System.getProperty("os.name", "unknown"));
        data.put("OS_ARCH", System.getProperty("os.arch", "unknown")); //Uses the system class to pull os and arch
        data.put("PCI_DEVICES", Integer.toString(pciInfo.getCount()));
        data.put("USB_DEVICES", Integer.toString(usbInfo.getCount()));

        try {
            FileStore disk = Files.getFileStore(Path.of("/"));
            long diskTotal = disk.getTotalSpace();
            long diskUsed = diskTotal - disk.getUsableSpace();
            data.put("DISK_USED", gigabytes(diskUsed, 1_000_000_000L));
            data.put("DISK_TOTAL", gigabytes(diskTotal, 1_000_000_000L));
            data.put("DISK_PERCENT", diskPercent(diskUsed, diskTotal));
        } catch (IOException e) {
            data.put("DISK_USED", "unavailable");
            data.put("DISK_TOTAL", "unavailable");
            data.put("DISK_PERCENT", "0");
        }

        data.put("TIMESTAMP", Instant.now().toString());
        
        return data;
    }

    private static synchronized void updateMemoryUsageHistory(long used, long total)
    {
        double usage = used < 0 || total <= 0 ? -1 : used * 1.0 / total;
        if (memoryUsageHistoryCount == SVG_HISTORY_SIZE)
            System.arraycopy(memoryUsageHistory, 1, memoryUsageHistory, 0, SVG_HISTORY_SIZE - 1);
        memoryUsageHistory[Math.min(memoryUsageHistoryCount, SVG_HISTORY_SIZE - 1)] = usage;
        if (memoryUsageHistoryCount < SVG_HISTORY_SIZE)
            memoryUsageHistoryCount++;
    }

    private static synchronized String memoryUsageGraph()
    {
        StringBuilder graph = new StringBuilder();
        graph.append("<div class=\"graph-card\"><div class=\"label\">Memory usage</div>");
        double current = memoryUsageHistoryCount == 0 ? -1 : memoryUsageHistory[memoryUsageHistoryCount - 1];
        graph.append("<div class=\"graph-value\">");
        if (current < 0)
            graph.append("not available");
        else
            graph.append(String.format(Locale.ROOT, "%.1f%%", current * 100));
        graph.append("</div><svg viewBox=\"0 0 640 130\" role=\"img\" aria-label=\"Memory usage history\">")
                .append("<line class=\"axis\" x1=\"8\" y1=\"112\" x2=\"632\" y2=\"112\"/>")
                .append("<line class=\"guide\" x1=\"8\" y1=\"60\" x2=\"632\" y2=\"60\"/>")
                .append("<line class=\"guide\" x1=\"8\" y1=\"8\" x2=\"632\" y2=\"8\"/>");
        if (current >= 0) {
            graph.append("<polyline class=\"memory-line\" points=\"");
            int samples = Math.min(memoryUsageHistoryCount, SVG_HISTORY_SIZE);
            for (int sample = 0; sample < samples; sample++) {
                double usage = Math.max(0, Math.min(1, memoryUsageHistory[sample]));
                double x = 8 + sample * 624.0 / Math.max(1, SVG_HISTORY_SIZE - 1);
                double y = 112 - usage * 104;
                graph.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
            }
            double currentX = 8 + (samples - 1) * 624.0 / Math.max(1, SVG_HISTORY_SIZE - 1);
            double currentY = 112 - Math.max(0, Math.min(1, current)) * 104;
            graph.append("\"/><circle class=\"memory-point\" cx=\"")
                    .append(String.format(Locale.ROOT, "%.1f", currentX)).append("\" cy=\"")
                    .append(String.format(Locale.ROOT, "%.1f", currentY)).append("\" r=\"4\"/>");
        }
        return graph.append("</svg><div class=\"graph-scale\"><span>0%</span><span>100%</span></div></div>").toString();
    }

    private static synchronized void updateCoreLoadHistory(double[] loads)//SVG Core
    {
        if (coreLoadHistory.length != loads.length) {
            coreLoadHistory = new double[loads.length][SVG_HISTORY_SIZE];
            coreLoadHistoryCount = 0;
        }

        for (int core = 0; core < loads.length; core++) {
            if (coreLoadHistoryCount == SVG_HISTORY_SIZE)
                System.arraycopy(coreLoadHistory[core], 1, coreLoadHistory[core], 0, SVG_HISTORY_SIZE - 1);
            coreLoadHistory[core][Math.min(coreLoadHistoryCount, SVG_HISTORY_SIZE - 1)] = loads[core];
        }
        if (coreLoadHistoryCount < SVG_HISTORY_SIZE)
            coreLoadHistoryCount++;
    }

    private static synchronized String coreLoadGraphs()
    {
        StringBuilder graphs = new StringBuilder();
        for (int core = 0; core < coreLoadHistory.length; core++) {
            double current = coreLoadHistoryCount == 0 ? -1 : coreLoadHistory[core][coreLoadHistoryCount - 1];
            graphs.append("<div class=\"graph-card\"><div class=\"label\">Core ")
                    .append(core).append("</div><div class=\"graph-value\">");
            if (current < 0)
                graphs.append("not available");
            else
                graphs.append(String.format(Locale.ROOT, "%.1f%%", current * 100));
            graphs.append("</div><svg viewBox=\"0 0 320 130\" role=\"img\" aria-label=\"CPU core ")
                    .append(core).append(" load history\"><line class=\"axis\" x1=\"8\" y1=\"112\" x2=\"312\" y2=\"112\"/>"
                    + "<line class=\"guide\" x1=\"8\" y1=\"60\" x2=\"312\" y2=\"60\"/>"
                    + "<line class=\"guide\" x1=\"8\" y1=\"8\" x2=\"312\" y2=\"8\"/>");
            if (current >= 0) {
                graphs.append("<polyline class=\"load-line\" points=\"");
                int samples = Math.min(coreLoadHistoryCount, SVG_HISTORY_SIZE);
                for (int sample = 0; sample < samples; sample++) {
                    double load = Math.max(0, Math.min(1, coreLoadHistory[core][sample]));
                    double x = 8 + sample * 304.0 / Math.max(1, SVG_HISTORY_SIZE - 1);
                    double y = 112 - load * 104;
                    graphs.append(String.format(Locale.ROOT, "%.1f,%.1f ", x, y));
                }
                double currentX = 8 + (samples - 1) * 304.0 / Math.max(1, SVG_HISTORY_SIZE - 1);
                double currentY = 112 - Math.max(0, Math.min(1, current)) * 104;
                graphs.append("\"/><circle class=\"load-point\" cx=\"")
                        .append(String.format(Locale.ROOT, "%.1f", currentX)).append("\" cy=\"")
                        .append(String.format(Locale.ROOT, "%.1f", currentY)).append("\" r=\"4\"/>");
            }
            graphs.append("</svg><div class=\"graph-scale\"><span>0%</span><span>100%</span></div></div>");
        }
        return graphs.toString();
    }

    private static String gigabytes(long bytes, long divisor)
    {
        return bytes < 0 ? "unavailable" : String.format(Locale.ROOT, "%.2f", (double) bytes / divisor); //Changes bytes to gigabytes
    }

    private static String memoryPercent(long used, long total)
    {
        return percent(used, total);
    }

    private static String diskPercent(long used, long total)
    {
        return percent(used, total);
    }

    private static String percent(long used, long total)
    {
        if (used < 0 || total <= 0)
            return "0";
        return String.format(Locale.ROOT, "%.1f", used * 100.0 / total); // calculates the 2 inputs as a percentage as a 1 decimal point float with a decimal seperator
    }

    /** The load on each core (0 to 1) over one second, or -1 for a core that cannot be read. */
    private static double[] measureLoads()
    {
        int cores = cpuInfo.getCount(); /*gets the no. of cores */
        long[] previousBusy = new long[cores];/*makes a list to store the busy and idle times of each core */
        long[] previousIdle = new long[cores];

        for (int core = 0; core < cores; core++) {
            previousBusy[core] = cpuInfo.getBusy(core);/* fills the list with each cores busy and idle times */
            previousIdle[core] = cpuInfo.getIdle(core);
        }

        try {
            Thread.sleep(1000);/*waits to take the next reading */
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double[] loads = new double[cores];
        for (int core = 0; core < cores; core++) {
            long busy = cpuInfo.getBusy(core);
            long idle = cpuInfo.getIdle(core);
            loads[core] = cpuInfo.calculateLoad(previousBusy[core], previousIdle[core], busy, idle);
        }
        return loads;
    }

    //work out the average of the valid cpu readings
    //returns -1 if there aren't any valid readings
    private static double averageLoad(double[] loads)
    {
        double totalLoad = 0;
        int readableCores = 0;

        for (double load : loads) {
            if (load >= 0 && load <= 1 && !Double.isNaN(load)) {
                totalLoad += load;
                readableCores++;
            }
        }

        if (readableCores == 0)
            return -1;

        return totalLoad / readableCores;
    }

    /** The average of the readable core loads as a percentage, or "not available". */
    private static String averageLoadText(double[] loads)
    {
        double average = averageLoad(loads);

        if (average < 0)
            return "not available";

        return String.format("%.1f%%", average * 100);
    }

    private static void writeReport(Path template, Path output, Map<String, String> stats) throws IOException { //This function writes the HTML Report
        String html = Files.readString(template, StandardCharsets.UTF_8);//first reads the html file as a string from the template path
 
        for (Map.Entry<String, String> e : stats.entrySet()) {//Then for each entry in my map 
            html = html.replace("{{" + e.getKey() + "}}", e.getValue());//It replaces the placeholder in the HTML File with the entry from the map
        }//This works as each key in the map is the same name as the placeholder value in the HTML Template
 
        Files.writeString(output, html, StandardCharsets.UTF_8);//Then it writes an output HTML File with the updated values
    }

}