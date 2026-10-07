#!/usr/bin/env python3
"""Report Java to Kotlin migration progress for the published SDK modules.

Measures ``android-core/src/main`` and ``android-kit-base/src/main`` only:
test source sets, kits and sample apps are out of scope. Java files listed in
``scripts/kotlin-migration-facades.txt`` stay Java by design (the public API
customers and kit authors compile against, plus ``package-info.java``); they
are counted separately so that **Java left to convert** can honestly reach
zero.

Three numbers are reported:

* **Java left to convert** (LOC): the goal metric; it only goes down.
* **Conversion progress**: ``1 - left / BASELINE_JAVA_LEFT``, where the
  baseline was measured when the migration started. Recompute it if the
  facade list changes.
* **Kotlin share**: Kotlin LOC over all ``src/main`` LOC. Kotlin is terser
  than Java, so this rises more slowly than conversion progress.

Usage::

    scripts/kotlin_migration_progress.py [--root DIR] [--format markdown|env]
                                         [--facades FILE]
    scripts/kotlin_migration_progress.py --headline DELTA_LEFT DELTA_JAVA

The ``env`` format prints ``KEY=value`` lines for CI. ``--headline`` renders the sticky-comment
headline for a pair of already-computed deltas (head minus base) instead of measuring a tree; see
``render_headline`` for why it checks ``delta_java`` before ``delta_left``.
"""

from __future__ import annotations

import argparse
import fnmatch
import sys
from dataclasses import dataclass
from pathlib import Path

MODULES = ("android-core/src/main", "android-kit-base/src/main")
FACADE_LIST = Path("scripts/kotlin-migration-facades.txt")

# Java LOC outside the facade list at main 4ee3ba4c (version 6.1.2).
BASELINE_JAVA_LEFT = 16_550


@dataclass
class ModuleStats:
    module: str
    kotlin_loc: int = 0
    java_loc: int = 0
    facade_loc: int = 0
    java_files: int = 0
    kotlin_files: int = 0

    @property
    def java_left(self) -> int:
        return self.java_loc - self.facade_loc

    @property
    def kotlin_share(self) -> float:
        total = self.kotlin_loc + self.java_loc
        return 100.0 if total == 0 else 100.0 * self.kotlin_loc / total


def count_lines(path: Path) -> int:
    """Newline count, matching ``wc -l`` and the figures quoted in the tracker."""
    return path.read_bytes().count(b"\n")


def load_facades(path: Path) -> list[str]:
    if not path.exists():
        return []
    entries: list[str] = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        entry = raw.split("#", 1)[0].strip()
        if entry:
            entries.append(entry)
    return entries


def is_facade(relative_path: str, facades: list[str]) -> bool:
    return any(fnmatch.fnmatchcase(relative_path, pattern) for pattern in facades)


def measure(root: Path, facades: list[str]) -> list[ModuleStats]:
    """Measure every module under ``root``; a missing module directory is an error, never a zero."""
    results: list[ModuleStats] = []
    for module in MODULES:
        stats = ModuleStats(module=module.split("/", 1)[0])
        module_dir = root / module
        if not module_dir.is_dir():
            raise FileNotFoundError(f"{module_dir} is not a directory; is --root the repository root?")
        for path in sorted(module_dir.rglob("*")):
            if not path.is_file():
                continue
            relative = path.relative_to(root).as_posix()
            if path.suffix == ".java":
                lines = count_lines(path)
                stats.java_loc += lines
                stats.java_files += 1
                if is_facade(relative, facades):
                    stats.facade_loc += lines
            elif path.suffix == ".kt":
                stats.kotlin_loc += count_lines(path)
                stats.kotlin_files += 1
        results.append(stats)
    return results


def totals(results: list[ModuleStats]) -> ModuleStats:
    total = ModuleStats(module="total")
    for stats in results:
        total.kotlin_loc += stats.kotlin_loc
        total.java_loc += stats.java_loc
        total.facade_loc += stats.facade_loc
        total.java_files += stats.java_files
        total.kotlin_files += stats.kotlin_files
    return total


def conversion_progress(java_left: int) -> float:
    if BASELINE_JAVA_LEFT <= 0:
        return 100.0
    return max(0.0, min(100.0, 100.0 * (1 - java_left / BASELINE_JAVA_LEFT)))


def render_markdown(results: list[ModuleStats]) -> str:
    total = totals(results)
    lines = [
        "| Module | Kotlin LOC | Java LOC | Java staying (facade) | Java left to convert | Kotlin share |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for stats in results:
        lines.append(
            f"| `{stats.module}` | {stats.kotlin_loc:,} | {stats.java_loc:,} | {stats.facade_loc:,} "
            f"| **{stats.java_left:,}** | {stats.kotlin_share:.1f}% |"
        )
    lines.append(
        f"| **Total** | **{total.kotlin_loc:,}** | **{total.java_loc:,}** | **{total.facade_loc:,}** "
        f"| **{total.java_left:,}** | **{total.kotlin_share:.1f}%** |"
    )
    lines.append("")
    lines.append(
        f"**Conversion progress: {conversion_progress(total.java_left):.1f}%** "
        f"({total.java_left:,} of {BASELINE_JAVA_LEFT:,} baseline Java LOC left to convert). "
        f"{total.java_files} Java files and {total.kotlin_files} Kotlin files in scope."
    )
    return "\n".join(lines)


def render_headline(delta_left: int, delta_java: int) -> str:
    """Render the sticky-comment headline for a before/after measurement.

    ``delta_java`` is checked first because that is what the ratchet step fails the job on; a
    drop in ``delta_left`` is reported alongside it rather than instead of it, so the headline
    never looks green while the job is about to go red. A rise in ``delta_left`` is called out
    even when ``delta_java`` does not grow (code moved out of a facade into a convertible file),
    since the ratchet itself would otherwise stay silent on that regression.
    """
    if delta_java > 0:
        headline = f":warning: This pull request **adds {delta_java} lines** of Java to the published modules."
        if delta_left < 0:
            headline += (
                f" Java left to convert still drops by {-delta_left} lines, but the ratchet below "
                "checks total Java, not just what is left to convert."
            )
        return headline
    if delta_left < 0:
        return f"This pull request converts **{-delta_left} lines** of Java to Kotlin."
    if delta_left > 0:
        return (
            f":warning: The Java left to convert increased by {delta_left} lines, even though total "
            "Java did not grow -- check whether code moved out of a facade file into a convertible one."
        )
    return "No change to the Java left to convert."


def render_env(results: list[ModuleStats]) -> str:
    total = totals(results)
    return "\n".join(
        [
            f"KOTLIN_LOC={total.kotlin_loc}",
            f"JAVA_LOC={total.java_loc}",
            f"JAVA_FACADE_LOC={total.facade_loc}",
            f"JAVA_LEFT_LOC={total.java_left}",
            f"JAVA_FILES={total.java_files}",
            f"KOTLIN_FILES={total.kotlin_files}",
            f"KOTLIN_SHARE_PCT={total.kotlin_share:.1f}",
            f"CONVERSION_PCT={conversion_progress(total.java_left):.1f}",
        ]
    )


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--root", default=".", help="repository root to measure (default: current directory)")
    parser.add_argument("--format", choices=("markdown", "env"), default="markdown")
    parser.add_argument("--facades", help="facade list to apply (default: scripts/kotlin-migration-facades.txt under --root)")
    parser.add_argument(
        "--headline",
        nargs=2,
        type=int,
        metavar=("DELTA_LEFT", "DELTA_JAVA"),
        help="print the sticky-comment headline for these deltas and exit; does not measure a tree",
    )
    args = parser.parse_args(argv)

    if args.headline is not None:
        print(render_headline(*args.headline))
        return 0

    root = Path(args.root).resolve()
    facade_path = Path(args.facades).resolve() if args.facades else root / FACADE_LIST
    try:
        results = measure(root, load_facades(facade_path))
    except FileNotFoundError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    print(render_env(results) if args.format == "env" else render_markdown(results))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
