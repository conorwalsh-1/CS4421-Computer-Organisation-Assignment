import java.util.List;
//made this separately to test the usb device list
//without changing the main program
class UsbDemo {
    public static void main(String[] args) {
        //get the list of usb devices
        List<UsbDevice> devices = UsbInspector.readDevices();
        System.out.println("USB devices: " + devices.size());

        //if there are no devices, print a message and stop
        if (devices.isEmpty()) {
            System.out.println("No USB devices found");
            return;
        }

        //go through each device in the list
        for (UsbDevice usb : devices) {
            String vendorName = usb.getVendorName();
            String productName = usb.getProductName();

            //if the names aren't found, use a default
            if (vendorName == null) {
                vendorName = "Unknown vendor";
            }

            if (productName == null) {
                productName = "Unknown product";
            }

            //why use hex? usb ids are normally displayed in hexadecimal
            //like they are in lsusb
            String vendorId = "N/A";
            String productId = "N/A";

            if (usb.getVendorId() >= 0) {
                vendorId = String.format("%04x", usb.getVendorId());
            }

            if (usb.getProductId() >= 0) {
                productId = String.format("%04x", usb.getProductId());
            }

            //print the device information
            System.out.println("Bus " + usb.getBusNumber() + " Device " + usb.getDeviceNumber() + ": ID " + vendorId + ":" + productId + " " + vendorName + " " + productName);
        }
    }
}