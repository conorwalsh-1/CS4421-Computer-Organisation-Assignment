import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * One PCI device, with its address, ID numbers and the names looked up for them.
 *
 * An address is domain:bus:device.function. The bus number is 8 bits (up to 256
 * buses), the device number 5 bits (up to 32 devices on a bus) and the function
 * number 3 bits (up to 8 functions in a device). Every device has a function 0;
 * a multi-function device adds more functions under the same device number.
 * PCI Express uses the same scheme.
 */
public class PciDevice {
    private final String address;
    private final int bus;
    private final int device;
    private final int function;
    private final int vendorId;
    private final int deviceId;
    private final int classCode;
    private final String vendorName;
    private final String deviceName;
    private final String className;
    /**
     * Constructor for each PCI device instance
     */
    public PciDevice(String address, int bus, int device, int function, int vendorId, int deviceId,
            int classCode, String vendorName, String deviceName, String className) {
        this.address = address;
        this.bus = bus;
        this.device = device;
        this.function = function;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;
        this.vendorName = vendorName;
        this.deviceName = deviceName;
        this.className = className;
    }

    /**
     * Reads every PCI device from pciInfo and looks up its names in pci.ids.
     * If pci.ids cannot be found the devices are still listed, with null names.
     */
    public static List<PciDevice> readAll() {
        idLookup ids;
        try {
            ids = idLookup.pci();
        } catch (IOException e) {
            ids = null;
        }

        List<PciDevice> list = new ArrayList<>();
        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendorId = pciInfo.getVendorId(i);
            int deviceId = pciInfo.getDeviceId(i);
            int classCode = pciInfo.getClassCode(i);

            String vendorName = null;
            String deviceName = null;
            String className = null;
            if (ids != null) {
                vendorName = ids.vendorName(vendorId);
                deviceName = ids.deviceName(vendorId, deviceId);
                className = ids.className(classCode);
            }

            PciDevice d = new PciDevice(pciInfo.getAddress(i), pciInfo.getBus(i), pciInfo.getDevice(i),
                    pciInfo.getFunction(i), vendorId, deviceId, classCode, vendorName, deviceName, className);
            list.add(d);
        }
        return list;
    }

    public String getAddress() {return address;}
    public int getBus() {return bus;}
    public int getDevice() {return device;}
    public int getFunction() {return function;}
    public int getVendorId() {return vendorId;}
    public int getDeviceId() {return deviceId;}
    public int getClassCode() {return classCode;}

    /gi** The vendor's name, or null if it is not in pci.ids. */
    public String getVendorName() {return vendorName;}

    /** The device's name, or null if it is not in pci.ids. */
    public String getDeviceName() {return deviceName;}

    /** The class name (sub class if known, else base class), or null. */
    public String getClassName() {return className;}

    /** One line describing the device: address, class, names and IDs. */
    public String describe() {
        return address + " " + orUnknown(className) + ": " + orUnknown(vendorName) + " "
                + orUnknown(deviceName) + " [" + hex(vendorId) + ":" + hex(deviceId) + "]";
    }

    private static String orUnknown(String name) {
        if (name == null) {
            return "Unknown";
        }
        return name;
    }

    /** A number in hexadecimal, padded with zeros to 4 digits. */
    private static String hex(int n) {
        String s = Integer.toHexString(n);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
}
