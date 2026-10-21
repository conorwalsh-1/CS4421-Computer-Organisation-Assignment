//stores the information for one usb device
//the details are passed in when the object is created

class UsbDevice {
    //why private? stops other classes from directly accessing the variables
    //why final? the values can't be reassigned after the constructor runs
    private final int busNumber;
    private final int deviceNumber;
    private final int vendorId;
    private final int productId;
    private final String vendorName;
    private final String productName;

    //constructor
    
    //sets the values for a new USB device
    UsbDevice(int busNumber, int deviceNumber, int vendorId, int productId, String vendorName, String productName){
        this.busNumber = busNumber;
        this.deviceNumber = deviceNumber;
        this.vendorId = vendorId;
        this.productId = productId;
        this.vendorName = vendorName;
        this.productName = productName;
    }

    //getters
    int getBusNumber(){
        return busNumber;
    }

    int getDeviceNumber(){
        return deviceNumber;
    }

    int getVendorId(){
        return vendorId;
    }

    int getProductId(){
        return productId;
    }

    String getVendorName(){
        return vendorName;
    }

    String getProductName(){
        return productName;
    }
}