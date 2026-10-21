import java.util.ArrayList;
import java.util.List;

/**
 * Sorting and summary counts for a list of PCI devices (from PciDevice.readAll()).
 *
 * The sort methods return a new sorted list and leave the original list alone.
 * The count methods print one line per vendor or class.
 * A device whose vendor or class is not in pci.ids is counted as "Unknown".
 */
public class PciAnalysis {

    /** A copy of the list sorted alphabetically by vendor name (A to Z, ignoring case). */
    public static List<PciDevice> sortByVendor(List<PciDevice> devices) {
        List<PciDevice> sorted = new ArrayList<>(devices);

        // bubble sort: keep swapping neighbours that are in the wrong order
        for (int pass = 0; pass < sorted.size() - 1; pass++) {
            for (int i = 0; i < sorted.size() - 1 - pass; i++) {
                String first = nameOrUnknown(sorted.get(i).getVendorName());
                String second = nameOrUnknown(sorted.get(i + 1).getVendorName());
                if (first.compareToIgnoreCase(second) > 0) {
                    swap(sorted, i, i + 1);
                }
            }
        }
        return sorted;
    }

    /** A copy of the list sorted numerically by bus, then device, then function. */
    public static List<PciDevice> sortByAddress(List<PciDevice> devices) {
        List<PciDevice> sorted = new ArrayList<>(devices);

        for (int pass = 0; pass < sorted.size() - 1; pass++) {
            for (int i = 0; i < sorted.size() - 1 - pass; i++) {
                if (comesAfter(sorted.get(i), sorted.get(i + 1))) {
                    swap(sorted, i, i + 1);
                }
            }
        }
        return sorted;
    }

    /** Prints how many devices each vendor has, for example "Intel Corporation: 5". */
    public static void printCountByVendor(List<PciDevice> devices) {
        // names[i] is a vendor and counts[i] is how many devices it has.
        // There can't be more vendors than devices, so that's the array size.
        String[] names = new String[devices.size()];
        int[] counts = new int[devices.size()];
        int used = 0; // how many slots of the arrays are filled in

        for (PciDevice d : devices) {
            String name = nameOrUnknown(d.getVendorName());

            // look for this vendor in the names we already have
            int found = -1;
            for (int i = 0; i < used; i++) {
                if (names[i].equals(name)) {
                    found = i;
                }
            }

            if (found != -1) {
                counts[found]++;
            } else {
                names[used] = name;
                counts[used] = 1;
                used++;
            }
        }

        for (int i = 0; i < used; i++) {
            System.out.println(names[i] + ": " + counts[i]);
        }
    }

    /** Prints how many devices there are of each class, for example "Audio device: 2". */
    public static void printCountByClass(List<PciDevice> devices) {
        String[] names = new String[devices.size()];
        int[] counts = new int[devices.size()];
        int used = 0;

        for (PciDevice d : devices) {
            String name = nameOrUnknown(d.getClassName());

            int found = -1;
            for (int i = 0; i < used; i++) {
                if (names[i].equals(name)) {
                    found = i;
                }
            }

            if (found != -1) {
                counts[found]++;
            } else {
                names[used] = name;
                counts[used] = 1;
                used++;
            }
        }

        for (int i = 0; i < used; i++) {
            System.out.println(names[i] + ": " + counts[i]);
        }
    }

    /** True if device a has a higher address than device b, so a should go after b. */
    private static boolean comesAfter(PciDevice a, PciDevice b) {
        if (a.getBus() != b.getBus()) {
            return a.getBus() > b.getBus();
        }
        if (a.getDevice() != b.getDevice()) {
            return a.getDevice() > b.getDevice();
        }
        return a.getFunction() > b.getFunction();
    }

    private static void swap(List<PciDevice> list, int i, int j) {
        PciDevice temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }

    private static String nameOrUnknown(String name) {
        if (name == null) {
            return "Unknown";
        }
        return name;
    }
}
