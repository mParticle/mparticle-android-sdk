#!/usr/bin/env python3
"""Render an SDK Size Impact Report comment from two measure_size.sh JSON payloads.

Usage:
  render_size_report.py [options] <base-json> <head-json>

Options:
  --stack NAME:LABEL[:BASE]  A column group to report. NAME matches the `<name>_*_bytes` keys
                             in the payload; LABEL is the heading. With BASE, an extra column
                             shows this stack's cost over stack BASE rather than over the
                             empty baseline. Repeatable; order is preserved.
  --marker TEXT              HTML comment used to find and replace the sticky PR comment.
  --baseline-note TEXT       Sentence describing what the baseline app is. Per-workflow rather
                             than fixed, because the fixtures do not share a baseline.
  --footnote TEXT            Trailing line naming what was measured, so a comment left behind
                             by a later push is visibly stale rather than passing as current.

Either payload may be empty or unparseable -- that renders as "not measured" rather than as a
zero, so a broken measurement can never be mistaken for a size-neutral change.
"""

import json
import sys

# Deltas below this are indistinguishable from build noise, so they only pick an emoji.
NEUTRAL_BYTES = 10 * 1024
# The JSON keys are historical; the labels are what the report claims. "APK size" is the
# archive's own byte length -- not the installed footprint, which includes ART-compiled
# artifacts and is device-dependent.
METRICS = (
    ("install", "APK size"),
    ("download", "Download size"),
    ("dex", "Dex bytes"),
)


def load(path):
    try:
        with open(path, encoding="utf-8") as handle:
            text = handle.read().strip()
        return json.loads(text) if text else None
    except (OSError, ValueError):
        return None


def human(size):
    if size is None:
        return "not measured"
    sign = "-" if size < 0 else ""
    size = abs(size)
    for unit, scale in (("MB", 1024 * 1024), ("KB", 1024)):
        if size >= scale:
            return f"{sign}{size / scale:.2f} {unit}"
    return f"{sign}{size} bytes"


def delta(size):
    if size is None:
        return "not measured"
    return ("+" if size > 0 else "") + human(size)


def cost(data, stack, metric, over="baseline"):
    """Cost of `stack` over `over`, both built by the same toolchain."""
    if data is None:
        return None
    try:
        return data[f"{stack}_{metric}_bytes"] - data[f"{over}_{metric}_bytes"]
    except KeyError:
        return None


def table(base, head, stack, marginal_base, marginal_label):
    header = "| Metric | Target branch | This PR | Change |"
    divider = "|---|---|---|---|"
    if marginal_base:
        header += f" On top of {marginal_label} |"
        divider += "---|"
    rows = [header, divider]
    for metric, label in METRICS:
        was = cost(base, stack, metric)
        now = cost(head, stack, metric)
        change = None if was is None or now is None else now - was
        cells = [label, human(was), human(now), delta(change)]
        if marginal_base:
            cells.append(delta(cost(head, stack, metric, over=marginal_base)))
        rows.append("| " + " | ".join(cells) + " |")
    return "\n".join(rows)


def status(base, head, stacks):
    changes = []
    for stack, _, _ in stacks:
        was = cost(base, stack, "install")
        now = cost(head, stack, "install")
        if was is not None and now is not None:
            changes.append(now - was)
    if not changes:
        return "ℹ️ Size could not be measured on one or both branches."
    # An increase in any stack wins over a decrease in another: taking the
    # largest-magnitude delta would let a shrink in one hide a growth in the other.
    if max(changes) > NEUTRAL_BYTES:
        return "⚠️ This change increases SDK size impact."
    if min(changes) < -NEUTRAL_BYTES:
        return "✅ This change decreases SDK size impact."
    return "➡️ SDK size impact change is minimal."


def parse_args(argv):
    stacks, marker, paths, footnote, baseline_note = (
        [],
        "<!-- sdk-size-report -->",
        [],
        "",
        "",
    )
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg == "--footnote":
            index += 1
            footnote = argv[index]
        elif arg == "--baseline-note":
            index += 1
            baseline_note = argv[index]
        elif arg == "--stack":
            index += 1
            parts = argv[index].split(":")
            if len(parts) == 2:
                parts.append("")
            stacks.append((parts[0], parts[1], parts[2]))
        elif arg == "--marker":
            index += 1
            marker = argv[index]
        else:
            paths.append(arg)
        index += 1
    return stacks, marker, paths, footnote, baseline_note


def main(argv):
    stacks, marker, paths, footnote, baseline_note = parse_args(argv)
    if len(paths) != 2 or not stacks:
        print(__doc__, file=sys.stderr)
        return 2
    base, head = load(paths[0]), load(paths[1])

    parts = [
        marker,
        "## 📦 SDK Size Impact Report",
        "",
        "What the SDK adds to a minified release APK.",
    ]
    if baseline_note:
        parts += ["", baseline_note]
    labels = {name: label for name, label, _ in stacks}
    for stack, label, marginal_base in stacks:
        marginal_label = labels.get(marginal_base, marginal_base)
        parts += [
            "",
            f"### {label}",
            "",
            table(base, head, stack, marginal_base, marginal_label),
        ]
    parts += ["", status(base, head, stacks), ""]
    parts += ["<details><summary>Raw measurements</summary>", ""]
    for label, data in (("Target branch", base), ("This PR", head)):
        body = json.dumps(data, sort_keys=True) if data else "not measured"
        parts += [f"**{label}:**", "", "```json", body, "```", ""]
    parts += ["</details>"]
    if footnote:
        parts += ["", f"<sub>{footnote}</sub>"]
    print("\n".join(parts))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
