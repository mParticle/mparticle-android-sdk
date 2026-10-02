#!/usr/bin/env python3
"""Compare the release artifacts against the last published release with japicmp.

``./gradlew apiCheck`` guards the *compiled* surface of ``android-core`` and
``android-kit-base`` before R8 runs. What consumers actually link against is the
release AAR: for ``android-core`` that is an R8-processed jar in which only the
classes ``android-core/proguard.pro`` keeps survive under their own names. This
script builds both release AARs, extracts ``classes.jar`` from each, downloads
the same artifact at the most recent version published to Maven Central, and
runs japicmp over the two.

R8 renames whatever no keep rule names, and the names it picks are not stable
between builds, so those symbols are excluded before the verdict is computed:

* a class whose simple name, or any nested-class segment of it, is one or two
  lowercase letters optionally followed by a digit (``a``, ``b1``,
  ``MParticle$a``, ``CoreCallbacks$KitListener$b``), or an anonymous class
  (``Outer$1``), is dropped from both jars before the comparison;
* inside a surviving class that the release build's R8 mapping file shows
  to have renamed members (a keep rule names the class only partially), a
  method or field with such a name is ignored when the report is evaluated;
  members of fully kept classes are always compared, whatever their name;
* an interface or superclass change that only involves such a class is
  ignored as well;
* ``BuildConfig`` is dropped from both jars; its fields carry per-build values.

Any remaining binary- or source-incompatible change fails the run. Additions
are reported but do not fail it.

japicmp runs with ``--ignore-missing-classes`` and no classpath for Android or
third-party types, so changes that only manifest through unresolved external
supertypes are not analysed. The compile-time dump covers those.

Usage::

    scripts/api_compat_report.py [--module android-core] [--previous 6.1.2]
                                 [--skip-build] [--output-dir build/api-compat]

Exit codes: 0 compatible, 2 incompatible change found, 1 tooling error.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
GROUP_PATH = "com/mparticle"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2"
MODULES = ("android-core", "android-kit-base")

JAPICMP_VERSION = "0.26.2"
JAPICMP_URL = (
    f"{MAVEN_CENTRAL}/com/github/siom79/japicmp/japicmp/{JAPICMP_VERSION}/"
    f"japicmp-{JAPICMP_VERSION}-jar-with-dependencies.jar"
)
JAPICMP_SHA256 = "d1eb211b7132eae1f9ee834270a8d19a9652d1983d3fe75ec564666ebef5c18d"

_OBFUSCATED_SEGMENT = re.compile(r"^(?:[a-z]{1,2}\d?|\d+)$")
_OBFUSCATED_MEMBER = re.compile(r"^[a-z]{1,2}\d?$")


def log(message: str) -> None:
    print(message, file=sys.stderr)


def run(cmd: list[str], cwd: Path) -> None:
    log("$ " + " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True)


# --- naming -----------------------------------------------------------------


def is_obfuscated_class(binary_name: str) -> bool:
    """``binary_name`` is a class name with ``/`` or ``.`` separators, with or without ``.class``."""
    name = binary_name[: -len(".class")] if binary_name.endswith(".class") else binary_name
    simple = name.replace("/", ".").rsplit(".", 1)[-1]
    return any(_OBFUSCATED_SEGMENT.match(segment) for segment in simple.split("$"))


def is_obfuscated_member(name: str | None) -> bool:
    return bool(name) and _OBFUSCATED_MEMBER.match(name) is not None


# --- artifacts --------------------------------------------------------------


def latest_published_version(module: str) -> str:
    url = f"{MAVEN_CENTRAL}/{GROUP_PATH}/{module}/maven-metadata.xml"
    with urllib.request.urlopen(url, timeout=60) as response:
        root = ET.fromstring(response.read())
    release = root.findtext("./versioning/release")
    if not release:
        raise RuntimeError(f"no <release> in {url}")
    return release


def fetch_text(url: str) -> str | None:
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            return response.read().decode("utf-8").strip()
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def download_verified(url: str, dest: Path, expected_sha256: str | None = None) -> Path:
    """Download ``url`` to ``dest`` and verify it against a pinned or published checksum."""
    if not dest.exists():
        log(f"downloading {url}")
        with urllib.request.urlopen(url, timeout=120) as response, dest.open("wb") as out:
            shutil.copyfileobj(response, out)
    if expected_sha256:
        actual = hashlib.sha256(dest.read_bytes()).hexdigest()
        if actual != expected_sha256:
            dest.unlink(missing_ok=True)
            raise RuntimeError(f"sha256 mismatch for {url}: expected {expected_sha256}, got {actual}")
        return dest
    for suffix, algorithm in ((".sha256", "sha256"), (".sha1", "sha1")):
        published = fetch_text(url + suffix)
        if published is None:
            continue
        expected = published.split()[0].lower()
        actual = hashlib.new(algorithm, dest.read_bytes()).hexdigest()
        if actual != expected:
            dest.unlink(missing_ok=True)
            raise RuntimeError(f"{algorithm} mismatch for {url}: expected {expected}, got {actual}")
        return dest
    raise RuntimeError(f"no published checksum found for {url}")


def extract_classes_jar(aar: Path, dest: Path) -> Path:
    with zipfile.ZipFile(aar) as archive:
        with archive.open("classes.jar") as source, dest.open("wb") as out:
            shutil.copyfileobj(source, out)
    return dest


def filter_jar(source: Path, dest: Path) -> tuple[int, int]:
    """Copy ``source`` to ``dest`` without R8-renamed classes and BuildConfig.

    Returns ``(kept, dropped)`` class counts.
    """
    kept = dropped = 0
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as out:
        for info in src.infolist():
            name = info.filename
            if name.endswith(".class"):
                if is_obfuscated_class(name) or name.rsplit("/", 1)[-1] == "BuildConfig.class":
                    dropped += 1
                    continue
                kept += 1
            out.writestr(info, src.read(name))
    return kept, dropped


def build_release_aars(modules: tuple[str, ...]) -> None:
    version = (REPO_ROOT / "VERSION").read_text(encoding="utf-8").strip()
    tasks = [f":{module}:assembleRelease" for module in modules]
    run(["./gradlew", *tasks, f"-PVERSION={version}", "--console=plain", "-q"], cwd=REPO_ROOT)


def local_release_aar(module: str) -> Path:
    aar = REPO_ROOT / module / "build" / "outputs" / "aar" / f"{module}-release.aar"
    if not aar.exists():
        raise RuntimeError(f"no release AAR at {aar}; run without --skip-build")
    return aar


def japicmp_jar(cache_dir: Path) -> Path:
    return download_verified(JAPICMP_URL, cache_dir / f"japicmp-{JAPICMP_VERSION}.jar", JAPICMP_SHA256)


def release_mapping(module: str) -> Path | None:
    mapping = REPO_ROOT / module / "build" / "outputs" / "mapping" / "release" / "mapping.txt"
    return mapping if mapping.exists() else None


_MAPPING_CLASS = re.compile(r"^(\S+) -> (\S+):$")
_MAPPING_MEMBER = re.compile(r"^\s+(?:\d+:\d+:)?\S+ (?P<name>[^\s(]+)(?:\([^)]*\))?(?::\d+:\d+)? -> (?P<renamed>\S+)$")


def load_renamed_members(mapping: Path | None) -> dict[str, set[str]]:
    """Read an R8 mapping file into ``{class name in the jar: {renamed member names}}``.

    Classes are keyed by the name they carry in the shipped jar (for a kept
    class, its original name), which is also how japicmp reports them. Only
    classes with at least one member renamed to a different name are listed;
    a class that a keep rule names in full never appears, so its members are
    always compared by their real names. Inlined frames, which R8 records as
    qualified method names mapped onto the caller, are skipped.
    """
    renamed: dict[str, set[str]] = {}
    if mapping is None:
        return renamed
    current: str | None = None
    for line in mapping.read_text(encoding="utf-8").splitlines():
        if not line or line.lstrip().startswith("#"):
            continue
        class_match = _MAPPING_CLASS.match(line)
        if class_match:
            current = class_match.group(2)
            continue
        member_match = _MAPPING_MEMBER.match(line)
        if current is None or not member_match:
            continue
        name, new_name = member_match.group("name"), member_match.group("renamed")
        if "." in name or name.startswith("<") or new_name.startswith("<") or name == new_name:
            continue
        renamed.setdefault(current, set()).add(new_name)
    return renamed


# --- report evaluation ------------------------------------------------------


@dataclass
class Finding:
    class_name: str
    member: str
    changes: list[str] = field(default_factory=list)

    def __str__(self) -> str:
        where = self.class_name if not self.member else f"{self.class_name}#{self.member}"
        return f"{where}: {', '.join(self.changes)}"


def _incompatible(element: ET.Element) -> bool:
    return element.get("binaryCompatible") == "false" or element.get("sourceCompatible") == "false"


def _change_types(element: ET.Element) -> list[str]:
    changes = element.find("compatibilityChanges")
    if changes is None:
        return []
    return [c.get("type") or "?" for c in changes if _incompatible(c)]


def evaluate_report(xml_path: Path, renamed_members: dict[str, set[str]] | None = None) -> tuple[list[Finding], int]:
    """Return ``(incompatible findings, number of classes with additions only)``.

    ``renamed_members`` comes from :func:`load_renamed_members`. Inside a class
    listed there, members whose name is R8-shaped are ignored: on the new side
    because the mapping proves the name is synthetic, on the old side because
    the class is only partially kept and R8 renamed its members there too.
    """
    renamed_members = renamed_members or {}
    root = ET.parse(xml_path).getroot()
    findings: list[Finding] = []
    additions = 0
    for clazz in root.iter("class"):
        class_name = clazz.get("fullyQualifiedName") or "?"
        if is_obfuscated_class(class_name):
            continue

        class_changes = [
            t for t in _change_types(clazz) if not t.startswith(("INTERFACE_", "SUPERCLASS_"))
        ]
        for interface in clazz.iterfind("./interfaces/interface"):
            if _incompatible(interface) and not is_obfuscated_class(interface.get("fullyQualifiedName") or ""):
                class_changes.extend(_change_types(interface) or [f"INTERFACE_{interface.get('changeStatus')}"])
        superclass = clazz.find("superclass")
        if superclass is not None and _incompatible(superclass):
            names = (superclass.get("superclassOld") or "", superclass.get("superclassNew") or "")
            if not all(is_obfuscated_class(n) for n in names if n):
                class_changes.extend(_change_types(superclass) or ["SUPERCLASS_MODIFIED"])
        if class_changes:
            findings.append(Finding(class_name, "", sorted(set(class_changes))))

        partially_kept = class_name in renamed_members
        for section, member_tag in (("methods", "method"), ("constructors", "constructor"), ("fields", "field")):
            for member in clazz.iterfind(f"./{section}/{member_tag}"):
                name = member.get("name") or ""
                if section != "constructors" and partially_kept and (
                    name in renamed_members[class_name] or is_obfuscated_member(name)
                ):
                    continue
                if _incompatible(member):
                    findings.append(Finding(class_name, name, sorted(set(_change_types(member))) or ["INCOMPATIBLE"]))

        if not class_changes and clazz.get("changeStatus") in ("NEW", "MODIFIED") and not _incompatible(clazz):
            additions += 1
    return findings, additions


def compare(module: str, previous: str, japicmp: Path, work: Path, output_dir: Path) -> bool:
    """Run japicmp for one module. Returns True when compatible."""
    module_dir = work / module
    module_dir.mkdir(parents=True, exist_ok=True)

    old_aar = download_verified(
        f"{MAVEN_CENTRAL}/{GROUP_PATH}/{module}/{previous}/{module}-{previous}.aar",
        module_dir / f"{module}-{previous}.aar",
    )
    old_raw = extract_classes_jar(old_aar, module_dir / "old-raw.jar")
    new_raw = extract_classes_jar(local_release_aar(module), module_dir / "new-raw.jar")
    old_jar = module_dir / "old.jar"
    new_jar = module_dir / "new.jar"
    old_kept, old_dropped = filter_jar(old_raw, old_jar)
    new_kept, new_dropped = filter_jar(new_raw, new_jar)
    log(f"{module}: previous {previous} -> {old_kept} classes compared ({old_dropped} R8-renamed dropped); "
        f"local -> {new_kept} compared ({new_dropped} dropped)")

    output_dir.mkdir(parents=True, exist_ok=True)
    html = output_dir / f"{module}.html"
    xml = output_dir / f"{module}.xml"
    cmd = [
        "java", "-jar", str(japicmp),
        "--old", str(old_jar),
        "--new", str(new_jar),
        "--ignore-missing-classes",
        "--only-modified",
        "--html-file", str(html),
        "--xml-file", str(xml),
    ]
    log("$ " + " ".join(cmd))
    result = subprocess.run(cmd, cwd=REPO_ROOT, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(f"japicmp exited {result.returncode} for {module}:\n{result.stdout}\n{result.stderr}")

    mapping = release_mapping(module)
    renamed = load_renamed_members(mapping)
    log(f"{module}: {'no R8 mapping file; every member compared by name' if mapping is None else f'{len(renamed)} partially kept classes from {mapping.relative_to(REPO_ROOT)}'}")
    findings, additions = evaluate_report(xml, renamed)
    if findings:
        print(f"{module}: INCOMPATIBLE with {previous}")
        for finding in findings:
            print(f"  {finding}")
    else:
        print(f"{module}: compatible with {previous} ({additions} classes with additions only)")
    log(f"{module}: report at {html}")
    return not findings


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--module", action="append", choices=MODULES, help="module to compare (default: both)")
    parser.add_argument("--previous", help="published version to compare against (default: latest on Maven Central)")
    parser.add_argument("--skip-build", action="store_true", help="reuse existing release AARs")
    parser.add_argument("--output-dir", default="build/api-compat", help="where to write the japicmp reports")
    args = parser.parse_args(argv)

    modules = tuple(args.module) if args.module else MODULES
    output_dir = (REPO_ROOT / args.output_dir).resolve()
    cache_dir = Path(os.environ.get("API_COMPAT_CACHE", tempfile.gettempdir())) / "mparticle-api-compat"
    cache_dir.mkdir(parents=True, exist_ok=True)

    if not args.skip_build:
        build_release_aars(modules)

    japicmp = japicmp_jar(cache_dir)
    all_compatible = True
    with tempfile.TemporaryDirectory(prefix="api-compat-") as tmp:
        work = Path(tmp)
        for module in modules:
            previous = args.previous or latest_published_version(module)
            if not compare(module, previous, japicmp, work, output_dir):
                all_compatible = False

    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as out:
            status = "compatible" if all_compatible else "**incompatible change detected**"
            out.write(f"### Binary compatibility\n\n{', '.join(modules)}: {status} with the last published release. "
                      f"Reports are attached as a workflow artifact.\n")
    return 0 if all_compatible else 2


if __name__ == "__main__":
    os.chdir(REPO_ROOT)
    try:
        sys.exit(main(sys.argv[1:]))
    except (subprocess.CalledProcessError, RuntimeError, OSError, zipfile.BadZipFile, KeyError, ET.ParseError) as error:
        log(f"error: {error}")
        sys.exit(1)
