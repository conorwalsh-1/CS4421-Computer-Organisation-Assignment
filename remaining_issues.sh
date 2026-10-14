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
