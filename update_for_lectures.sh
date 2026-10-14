#!/usr/bin/env bash
# Aligns the cards with the CS4421 lecture content (W1-W6).
#   - Updates cards 2, 5, 7, 13, 14, 17 and 18
#   - Creates new card 19 (bus details: PCI bridges and USB speeds)
# Replacing a card's text resets its ticked boxes, so run this before people
# start ticking things off. Safe to run twice: card 19 is only created once.
#
# Run from inside your cloned repo:   bash update_for_lectures.sh
# Or point at a repo explicitly:      REPO=owner/repo bash update_for_lectures.sh

set -e

REPO="${REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
echo "Updating issues in $REPO"

find_issue() {
  gh issue list --repo "$REPO" --state all --limit 200 --json number,title \
    -q ".[] | select(.title == \"$1\") | .number" | head -n1
}

update() {
  local title="$1" body="$2" num
  num=$(find_issue "$title")
  if [ -z "$num" ]; then
    echo "  Not found, skipping: $title"
    return
  fi
  gh issue edit "$num" --repo "$REPO" --body "$body" >/dev/null
  echo "  Updated #$num: $title"
}

create_once() {
  local title="$1" milestone="$2" labels="$3" body="$4" num
  num=$(find_issue "$title")
  if [ -n "$num" ]; then
    echo "  Already exists (#$num), skipping: $title"
    return
  fi
  gh issue create --repo "$REPO" --title "$title" --milestone "$milestone" \
    --label "$labels" --body "$body" >/dev/null
  echo "  Created: $title"
}

# ---------------- Core ----------------

update "2. CPU: cores, model and load" "$(cat <<'EOF'
Starter code gives `cpuInfo.getCount()`, `getModel()`, `getField(name)`, `getBusy(core)`, `getIdle(core)`.

- [ ] Report core count and model
- [ ] arm64 fallback: when `getModel()` returns "unknown", use `getField("CPU implementer")` / `getField("CPU part")` or say "not available"
- [ ] Read `getBusy`/`getIdle` for every core, wait ~1s (`Thread.sleep(1000)`), read again
- [ ] Write `calculateLoad(prevBusy, prevIdle, busy, idle)`: load = Δbusy / (Δbusy + Δidle)
- [ ] Load per core and average across all cores
- [ ] Handle -1 (value could not be read)
- [ ] Be able to explain: the counts are ticks since boot, why one reading alone tells you nothing, and what "idle" means (CPU has no work and runs an idle instruction such as x86 `HLT`, W1L3; timer interrupts let the scheduler switch tasks, W2L6)
EOF
)"

update "5. PCI device list" "$(cat <<'EOF'
Starter code gives `pciInfo.getCount()`, `getAddress(i)`, `getBus(i)`, `getDevice(i)`, `getFunction(i)`, `getVendorId(i)`, `getDeviceId(i)`, `getClassCode(i)`.

- [ ] Create `PciDevice` class (bus, device, function, vendor ID, device ID, class code, names)
- [ ] Fill a `List<PciDevice>` from `pciInfo`
- [ ] Compare with `lspci -nn` on alderaan
- [ ] Be able to explain (W4L10): address `0000:00:02.0` = domain:bus:device.function; bus is 8 bits (256 buses), device 5 bits (32 per bus), function 3 bits (8 per device); function 0 is mandatory; the same scheme carries over to PCIe
EOF
)"

update "7. Name lookup (pci.ids and usb.ids)" "$(cat <<'EOF'
Starter code: `idLookup.pci()` / `idLookup.usb()` with `vendorName(id)`, `deviceName(vendor, device)`, `className(classCode)`. These return `null` when an ID isn't found.

- [ ] Read `idLookup` and be able to explain how it parses the files (vendor lines, tab-indented device lines, `C` class section)
- [ ] Add vendor and device names to `PciDevice` and `UsbDevice`
- [ ] When a lookup returns `null`, show the raw hex ID instead
- [ ] Handle a missing ids file without crashing (constructor throws `IOException`)
- [ ] Be able to explain (W4L10): vendor and device IDs are 16-bit numbers the OS uses to pick a driver; the hardware only stores numbers, so a lookup file is needed for names (e.g. Intel = 0x8086)
EOF
)"

# ---------------- Extension ----------------

update "13. Memory explorer: timing loops" "$(cat <<'EOF'
Can be started now. It doesn't depend on the core code. Run timings on **alderaan** or a Linux VM.

Lecture background: W2L4 (cache, locality of reference, cache lines, hit/miss) and W4L11 (why cache matters, direct-mapped / set-associative).

- [ ] Write down what you expect to see and why, using those lecture terms, before measuring
- [ ] Working-set test: read an `int[]` of size ~4 KB up to ~64 MB, many times; time with `System.nanoTime()` and report ns per access
- [ ] Stride test: read the same large array with strides of 1, 2, 4 … 64 ints (4 to 256 bytes)
- [ ] Do warm-up runs first and repeat each measurement a few times (the Java JIT and other programs add noise)
- [ ] Keep the loop simple; the goal is clear results you can explain, not perfect numbers
EOF
)"

update "14. Memory explorer: analysis and comparison" "$(cat <<'EOF'
- [ ] Find where time per access jumps as the array grows; estimate where L1 and L2 end (L3 may be blurry, especially on the shared cloud server)
- [ ] From the stride test, estimate the cache line size (time per access rises until stride ≥ one line, then flattens)
- [ ] Table: measured vs reported in `/sys/devices/system/cpu/cpu0/cache/` (needs card 12)
- [ ] Explain each result with lecture terms: locality of reference, hit, miss, cache line fill, L1/L2/L3 sizes (W2L4, W4L10, W4L11)
- [ ] Explain why results don't match exactly (prefetching, other users on alderaan, JIT, timer resolution)
- [ ] Optional: repeat on a second Linux machine/VM and compare
EOF
)"

# ---------------- Final ----------------

update "17. Documentation and submission" "$(cat <<'EOF'
Final push due **29 Oct**.

- [ ] README: build/run steps, which file each piece of data comes from, how to run the extension
- [ ] Plagiarism declaration signed with all IDs
- [ ] "Declaration of Generative AI and AI-assisted technologies": name each tool and why it was used; collect everyone's saved prompts and responses
- [ ] Check AI use against the module rules (W1 overview: AI to learn is fine and must be declared; don't submit AI-written code)
- [ ] Clean up code and comment the tricky parts (load calculation, ids parsing, timing loop)
- [ ] Fresh clone builds and runs on alderaan
- [ ] Final push by 29 Oct
EOF
)"

update "18. Interview prep" "$(cat <<'EOF'
Whole team: assign everyone. Interview is 30 Oct. Everyone must be able to answer for any part.

- [ ] Each person walks the others through what they built
- [ ] PCI (W4L10): bus/device/function and their bit widths, enumeration, vendor/device IDs and drivers, PCI vs PCIe (parallel bus vs serial lanes)
- [ ] USB (W4L10): host vs device, root hubs, speeds (1.1 / 2.0 / 3.x), device classes, why a VM or the cloud server may have no USB
- [ ] CPU load: ticks since boot, two readings, Δbusy / (Δbusy + Δidle), what idle means
- [ ] Memory: MemTotal vs MemAvailable, why "used" = Total − Available
- [ ] Cache (W2L4, W4L11): locality, hit/miss, cache lines, L1/L2/L3, and what the extension measured and why the numbers differ from `/sys`
- [ ] Design choices: why `PciDevice`/`UsbDevice` classes, why two sorts, how missing files are handled
- [ ] Mock interview: quiz each other
EOF
)"

# ---------------- New card ----------------

create_once "19. Bus details: PCI bridges and USB speeds" "2 - Polish" "polish,pci,usb" "$(cat <<'EOF'
Ties the output to the buses lectures (W3L9, W4L10). Not in the starter code, so read the extra `/sys` files yourselves (`sysFile` may help).

- [ ] Mark PCI bridges (class 0x06xx) and show which bus numbers sit behind them; explain that enumeration starts a new bus at each bridge
- [ ] Show each USB device's speed from `/sys/bus/usb/devices/*/speed` (Mbps) and map it to the USB version (1.5/12 = USB 1.x, 480 = 2.0, 5000+ = 3.x)
- [ ] Show the USB device class (`bDeviceClass`) and label root hubs
- [ ] On alderaan, point out virtual devices (e.g. vendor 0x1af4 Red Hat virtio) and be able to explain why a VM shows them
EOF
)"

echo "Done"
