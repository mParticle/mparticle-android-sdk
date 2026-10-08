#!/usr/bin/env python3
"""Render an SDK Size Impact Report comment from base/head JSON payloads of one or more fixtures.

Usage:
  render_size_report.py [--marker TEXT] [--footnote TEXT]
      --fixture NAME BASE_JSON HEAD_JSON [--baseline-note TEXT] --stack NAME:LABEL[:BASE] ...
      [--fixture ...]

Options:
  --fixture NAME BASE HEAD   Starts a fixture: one reference app measured on the target branch
                             (BASE) and the pull request (HEAD). The --baseline-note and --stack
                             options that follow belong to it. Repeatable; order is preserved.
  --baseline-note TEXT       Sentence describing what this fixture's baseline app is. Per
                             fixture, because the fixtures do not share a baseline.
  --stack NAME:LABEL[:BASE]  A table to report. NAME matches the `<name>_*_bytes` keys in the
                             fixture's payload; LABEL is the heading. With BASE, an extra column
                             shows this stack's cost over stack BASE rather than over the
                             fixture's baseline. Repeatable; order is preserved.
  --marker TEXT              HTML comment used to find and replace the sticky PR comment.
  --footnote TEXT            Trailing line naming what was measured, so a comment left behind
                             by a later push is visibly stale rather than passing as current.

Any payload may be empty or unparseable -- that renders as "not measured" rather than as a
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


def status(fixtures):
    changes, unmeasured = [], 0
    for fixture in fixtures:
        for stack, _, _ in fixture["stacks"]:
            was = cost(fixture["base"], stack, "install")
            now = cost(fixture["head"], stack, "install")
            if was is not None and now is not None:
                changes.append(now - was)
            else:
                unmeasured += 1
    if not changes:
        return "ℹ️ Size could not be measured on one or both branches."
    # An increase in any stack wins over a decrease in another: taking the
    # largest-magnitude delta would let a shrink in one hide a growth in the other.
    if max(changes) > NEUTRAL_BYTES:
        verdict = "⚠️ This change increases SDK size impact."
    elif min(changes) < -NEUTRAL_BYTES:
        verdict = "✅ This change decreases SDK size impact."
    else:
        verdict = "➡️ SDK size impact change is minimal."
    # Otherwise a fixture that failed to build would pass as size-neutral.
    if unmeasured:
        verdict += " Some stacks could not be measured, so this covers only the rest."
    return verdict


def parse_args(argv):
    fixtures, marker, footnote = [], "<!-- sdk-size-report -->", ""
    index = 0
    while index < len(argv):
        arg = argv[index]
        if arg == "--fixture":
            name, base, head = argv[index + 1 : index + 4]
            index += 3
            fixtures.append(
                {"name": name, "base": load(base), "head": load(head), "note": "", "stacks": []}
            )
        elif arg in ("--baseline-note", "--stack") and not fixtures:
            raise ValueError(f"{arg} must follow a --fixture")
        elif arg == "--baseline-note":
            index += 1
            fixtures[-1]["note"] = argv[index]
        elif arg == "--stack":
            index += 1
            parts = argv[index].split(":")
            if len(parts) == 2:
                parts.append("")
            fixtures[-1]["stacks"].append((parts[0], parts[1], parts[2]))
        elif arg == "--marker":
            index += 1
            marker = argv[index]
        elif arg == "--footnote":
            index += 1
            footnote = argv[index]
        else:
            raise ValueError(f"unexpected argument: {arg}")
        index += 1
    return fixtures, marker, footnote


def main(argv):
    try:
        fixtures, marker, footnote = parse_args(argv)
    except ValueError as error:
        print(f"{error}\n{__doc__}", file=sys.stderr)
        return 2
    if not fixtures or not all(fixture["stacks"] for fixture in fixtures):
        print(__doc__, file=sys.stderr)
        return 2

    parts = [
        marker,
        "## 📦 SDK Size Impact Report",
        "",
        "What the SDK adds to a minified release APK.",
    ]
    for fixture in fixtures:
        labels = {name: label for name, label, _ in fixture["stacks"]}
        for position, (stack, label, marginal_base) in enumerate(fixture["stacks"]):
            parts += ["", f"### {label}", ""]
            # Once per fixture: later stacks share the baseline, and their "On top of" column
            # already names what they are measured against.
            if position == 0 and fixture["note"]:
                parts += [fixture["note"], ""]
            parts.append(
                table(
                    fixture["base"],
                    fixture["head"],
                    stack,
                    marginal_base,
                    labels.get(marginal_base, marginal_base),
                )
            )
    parts += ["", status(fixtures), ""]
    parts += ["<details><summary>Raw measurements</summary>", ""]
    for fixture in fixtures:
        for label, data in (("Target branch", fixture["base"]), ("This PR", fixture["head"])):
            body = json.dumps(data, sort_keys=True) if data else "not measured"
            parts += [f"**{fixture['name']}, {label}:**", "", "```json", body, "```", ""]
    parts += ["</details>"]
    if footnote:
        parts += ["", f"<sub>{footnote}</sub>"]
    print("\n".join(parts))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
