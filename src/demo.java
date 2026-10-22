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
        menuLine(inner, "5", "Summary", "the whole machine in one line");
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
        System.out.println("  " + DIM + devices.size() + " devices, numbered the same way as lsusb" + RESET);
        System.out.println();

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