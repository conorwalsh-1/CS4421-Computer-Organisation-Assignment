import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns vendor, device and class numbers into names, using the pci.ids and
 * usb.ids
 * files. Both files list a vendor on a line by itself, followed by its devices
 * on
 * lines that start with a tab:
 *
 * 8086 Intel Corporation
 * 1237 440FX - 82441FX PMC [Natoma]
 *
 * Use it like this:
 *
 * idLookup pci = idLookup.pci();
 * System.out.println(pci.vendorName(0x8086));
 * System.out.println(pci.deviceName(0x8086, 0x1237));
 */
public class idLookup {
    private final Map<Integer, String> vendors = new HashMap<>(); // maps vendor IDs to vendor names
    private final Map<Long, String> devices = new HashMap<>(); // maps combined vendor and device IDs to device names.
                                                               // Combined as different vendors can share a device ID
    private final Map<Integer, String> baseClasses = new HashMap<>();
    private final Map<Integer, String> classes = new HashMap<>(); // pci.ids only

    /**
     * Reads all lines from a pci.ids or usb.ids file. Throws IOException if it
     * cannot be read.
     */
    public idLookup(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file); // Reads all lines from file, splits them into lines to return
                                                       // them as strings
        int vendor = -1;
        int baseClass = -1;
        boolean inClasses = false; // true if in the class section of pci.ids

        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) // skip empty lines and comments
                continue;

            if (line.startsWith("C ")) {
                // class line e.g. "C 06 Bridge"
                inClasses = true;
                baseClass = Integer.parseInt(line.substring(2, 4), 16); // takes the hex number after C and converts it
                                                                        // to decimal and stores it in baseClass
                baseClasses.put(baseClass, line.substring(6).strip()); //stores the base class separately
                                                                        // lower bits for the subclass so they can be
                                                                        // stored in same map and takes class name to
                                                                        // store in classes map
            } else if (inClasses && line.startsWith("\t") && !line.startsWith("\t\t")) {
                // subclass line e.g. "\t00 Host bridge"
                if (baseClass >= 0 && line.length() > 5 && isHex(line, 1, 3)) // checks if line fits format of subclass
                                                                              // line and has a valid baseClass
                    classes.put((baseClass << 8) | Integer.parseInt(line.substring(1, 3), 16),
                            line.substring(5).strip());
            } else if (inClasses) {
                if (Character.isUpperCase(line.charAt(0)) && !line.startsWith("C ")) // end of class section
                    baseClass = -1; // keep inClasses true to not falsely parse other sections as vendors or devices
            } else if (isHex(line, 0, 4) && line.length() > 6) { // e.g. "8086 Intel Corporation"
                vendor = Integer.parseInt(line.substring(0, 4), 16); // converts first 4 characters of line to decimal
                                                                     // and stores it in vendor
                vendors.put(vendor, line.substring(6).strip()); // stores vendor name with vendor ID in vendors map
            } else if (line.startsWith("\t") && !line.startsWith("\t\t")
                    && vendor >= 0 && isHex(line, 1, 5) && line.length() > 7) {
                int device = Integer.parseInt(line.substring(1, 5), 16); // device ID = 4 hex digits after \t as decimal
                devices.put(key(vendor, device), line.substring(7).strip()); // stores device name with combined ID in
                                                                             // devices map
            }
        }
    }

    /** Load pci.ids from the usual places: ./ids, then the system copies. */
    public static idLookup pci() throws IOException {
        return find("pci.ids", "/usr/share/misc/pci.ids", "/usr/share/hwdata/pci.ids");
    }

    /** Load usb.ids from the usual places: ./ids, then the system copies. */
    public static idLookup usb() throws IOException {
        return find("usb.ids", "/usr/share/misc/usb.ids", "/usr/share/hwdata/usb.ids",
                "/var/lib/usbutils/usb.ids");
    }

    /** Searches for specified id files in listed system paths */
    private static idLookup find(String name, String... systemPaths) throws IOException {
        Path local = Path.of("ids", name);
        if (Files.isReadable(local))
            return new idLookup(local);
        for (String p : systemPaths)
            if (Files.isReadable(Path.of(p)))
                return new idLookup(Path.of(p));
        throw new IOException("cannot find " + name + " (looked in ./ids and the system folders)");
    }

    /** The vendor's name, or null if the number is not in the file. */
    public String vendorName(int vendorId) {
        return vendors.get(vendorId);
    }

    /** The device's name for that vendor, or null if it is not in the file. */
    public String deviceName(int vendorId, int deviceId) {
        return devices.get(key(vendorId, deviceId));
    }

    /**
     * The name of a PCI class. Pass the 24-bit class code; the sub class name is
     * returned if it is known, otherwise the base class name, otherwise null.
     */
    public String className(int classCode) {
        String sub = classes.get(classCode >> 8);
        return sub != null ? sub : baseClasses.get(classCode >> 16);
    }

    /** How many vendors were read. */
    public int vendorCount() {
        return vendors.size();
    }

    /**
     * Takes the 16-bit vendor and device IDs and combines. High 16 bits are vendor,
     * low 16 bits are device.
     */
    private static long key(int vendor, int device) {
        return ((long) vendor << 16) | device;
    }

    /** Check if a given string is hex value matching IDs used in files */
    private static boolean isHex(String s, int from, int to) {
        if (s.length() < to)
            return false;
        for (int i = from; i < to; i++)
            if (Character.digit(s.charAt(i), 16) < 0 || Character.isUpperCase(s.charAt(i)))
                return false;
        return true;
    }
}
