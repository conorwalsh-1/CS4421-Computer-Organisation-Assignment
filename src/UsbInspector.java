import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

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
}