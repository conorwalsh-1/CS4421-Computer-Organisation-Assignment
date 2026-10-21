/**
 * A short example that shows how to call each provided class. It prints raw
 * numbers and a few names. It is NOT a solution to the assignment: you still
 * have to measure load, store the devices, sort them, analyse them and present
 * the results.
 *
 * Build:  javac *.java
 * Run:    java demo        (run it from the folder that contains the ids folder)
 */
public class demo
{
    public static void main(String[] args) throws Exception
    {
        int cores = cpuInfo.getCount(); /*gets the no. of cores */
        String model = cpuInfo.getModel();/*gets the model name */
        if (model.equals("unknown")) {/*Arm64 machines mightn't have a model name field so the getModel method cant find it */
            String implementer = cpuInfo.getField("CPU implementer");/*I get the implementer instead */
            String part = cpuInfo.getField("CPU part");/*And the model */
            model = !implementer.isEmpty() || !part.isEmpty()/*Model = the Cpu implementer and the part number only if theres no implementer */
                    ? "ARM implementer=" + (implementer.isEmpty() ? "not available" : implementer)/*Says not available if the implementer var is empty  */
                      + ", part=" + (part.isEmpty() ? "not available" : part)
                    : "not available";
        }

        long[] previousBusy = new long[cores];/*makes a list to store the busy and idle times of each core */
        long[] previousIdle = new long[cores];
        for (int core = 0; core < cores; core++) {
            previousBusy[core] = cpuInfo.getBusy(core);/*fills the list with each cores busy and idle times */
            previousIdle[core] = cpuInfo.getIdle(core);
        }
        Thread.sleep(1000);/*waits to take the next reading */

        double totalLoad = 0;
        int readableCores = 0;
        System.out.println("cores:  " + cores);
        System.out.println("model:  " + model);
        for (int core = 0; core < cores; core++) {
            long busy = cpuInfo.getBusy(core);
            long idle = cpuInfo.getIdle(core);
            double load = cpuInfo.calculateLoad(previousBusy[core], previousIdle[core], busy, idle);
            if (load < 0) {
                System.out.printf("core %d: load=not available%n", core);/*checks the load is greate than 0 */
            } else {
                totalLoad += load;/*totals all the loads on each core */
                readableCores++;
                System.out.printf("core %d: load=%.1f%%%n", core, load * 100);/*Returns the load on each core */
            }
        }
        System.out.printf("average load: %s%n", readableCores == 0
                ? "not available"
                : String.format("%.1f%%", totalLoad / readableCores * 100));/*Return the average load */
        System.out.println("memory: total=" + memInfo.getTotal() + " KiB, used=" + memInfo.getUsed() + " KiB");

        idLookup pci = idLookup.pci();
        System.out.println("PCI devices: " + pciInfo.getCount());
        for (int i = 0; i < pciInfo.getCount(); i++) {
            int vendor = pciInfo.getVendorId(i);
            int device = pciInfo.getDeviceId(i);
            System.out.printf("  %s  %04x:%04x  %s %s%n", pciInfo.getAddress(i), vendor, device,
                              pci.vendorName(vendor), pci.deviceName(vendor, device));
        }

        idLookup usb = idLookup.usb();
        System.out.println("USB devices: " + usbInfo.getCount());
        for (int i = 0; i < usbInfo.getCount(); i++) {
            int vendor = usbInfo.getVendorId(i);
            int product = usbInfo.getProductId(i);
            System.out.printf("  Bus %03d Device %03d: ID %04x:%04x  %s %s%n", usbInfo.getBusNumber(i),
                              usbInfo.getDeviceNumber(i), vendor, product,
                              usb.vendorName(vendor), usb.deviceName(vendor, product));
        }
    }
}
