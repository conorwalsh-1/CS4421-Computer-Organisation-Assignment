import java.util.List;
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

    public static void main(String[] args)
    {
        Scanner sc = new Scanner(System.in);

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
}