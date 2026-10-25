# USB Device List - Sample Output

This is the output I got from testing the usb device list on my laptop running arch linux

## Running the program

```bash
javac -d out src/*.java
java -cp out UsbDemo
```

## Output

```text
USB devices: 8
Bus 1 Device 1: ID 1d6b:0002 Linux Foundation 2.0 root hub
Bus 1 Device 2: ID 05e3:0610 Genesys Logic, Inc. Hub
Bus 1 Device 3: ID 048d:c101 Integrated Technology Express, Inc. Unknown product
Bus 2 Device 1: ID 1d6b:0003 Linux Foundation 3.0 root hub
Bus 2 Device 2: ID 05e3:0620 Genesys Logic, Inc. GL3523 Hub
Bus 3 Device 1: ID 1d6b:0002 Linux Foundation 2.0 root hub
Bus 3 Device 2: ID 0bda:4852 Realtek Semiconductor Corp. Unknown product
Bus 4 Device 1: ID 1d6b:0003 Linux Foundation 3.0 root hub
```

## Comparing with lsusb

Running lsusb also returned similar results

## Testing with no USB devices

I used a separate empty directory to simulate a machine without any usb devices

```bash
mkdir -p /tmp/usb-test/sys/bus/usb/devices
java -Dhw.root=/tmp/usb-test -cp out UsbDemo
```

Output:

```text
USB devices: 0
No USB devices found
```

Ran successfully without crashing