import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

class UsbInspector {
    //gets the current USB devices and stores their details in a list
    static List<UsbDevice> readDevices() {
        List<UsbDevice> devices = new ArrayList<>();

        //get the usb devices, refreshes the list in case usb devices have been added / removed
        usbInfo.rescan();
        int count = usbInfo.getCount();

        //load the usb names
        idLookup lookup = null;

        //try to load the USB names database
        //if it fails we can still use the numeric ids
        try {
            lookup = idLookup.usb();
        } catch (IOException e) {
            System.out.println("Could not load USB names");
        }

        for (int i = 0; i < count; i++) {
            int bus = usbInfo.getBusNumber(i);
            int device = usbInfo.getDeviceNumber(i);
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);

            String vendorName = null;
            String productName = null;

            //find the names if they're available
            if (lookup != null && vendor >= 0) {
                vendorName = lookup.vendorName(vendor);

                if (product >= 0) {
                    productName = lookup.deviceName(vendor, product);
                }
            }

            UsbDevice usb = new UsbDevice(bus, device, vendor, product, vendorName, productName);
            devices.add(usb);
        }

        return devices;
    }

    //show the speed and class of each USB device
    static void showBusInfo() {
        File[] devices = new File("/sys/bus/usb/devices").listFiles();

        if (devices == null) return;

        for (File device : devices) {
            try {
                File speedFile = new File(device, "speed");
                if (!speedFile.exists()) continue;

                String speed = Files.readString(speedFile.toPath()).trim();
                String deviceClass = Files.readString(new File(device, "bDeviceClass").toPath()).trim();

                double mbps = Double.parseDouble(speed);
                String version = "Unknown";

                if (mbps == 1.5 || mbps == 12) version = "USB 1.x";
                else if (mbps == 480) version = "USB 2.0";
                else if (mbps >= 5000) version = "USB 3.x or newer";

                System.out.println(device.getName() + ": " + speed + " Mbps (" + version + ")");
                System.out.println("  Class: 0x" + deviceClass);
                if (deviceClass.equalsIgnoreCase("00")) {
                   System.out.println("  Class set by interface");
                }

                if (device.getName().startsWith("usb")) {
                    System.out.println("  Root hub");
                }

            } catch (IOException | NumberFormatException e) {
                System.out.println("Could not read " + device.getName());
            }
        }
    }
}
