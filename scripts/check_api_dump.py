#!/usr/bin/env python3
"""Classify changes to the committed public API dumps.

The binary-compatibility-validator plugin writes one ``.api`` file per published
module (``android-core/api/android-core.api`` and
``android-kit-base/api/android-kit-base.api``). ``./gradlew apiCheck`` fails
when the compiled surface differs from the committed dump, and ``apiDump``
regenerates it. That keeps every change to the public surface visible in the
pull request diff, but it does not say whether a change is acceptable.

This script compares the dumps on the current tree with the dumps on a base
revision and classifies every changed class:

* **frozen** -- a class outside ``com.mparticle.internal``, or an internal
  class listed in ``scripts/api-frozen-internals.txt``. These are the
  documented customer API and the contract kit authors compile against.
  Any change fails with exit code 2.
* **reviewable** -- any other class inside ``com.mparticle.internal``. The
  change is reported and the script exits 0; the reviewer decides.

Usage::

    scripts/check_api_dump.py --base origin/main

Exit codes: 0 no frozen change, 2 frozen change, 1 usage or tooling error.
"""

from __future__ import annotations

import argparse
import fnmatch
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DUMPS = (
    Path("android-core/api/android-core.api"),
    Path("android-kit-base/api/android-kit-base.api"),
)
FROZEN_LIST = REPO_ROOT / "scripts" / "api-frozen-internals.txt"
INTERNAL_PREFIX = "com.mparticle.internal."

_HEADER = re.compile(r"^(?P<modifiers>[^{]*?)\bclass (?P<name>\S+)(?P<rest>[^{]*)\{\s*$")


@dataclass
class ClassEntry:
    header: str
    members: set[str] = field(default_factory=set)


def parse_dump(text: str) -> dict[str, ClassEntry]:
    """Parse a BCV dump into ``{jvm_class_name: ClassEntry}``."""
    classes: dict[str, ClassEntry] = {}
    current: ClassEntry | None = None
    for raw in text.splitlines():
        line = raw.rstrip()
        if not line:
            continue
        if line.startswith(("\t", " ")):
            if current is not None:
                current.members.add(line.strip())
            continue
        if line == "}":
            current = None
            continue
        match = _HEADER.match(line)
        if match:
            current = ClassEntry(header=line.strip())
            classes[match.group("name")] = current
    return classes


def jvm_to_dotted(name: str) -> str:
    return name.replace("/", ".")


def outer_class(dotted: str) -> str:
    return dotted.split("$", 1)[0]


def load_frozen_patterns(path: Path) -> list[str]:
    patterns: list[str] = []
    if not path.exists():
        return patterns
    for raw in path.read_text(encoding="utf-8").splitlines():
        entry = raw.split("#", 1)[0].strip()
        if entry:
            patterns.append(entry)
    return patterns


def is_frozen(dotted: str, frozen_patterns: list[str]) -> bool:
    if not dotted.startswith(INTERNAL_PREFIX):
        return True
    outer = outer_class(dotted)
    for pattern in frozen_patterns:
        if fnmatch.fnmatchcase(dotted, pattern) or fnmatch.fnmatchcase(outer, pattern):
            return True
    return False


def git_show(base: str, path: Path) -> str | None:
    try:
        return subprocess.run(
            ["git", "show", f"{base}:{path.as_posix()}"],
            cwd=REPO_ROOT,
            check=True,
            capture_output=True,
            text=True,
        ).stdout
    except subprocess.CalledProcessError:
        return None


def describe_change(old: ClassEntry | None, new: ClassEntry | None) -> list[str]:
    lines: list[str] = []
    if old is None and new is not None:
        lines.append("  + class added")
        return lines
    if new is None and old is not None:
        lines.append("  - class removed")
        return lines
    assert old is not None and new is not None
    if old.header != new.header:
        lines.append(f"  ~ declaration: {old.header}  ->  {new.header}")
    for member in sorted(old.members - new.members):
        lines.append(f"  - {member}")
    for member in sorted(new.members - old.members):
        lines.append(f"  + {member}")
    return lines


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--base", required=True, help="git revision holding the baseline dumps, e.g. origin/main")
    args = parser.parse_args(argv)

    frozen_patterns = load_frozen_patterns(FROZEN_LIST)
    frozen_hits: list[str] = []
    reviewable_hits: list[str] = []

    for dump in DUMPS:
        current_path = REPO_ROOT / dump
        if not current_path.exists():
            print(f"error: {dump} is missing; run ./gradlew apiDump", file=sys.stderr)
            return 1
        old_text = git_show(args.base, dump)
        if old_text is None:
            print(f"note: {dump} does not exist at {args.base}; nothing to compare against (baseline creation)")
            continue
        old = parse_dump(old_text)
        new = parse_dump(current_path.read_text(encoding="utf-8"))

        for jvm_name in sorted(set(old) | set(new)):
            before, after = old.get(jvm_name), new.get(jvm_name)
            if before is not None and after is not None and before.header == after.header and before.members == after.members:
                continue
            dotted = jvm_to_dotted(jvm_name)
            block = [f"{dump}: {dotted}"] + describe_change(before, after)
            (frozen_hits if is_frozen(dotted, frozen_patterns) else reviewable_hits).append("\n".join(block))

    if not frozen_hits and not reviewable_hits:
        print(f"api dumps unchanged against {args.base}")
        return 0

    if reviewable_hits:
        print("Internal implementation surface changed (reviewable; explain in the PR description):")
        print("\n".join(reviewable_hits))
        print()
    if frozen_hits:
        print("FROZEN API CHANGED. These classes are customer or kit-author contracts.")
        print("Fix the change, or label the pull request 'api-change-approved' after API review.")
        print("\n".join(frozen_hits))
        return 2
    return 0


if __name__ == "__main__":
    os.chdir(REPO_ROOT)
    sys.exit(main(sys.argv[1:]))
