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
 *
 * Build:  javac -d out src/*.java
 * Run:    java -cp out demo
 */
public class demo
{
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String PURPLE = "\u001B[35m";

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

    /** Prints the title at the top of the menu. */
    private static void printBanner()
    {
        System.out.println(RED);
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║      CS4421  HARDWARE  INSPECTOR     ║");
        System.out.println("╚══════════════════════════════════════╝" + RESET);
    }

    /** Prints the list of options. */
    private static void printMenu()
    {
        System.out.println(PURPLE);
        System.out.println("  1. CPU");
        System.out.println("  2. Memory");
        System.out.println("  3. PCI");
        System.out.println("  4. USB");
        System.out.println("  5. Summary");
        System.out.println("  6. Make HTML Report");
        System.out.println("  0. Exit" + RESET);
        System.out.println();
        System.out.print("Pick an option: ");
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
        System.out.println(GREEN + title + RESET);
        System.out.println();
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
        heading("PCI");
        List<PciDevice> devices = PciAnalysis.sortByAddress(PciDevice.readAll());
        System.out.println("PCI devices: " + devices.size());
        for (PciDevice d : devices)
            System.out.println("  " + d.describe());

        System.out.println();
        System.out.println("Devices by vendor:");
        PciAnalysis.printCountByVendor(devices);

        System.out.println();
        System.out.println("Devices by class:");
        PciAnalysis.printCountByClass(devices);
    }

    /** Displays every USB device with its bus, device number, IDs and names. */
    private static void usbMenu()
    {
        heading("USB");
        List<UsbDevice> devices = UsbInspector.readDevices();
        System.out.println("USB devices: " + devices.size());
        if (devices.isEmpty()) {
            System.out.println("No USB devices found");
            return;
        }

        for (UsbDevice usb : devices)
            System.out.println("  " + usb.describe());
    }

    /** Displays a one-paragraph summary of the whole machine. */
    private static void summaryMenu()
    {
        heading("Summary");
        double[] loads = measureLoads();
        System.out.printf("This computer has a %s CPU with %d cores, an average load of %s, "
                          + "%s of memory used out of %s, and %d PCI and %d USB devices.%n", /*Formatted summary */
                          cpuReport.getModel(), loads.length, averageLoadText(loads),
                          memReport.formatKiB(memInfo.getUsed()), memReport.formatKiB(memInfo.getTotal()),
                          pciInfo.getCount(), usbInfo.getCount());
    }

    public static Map<String, String> collectTemplateData() //Collects all the data required for the html display in a map
    {
        Map<String, String> data = new LinkedHashMap<>();
        double[] loads = measureLoads();
        double averageLoad = averageLoad(loads); //uses the average load method on "loads" which is gotten by the measureloads() function which calls back to cpuinfo.java
        long memoryUsed = memInfo.getUsed();
        long memoryTotal = memInfo.getTotal();

        data.put("CPU_LOAD", averageLoad < 0 ? "0" : String.format(Locale.ROOT, "%.1f", averageLoad * 100));
        data.put("CPU_CORES", Integer.toString(loads.length));
        data.put("CPU_MODEL", cpuReport.getModel());
        data.put("MEM_USED", gigabytes(memoryUsed, 1024L * 1024));
        data.put("MEM_TOTAL", gigabytes(memoryTotal, 1024L * 1024));
        data.put("MEM_PERCENT", memoryPercent(memoryUsed, memoryTotal));
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