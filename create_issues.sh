#!/usr/bin/env bash
# Creates labels, milestones and all 18 issues for the CS4421 project.
#
# Before running:
#   1. Install the GitHub CLI: https://cli.github.com
#   2. Log in:                 gh auth login
#   3. (Optional) To add issues to a Project board automatically:
#        gh auth refresh -s project
#      and set PROJECT below to your project's exact name.
#
# Run from inside your cloned repo:   bash create_issues.sh
# Or point at a repo explicitly:      REPO=owner/repo bash create_issues.sh

set -e

REPO="${REPO:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"
PROJECT="${PROJECT:-}"   # e.g. PROJECT="CS4421 Board"

echo "Creating issues in $REPO"

# ---------- Labels ----------
make_label() { gh label create "$1" --color "$2" --repo "$REPO" --force >/dev/null; }

make_label setup       "bfdadc"
make_label core        "0e8a16"
make_label cpu         "1d76db"
make_label memory      "1d76db"
make_label usb         "1d76db"
make_label pci         "1d76db"
make_label lookup      "1d76db"
make_label analysis    "5319e7"
make_label output      "fbca04"
make_label testing     "d93f0b"
make_label polish      "c2e0c6"
make_label extension   "b60205"
make_label export      "f9d0c4"
make_label docs        "0075ca"
make_label interview   "e99695"
make_label whole-team  "000000"
echo "Labels done"

# ---------- Milestones ----------
make_milestone() {
  gh api "repos/$REPO/milestones" -f title="$1" -f due_on="$2" >/dev/null 2>&1 \
    || echo "  Milestone '$1' already exists, skipping"
}

make_milestone "0 - Setup"     "2026-10-15T23:00:00Z"
make_milestone "1 - Core"      "2026-10-22T23:00:00Z"
make_milestone "2 - Polish"    "2026-10-25T23:00:00Z"
make_milestone "3 - Extension" "2026-10-26T23:00:00Z"
make_milestone "4 - Final"     "2026-10-28T23:00:00Z"
echo "Milestones done"

# ---------- Issues ----------
issue() {
  local title="$1" milestone="$2" labels="$3" body="$4"
  local args=(--repo "$REPO" --title "$title" --milestone "$milestone" --label "$labels" --body "$body")
  [ -n "$PROJECT" ] && args+=(--project "$PROJECT")
  gh issue create "${args[@]}" >/dev/null
  echo "  Created: $title"
}

# ===== Milestone 0 - Setup =====
issue "1. Project setup" "0 - Setup" "setup,whole-team" "$(cat <<'EOF'
Whole team: assign everyone.

- [ ] Create GitHub repo and add all four members
- [ ] Add provided classes (`cpuInfo`, `memInfo`, `pciInfo`, `usbInfo`) and `pci.ids` / `usb.ids`
- [ ] Agree on package structure and a `Main` class
- [ ] Everyone clones, builds and runs it
- [ ] Everyone runs `cat` on `/proc/cpuinfo`, `/proc/stat`, `/proc/meminfo`, `/sys/bus/pci/devices/` to see the raw data
- [ ] Read the provided classes and decide whether to use, extend or replace them
EOF
)"

# ===== Milestone 1 - Core =====
issue "2. CPU: cores, model and load" "1 - Core" "core,cpu" "$(cat <<'EOF'
- [ ] Count cores from `processor` lines in `/proc/cpuinfo`
- [ ] Read `model name`; arm64 fallback (implementer/part, or "not available")
- [ ] Read busy/idle from `cpuN` lines in `/proc/stat` twice, ~1s apart
- [ ] Load per core = Δbusy / (Δbusy + Δidle)
- [ ] Average load across all cores
EOF
)"

issue "3. Memory: total, used, percentage" "1 - Core" "core,memory" "$(cat <<'EOF'
- [ ] Read `MemTotal` and `MemAvailable` from `/proc/meminfo`
- [ ] Used = Total − Available; calculate % in use
- [ ] Convert kB to MiB/GiB
EOF
)"

issue "4. USB device list" "1 - Core" "core,usb" "$(cat <<'EOF'
- [ ] `UsbDevice` class (vendor ID, product ID)
- [ ] Read `idVendor` / `idProduct` from `/sys/bus/usb/devices/`, skipping folders without them
- [ ] Store in a `List`
- [ ] Print "No USB devices found" when empty; no crash if the folder is missing
EOF
)"

issue "5. PCI device list" "1 - Core" "core,pci" "$(cat <<'EOF'
- [ ] `PciDevice` class (bus, device, function, vendor, device ID, class)
- [ ] Parse bus/device/function from folder name (`0000:00:02.0`)
- [ ] Read `vendor`, `device`, `class` hex files
- [ ] Store in a `List`
EOF
)"

issue "6. PCI analysis: sorting and summary" "1 - Core" "core,analysis,pci" "$(cat <<'EOF'
- [ ] Sort alphabetically by vendor name (`Comparator`)
- [ ] Sort numerically by bus → device → function
- [ ] Count devices per vendor (`Map<String, Integer>`)
- [ ] Count devices per class
EOF
)"

issue "7. Name lookup (pci.ids and usb.ids)" "1 - Core" "core,lookup" "$(cat <<'EOF'
- [ ] Parse vendor lines and tab-indented device lines; skip `#` and blank lines
- [ ] Store in a `Map`, one parser reused for both files
- [ ] Show names next to IDs in PCI and USB output
- [ ] Show the raw ID when not found; no crash if a file is missing
EOF
)"

issue "8. Text output and integration" "1 - Core" "core,output" "$(cat <<'EOF'
- [ ] Section headings: Processor, Memory, PCI, USB, Analysis
- [ ] Aligned columns with `printf`
- [ ] Plain-English summary sentence (e.g. "4 cores and 16 GiB RAM, 8 GiB in use")
- [ ] All sections called from `Main`; runs end to end
EOF
)"

issue "9. Robustness and testing" "1 - Core" "core,testing,whole-team" "$(cat <<'EOF'
Whole team: assign everyone.

- [ ] Handle missing files: cache folder, USB, `model name`, ids files
- [ ] Runs headless on alderaan
- [ ] Output checked against `lscpu`, `free -h`, `lspci -nn`
EOF
)"

# ===== Milestone 2 - Polish =====
issue "10. CPU and memory bar graphs" "2 - Polish" "polish,output" "$(cat <<'EOF'
- [ ] Per-core CPU load bars, e.g. `cpu0 [██████░░░░] 61%`
- [ ] Average CPU load bar
- [ ] Memory used/available bar
EOF
)"

issue "11. Summary chart and colour output" "2 - Polish" "polish,output" "$(cat <<'EOF'
- [ ] Bar chart of devices per vendor/class
- [ ] ANSI colours (green < 50%, yellow < 80%, red above)
- [ ] `--no-colour` flag
EOF
)"

issue "12. Extra hardware detail" "2 - Polish" "polish,pci,cpu" "$(cat <<'EOF'
- [ ] Decode PCI class codes to names (e.g. "Display controller")
- [ ] Report cache info from `/sys/devices/system/cpu/cpu0/cache/indexN` (level, type, size, line size)
- [ ] Handle a missing cache folder
EOF
)"

# ===== Milestone 3 - Extension =====
issue "13. Memory explorer: timing loops" "3 - Extension" "extension" "$(cat <<'EOF'
Can be started now. It doesn't depend on the core code.

- [ ] Review lecture notes on cache levels and cache lines; write down the expected results
- [ ] Timing loop over working-set sizes ~4 KB to ~64 MB using `System.nanoTime()`
- [ ] Timing loop over strides (1, 2, 4 … 128)
- [ ] Warm-up runs to reduce JIT noise
EOF
)"

issue "14. Memory explorer: analysis and comparison" "3 - Extension" "extension,analysis" "$(cat <<'EOF'
- [ ] Estimate L1/L2/L3 sizes from the jumps in time per access
- [ ] Estimate the cache line size from the stride results
- [ ] Table comparing measured and `/sys` reported values, with differences explained
- [ ] Run on a laptop and alderaan and compare
EOF
)"

issue "15. Extension: CSV export" "3 - Extension" "extension,export" "$(cat <<'EOF'
- [ ] Write size/stride vs time results to a `.csv` file
- [ ] Clear column headers and units
EOF
)"

issue "16. Extension: SVG/HTML graph" "3 - Extension" "extension,export" "$(cat <<'EOF'
- [ ] Line graph: working-set size (log scale) vs time per access
- [ ] Mark the reported cache sizes on the graph
- [ ] Opens in a browser on your own laptop; text version still works without it
EOF
)"

# ===== Milestone 4 - Final =====
issue "17. Documentation and submission" "4 - Final" "docs" "$(cat <<'EOF'
Final push due **29 Oct**.

- [ ] README: build/run steps, which file each piece of data comes from, how to run the extension
- [ ] Plagiarism declaration signed with all IDs
- [ ] "Declaration of Generative AI and AI-assisted technologies"; collect everyone's saved prompts and responses
- [ ] Clean up code and comment the tricky parts (load calculation, ids parsing, timing loop)
- [ ] Fresh clone builds and runs on alderaan and a laptop
- [ ] Final push by 29 Oct
EOF
)"

issue "18. Interview prep" "4 - Final" "interview,whole-team" "$(cat <<'EOF'
Whole team: assign everyone. Interview is 30 Oct.

- [ ] Each person walks the others through what they built
- [ ] Everyone can explain: bus/device/function, vendor IDs and why a lookup file is needed, the CPU load calculation, and the extension results
- [ ] Mock interview: quiz each other
EOF
)"

echo "All 18 issues created in $REPO"
