
/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java demo        (run it from the folder that contains the ids folder)
 */
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class demo {
    public static void main(String[] args) throws Exception {
        int cores = cpuInfo.getCount(); /* gets the no. of cores */
        String model = cpuInfo.getModel();/* gets the model name */
        if (model.equals("unknown")) {/*
                                       * Arm64 machines mightn't have a model name field so the getModel method cant
                                       * find it
                                       */
            String implementer = cpuInfo.getField("CPU implementer");/* I get the implementer instead */
            String part = cpuInfo.getField("CPU part");/* And the model */
            model = !implementer.isEmpty() || !part.isEmpty()/*
                                                              * Model = the Cpu implementer and the part number only if
                                                              * theres no implementer
                                                              */
                    ? "ARM implementer=" + (implementer.isEmpty() ? "not available" : implementer)/*
                                                                                                   * Says not available
                                                                                                   * if the implementer
                                                                                                   * var is empty
                                                                                                   */
                            + ", part=" + (part.isEmpty() ? "not available" : part)
                    : "not available";
        }

        long[] previousBusy = new long[cores];/* makes a list to store the busy and idle times of each core */
        long[] previousIdle = new long[cores];
        for (int core = 0; core < cores; core++) {
            previousBusy[core] = cpuInfo.getBusy(core);/* fills the list with each cores busy and idle times */
            previousIdle[core] = cpuInfo.getIdle(core);
        }
        Thread.sleep(1000);/* waits to take the next reading */

        double totalLoad = 0;
        int readableCores = 0;
        System.out.println("cores:  " + cores);
        System.out.println("model:  " + model);
        for (int core = 0; core < cores; core++) {
            long busy = cpuInfo.getBusy(core);
            long idle = cpuInfo.getIdle(core);
            double load = cpuInfo.calculateLoad(previousBusy[core], previousIdle[core], busy, idle);
            if (load < 0) {
                System.out.printf("core %d: load=not available%n", core);/* checks the load is greate than 0 */
            } else {
                totalLoad += load;/* totals all the loads on each core */
                readableCores++;
                System.out.printf("core %d: load=%.1f%%%n", core, load * 100);/* Returns the load on each core */
            }
        }
        System.out.printf("average load: %s%n", readableCores == 0
                ? "not available"
                : String.format("%.1f%%", totalLoad / readableCores * 100));/* Return the average load */
        System.out.println("memory: total=" + memInfo.getTotal() + " KiB, used=" + memInfo.getUsed() + " KiB");

        List<PciDevice> pciDevices = PciDevice.readAll(); // list of PciDevice objects
        System.out.println("PCI devices: " + pciDevices.size());

        for (PciDevice device : pciDevices) {
            System.out.println("  " + device.describe()); // print all PCI descriptions
        }

        idLookup usb = null;
        try {
            usb = idLookup.usb();
        } catch (IOException e) {
            System.err.println("USB names unavailable: " + e.getMessage());
        }

        System.out.println("USB devices: " + usbInfo.getCount());

        List<UsbDevice> usbDevices = new ArrayList<>(); // list of UsbDevice objects

        for (int i = 0; i < usbInfo.getCount(); i++) { // loop through all USB devices, gets their data and creates a
                                                       // UsbDevice object for each
            int vendorId = usbInfo.getVendorId(i);
            int productId = usbInfo.getProductId(i);

            String vendorName = usb == null ? null : usb.vendorName(vendorId);
            String productName = usb == null ? null : usb.deviceName(vendorId, productId);

            usbDevices.add(new UsbDevice(usbInfo.getBusNumber(i), usbInfo.getDeviceNumber(i), vendorId, productId,
                    vendorName, productName));
        }

        for (UsbDevice device : usbDevices) {
            System.out.println("  " + device.describe()); // print all USB descriptions
        }
    }
}
