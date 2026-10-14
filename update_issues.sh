#!/usr/bin/env bash
# Updates the card checklists to match the provided starter code (hw-starter).
# Replaces the body of cards 1, 2, 3, 4, 5, 7, 9 and 12. Ticked boxes on those
# cards are reset, so run it before people start ticking things off.
#
# Run from inside your cloned repo:   bash update_issues.sh
# Or point at a repo explicitly:      REPO=owner/repo bash update_issues.sh

set -e

REPO="${REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
echo "Updating issues in $REPO"

update() {
  local title="$1" body="$2"
  local num
  num=$(gh issue list --repo "$REPO" --state all --limit 200 --json number,title \
        -q ".[] | select(.title == \"$title\") | .number" | head -n1)
  if [ -z "$num" ]; then
    echo "  Not found, skipping: $title"
    return
  fi
  gh issue edit "$num" --repo "$REPO" --body "$body" >/dev/null
  echo "  Updated #$num: $title"
}

update "1. Project setup" "$(cat <<'EOF'
Whole team: assign everyone.

- [ ] Create GitHub repo and add all four members
- [ ] Add starter code (`src/`, `ids/`) and `.gitignore`
- [ ] Everyone clones, builds and runs it: `javac -d out src/*.java` then `java -cp out demo`
- [ ] Everyone can log into **alderaan** (the starter code reads `/proc` and `/sys`, which WSL2 doesn't provide, so run and test there or in a Linux VM)
- [ ] Everyone runs `cat` on `/proc/cpuinfo`, `/proc/stat`, `/proc/meminfo`, `/sys/bus/pci/devices/` on alderaan to see the raw data
- [ ] Each person reads the provided class for their area (`cpuInfo`, `memInfo`, `pciInfo`, `usbInfo`, `idLookup`, `sysFile`) and explains it to the group
- [ ] Agree on package structure and a `Main` class (keep `demo` as a reference only)
EOF
)"

update "2. CPU: cores, model and load" "$(cat <<'EOF'
Starter code gives `cpuInfo.getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)`.

- [ ] Report core count and model
- [ ] arm64 fallback: when `getModel()` returns "unknown", use `getField("CPU implementer")` / `getField("CPU part")` or say "not available"
- [ ] Read `getBusy`/`getIdle` for every core, wait ~1s (`Thread.sleep(1000)`), read again
- [ ] Write `calculateLoad(prevBusy, prevIdle, busy, idle)`: load = Δbusy / (Δbusy + Δidle)
- [ ] Load per core and average across all cores
- [ ] Handle -1 (value could not be read)
EOF
)"

update "3. Memory: total, used, percentage" "$(cat <<'EOF'
Starter code gives `memInfo.getTotal()`, `getAvailable()`, `getUsed()` (all in KiB).

- [ ] Check how `getUsed()` is calculated (should be Total − Available) and be able to explain it
- [ ] Calculate % in use
- [ ] Convert KiB to MiB/GiB for display
- [ ] Handle -1 (value could not be read)
EOF
)"

update "4. USB device list" "$(cat <<'EOF'
Starter code gives `usbInfo.getCount()`, `getVendorId(i)`, `getProductId(i)`, `getManufacturer(i)`, `getProduct(i)`, `getBusNumber(i)`, `getDeviceNumber(i)`.

- [ ] Create `UsbDevice` class (bus, device number, vendor ID, product ID, names)
- [ ] Fill a `List<UsbDevice>` from `usbInfo`
- [ ] Note: root hubs are included in the list, so decide whether to show or label them
- [ ] When `getCount()` is 0, print "No USB devices found" and keep running
- [ ] Compare with `lsusb` on a machine that has USB
EOF
)"

update "5. PCI device list" "$(cat <<'EOF'
Starter code gives `pciInfo.getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`.

- [ ] Create `PciDevice` class (bus, device, function, vendor ID, device ID, class code, names)
- [ ] Fill a `List<PciDevice>` from `pciInfo`
- [ ] Be able to explain how the address `0000:00:02.0` maps to bus/device/function
- [ ] Compare with `lspci -nn` on alderaan
EOF
)"

update "7. Name lookup (pci.ids and usb.ids)" "$(cat <<'EOF'
Starter code: `idLookup.pci()` / `idLookup.usb()` with `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)`. These return `null` when an ID isn't found.

- [ ] Read `idLookup` and be able to explain how it parses the files (vendor lines, tab-indented device lines, `C` class section)
- [ ] Add vendor and device names to `PciDevice` and `UsbDevice`
- [ ] When a lookup returns `null`, show the raw hex ID instead
- [ ] Handle a missing ids file without crashing (constructor throws `IOException`)
EOF
)"

update "9. Robustness and testing" "$(cat <<'EOF'
Whole team: assign everyone. Run on **alderaan** or a Linux VM; WSL2 doesn't have the `/proc` and `/sys` files.

- [ ] Handle -1 / empty / `null` values from the starter classes everywhere they're used
- [ ] Missing cache folder, no USB, no `model name`, missing ids files: no crash
- [ ] Runs headless on alderaan
- [ ] Output checked against `lscpu`, `free -h`, `lspci -nn`, `lsusb`
EOF
)"

update "12. Extra hardware detail" "$(cat <<'EOF'
- [ ] Show PCI class names using `idLookup.className(classCode)` (raw code when `null`)
- [ ] Report cache info from `/sys/devices/system/cpu/cpu0/cache/indexN` (level, type, size, coherency_line_size); not in the starter code, so write a small reader (`sysFile` may help)
- [ ] Handle a missing cache folder
EOF
)"

echo "Done"
