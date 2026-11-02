#!/usr/bin/env python3
"""Compare the SDK's public surface with the last published release, using japicmp.

``./gradlew apiCheck`` guards the *compiled* surface of ``android-core`` and
``android-kit-base`` before R8 runs. This script guards what consumers link
against: the classes and members the last published release keeps under their
own names after R8.

R8 renames every class no keep rule names, and it can pick a different short
name in each build. Two minified jars therefore disagree on the signature of a
kept method that names such a class, and japicmp reads that method as removed
although nothing changed. So the comparison runs on jars built without R8:

1. This branch is built twice per module: ``assembleRelease`` for the release
   AAR, and ``bundleLibCompileToJarRelease`` for the same classes before R8
   (the un-minified jar).
2. The previous release's source is downloaded from its GitHub tag and its
   un-minified jar is built the same way.
3. japicmp compares the two un-minified jars. Every class has its real name on
   both sides, so a class R8 renamed cannot make a kept member read as removed.
4. Only findings on the previous release's public surface count. That surface
   is read from the release AAR published on Maven Central: a public class it
   keeps under its own name, and in that class a public or protected member it
   keeps under its own name. A class or member counts as kept when it appears,
   by name and parameter count, both in the published jar and in the
   un-minified build of the same tag. An incompatible member that is new on
   this branch counts in any class of the surface, so an abstract method added
   to a kept class is still reported.
5. This branch's release AAR must still keep every class and member of that
   surface under its own name. A keep rule that stops matching breaks
   consumers even when the source is unchanged, so it is reported as
   ``CLASS_NO_LONGER_KEPT`` or ``MEMBER_NO_LONGER_KEPT``.

An interface or superclass change counts unless every class it names is a class
of the build that the previous release does not keep by name. ``BuildConfig``
is dropped from both jars; its fields carry per-build values.

Any remaining binary- or source-incompatible change fails the run. Additions
are reported but do not fail it.

japicmp runs with ``--ignore-missing-classes`` and no classpath for Android or
third-party types, so changes that only manifest through unresolved external
supertypes are not analysed. The compile-time dump covers those.

Usage::

    scripts/api_compat_report.py [--module android-core] [--previous 6.1.2]
                                 [--previous-source DIR] [--skip-build]
                                 [--output-dir build/api-compat]
                                 [--comment-file PATH]

``--previous-source`` builds the previous release from a local checkout of its
tag instead of downloading the tag's source archive. ``--skip-build`` reuses
this branch's existing build outputs. The previous release's un-minified jars
are cached by version in ``API_COMPAT_CACHE`` (the temporary directory by
default), so a second run does not build them again.

``--comment-file`` writes a short markdown summary (one line per module, with
the findings collapsed behind a disclosure when a module is incompatible) to
``PATH``. The workflow posts that file as a pull request comment, the same
way the SDK Size Impact Report does, so an incompatibility is visible in the
PR conversation without opening the job logs. The same text is also appended
to ``GITHUB_STEP_SUMMARY`` when it is set, regardless of ``--comment-file``.

Exit codes: 0 compatible, 2 incompatible change found, 1 tooling error.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import struct
import subprocess
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from dataclasses import dataclass, field
from pathlib import Path, PurePosixPath

REPO_ROOT = Path(__file__).resolve().parent.parent
GROUP_PATH = "com/mparticle"
MAVEN_CENTRAL = "https://repo1.maven.org/maven2"
MODULES = ("android-core", "android-kit-base")
SOURCE_ARCHIVE = "https://github.com/mParticle/mparticle-android-sdk/archive/refs/tags/{tag}.tar.gz"
# The AGP task that jars a library's compiled Java and Kotlin classes before R8.
# tooling/custom-lint-rules/build.gradle depends on the same task.
UNMINIFIED_TASK = "bundleLibCompileToJarRelease"

JAPICMP_VERSION = "0.26.2"
JAPICMP_URL = (
    f"{MAVEN_CENTRAL}/com/github/siom79/japicmp/japicmp/{JAPICMP_VERSION}/"
    f"japicmp-{JAPICMP_VERSION}-jar-with-dependencies.jar"
)
JAPICMP_SHA256 = "d1eb211b7132eae1f9ee834270a8d19a9652d1983d3fe75ec564666ebef5c18d"

ACC_PUBLIC = 0x0001
ACC_PROTECTED = 0x0004
ACC_SYNTHETIC = 0x1000

# A member is keyed by its name and parameter count; a field's count is None and a
# constructor's name is "<init>".
MemberKey = tuple


def log(message: str) -> None:
    print(message, file=sys.stderr)


def run(cmd: list[str], cwd: Path, env: dict[str, str] | None = None) -> None:
    log("$ " + " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True, env=env)


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


def filter_jar(source: Path, dest: Path) -> int:
    """Copy ``source`` to ``dest`` without BuildConfig. Returns the number of classes kept."""
    kept = 0
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as out:
        for info in src.infolist():
            name = info.filename
            if name.endswith(".class"):
                if name.rsplit("/", 1)[-1] == "BuildConfig.class":
                    continue
                kept += 1
            out.writestr(info, src.read(name))
    return kept


def build_branch(modules: tuple[str, ...]) -> None:
    """Build this branch's release AARs and its un-minified jars."""
    version = (REPO_ROOT / "VERSION").read_text(encoding="utf-8").strip()
    tasks = [task for module in modules for task in (f":{module}:assembleRelease", f":{module}:{UNMINIFIED_TASK}")]
    run(["./gradlew", *tasks, f"-PVERSION={version}", "--console=plain", "-q"], cwd=REPO_ROOT)


def local_release_aar(module: str) -> Path:
    aar = REPO_ROOT / module / "build" / "outputs" / "aar" / f"{module}-release.aar"
    if not aar.exists():
        raise RuntimeError(f"no release AAR at {aar}; run without --skip-build")
    return aar


def unminified_jar(root: Path, module: str) -> Path:
    """The jar :data:`UNMINIFIED_TASK` wrote for ``module`` under the project ``root``: AGP 8
    writes it to ``compile_library_classes_jar/release/<task>/classes.jar``, AGP 7 to
    ``compile_library_classes_jar/release/classes.jar``."""
    intermediates = root / module / "build" / "intermediates"
    jars = sorted(
        path
        for path in (intermediates.rglob("classes.jar") if intermediates.is_dir() else ())
        if UNMINIFIED_TASK in path.parts or path.parts[-3:-1] == ("compile_library_classes_jar", "release")
    )
    if len(jars) != 1:
        raise RuntimeError(
            f"expected one un-minified classes jar under {intermediates}, found {len(jars)}; "
            f"run :{module}:{UNMINIFIED_TASK} (or this script without --skip-build)"
        )
    return jars[0]


def extract_source(archive: Path, dest: Path) -> tuple[Path, str | None]:
    """Extract a GitHub source archive into ``dest``.

    Returns the project root (the archive's one top-level directory) and the commit
    the archive was cut from (GitHub writes it in the archive's global header), if any.
    Refuses an archive with an absolute path, a ``..`` segment, a link or a device.
    """
    with tarfile.open(archive) as tar:
        members = tar.getmembers()
        roots = set()
        for member in members:
            path = PurePosixPath(member.name)
            if path.is_absolute() or ".." in path.parts or not (member.isfile() or member.isdir()):
                raise RuntimeError(f"refusing archive entry {member.name!r} in {archive.name}")
            roots.add(path.parts[0])
        if len(roots) != 1:
            raise RuntimeError(f"expected one top-level directory in {archive.name}, found {sorted(roots)}")
        if hasattr(tarfile, "data_filter"):
            tar.extractall(dest, filter="data")
        else:
            tar.extractall(dest)
        commit = tar.pax_headers.get("comment")
    return dest / roots.pop(), commit


def download_release_source(version: str, work: Path) -> tuple[Path, str | None]:
    """Download and extract the source of the release ``version`` from its GitHub tag."""
    for tag in (version, f"v{version}"):
        url = SOURCE_ARCHIVE.format(tag=tag)
        archive = work / f"source-{tag}.tar.gz"
        log(f"downloading {url}")
        try:
            with urllib.request.urlopen(url, timeout=300) as response, archive.open("wb") as out:
                shutil.copyfileobj(response, out)
        except urllib.error.HTTPError as error:
            if error.code == 404:
                continue
            raise
        root, commit = extract_source(archive, work / f"source-{tag}")
        found = (root / "VERSION").read_text(encoding="utf-8").strip()
        if found != version:
            raise RuntimeError(f"{url} has VERSION {found}, not {version}")
        log(f"previous release {version}: tag {tag}, commit {commit or 'unknown'}")
        return root, commit
    raise RuntimeError(f"no source archive for tag {version} or v{version} at {SOURCE_ARCHIVE.format(tag=version)}")


def previous_unminified_jars(
    version: str, modules: tuple[str, ...], source: Path | None, work: Path, cache_dir: Path
) -> dict[str, Path]:
    """Build the un-minified jars of the release ``version`` and cache them by version."""
    cached = {module: cache_dir / f"{module}-{version}-unminified.jar" for module in modules}
    if all(path.exists() for path in cached.values()):
        return cached
    commit = None
    if source is None:
        source, commit = download_release_source(version, work)
        gradlew = source / "gradlew"
        gradlew.chmod(gradlew.stat().st_mode | 0o100)
    env = dict(os.environ)
    # android-core/build.gradle reads the commit from git, else from MP_GIT_SHA; a source
    # archive is no git checkout.
    env["MP_GIT_SHA"] = commit if commit and len(commit) >= 7 else "0000000"
    tasks = [f":{module}:{UNMINIFIED_TASK}" for module in modules]
    run(["./gradlew", *tasks, f"-PVERSION={version}", "--console=plain", "-q"], cwd=source, env=env)
    for module, path in cached.items():
        shutil.copyfile(unminified_jar(source, module), path)
    return cached


def japicmp_jar(cache_dir: Path) -> Path:
    return download_verified(JAPICMP_URL, cache_dir / f"japicmp-{JAPICMP_VERSION}.jar", JAPICMP_SHA256)


# --- class files ------------------------------------------------------------

# Bytes after the tag of each constant pool entry other than Utf8 (JVMS 4.4).
_CONSTANT_SIZES = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}


@dataclass(frozen=True)
class ClassInfo:
    """What a class file says about a class's surface: whether the class is public, and
    the key of each of its public or protected members that is not synthetic."""

    public: bool
    members: frozenset


def parameter_count(descriptor: str) -> int:
    """The number of parameters in a method descriptor such as ``(I[JLjava/lang/String;)V``."""
    params = descriptor[1 : descriptor.index(")")]
    count = index = 0
    while index < len(params):
        while params[index] == "[":
            index += 1
        index = params.index(";", index) + 1 if params[index] == "L" else index + 1
        count += 1
    return count


def read_class(data: bytes) -> tuple[str, ClassInfo]:
    """The binary name (``com.mparticle.MParticle$Builder``) and :class:`ClassInfo` of a
    class file. Raises ``ValueError`` for bytes that are not a class file."""
    try:
        if data[:4] != b"\xca\xfe\xba\xbe":
            raise ValueError("not a class file")
        (count,) = struct.unpack_from(">H", data, 8)
        utf8: dict[int, str] = {}
        classes: dict[int, int] = {}
        offset, index = 10, 1
        while index < count:
            tag = data[offset]
            if tag == 1:
                (length,) = struct.unpack_from(">H", data, offset + 1)
                utf8[index] = data[offset + 3 : offset + 3 + length].decode("utf-8", "replace")
                offset += 3 + length
            else:
                if tag == 7:
                    (classes[index],) = struct.unpack_from(">H", data, offset + 1)
                offset += 1 + _CONSTANT_SIZES[tag]
            index += 2 if tag in (5, 6) else 1

        access, this_entry, _super_entry, interface_count = struct.unpack_from(">HHHH", data, offset)
        offset += 8 + 2 * interface_count

        def members(is_method: bool) -> set[MemberKey]:
            nonlocal offset
            (member_count,) = struct.unpack_from(">H", data, offset)
            offset += 2
            found = set()
            for _ in range(member_count):
                flags, name, descriptor, attribute_count = struct.unpack_from(">HHHH", data, offset)
                offset += 8
                for _ in range(attribute_count):
                    (length,) = struct.unpack_from(">I", data, offset + 2)
                    offset += 6 + length
                if flags & ACC_SYNTHETIC or not flags & (ACC_PUBLIC | ACC_PROTECTED) or utf8[name] == "<clinit>":
                    continue
                found.add((utf8[name], parameter_count(utf8[descriptor]) if is_method else None))
            return found

        keys = members(False) | members(True)
        name = utf8[classes[this_entry]].replace("/", ".")
    except (KeyError, IndexError, struct.error, UnicodeError) as error:
        raise ValueError(f"not a readable class file: {type(error).__name__}") from error
    return name, ClassInfo(public=bool(access & ACC_PUBLIC), members=frozenset(keys))


def read_jar(jar: Path) -> dict[str, ClassInfo]:
    """Every readable class in ``jar`` but BuildConfig, by binary name."""
    found: dict[str, ClassInfo] = {}
    with zipfile.ZipFile(jar) as archive:
        for entry in archive.namelist():
            if not entry.endswith(".class") or entry.rsplit("/", 1)[-1] == "BuildConfig.class":
                continue
            try:
                name, info = read_class(archive.read(entry))
            except ValueError:
                continue
            found[name] = info
    return found


def kept_surface(published: dict[str, ClassInfo], unminified: dict[str, ClassInfo]) -> dict[str, frozenset]:
    """The public surface a published release keeps under its own names after R8:
    ``{public class: member keys}`` for each class and member found by name both in the
    published jar and in the un-minified build of the same release."""
    return {
        name: published[name].members & unminified[name].members
        for name in published.keys() & unminified.keys()
        if unminified[name].public
    }


# --- findings ---------------------------------------------------------------


@dataclass
class Finding:
    class_name: str
    member: str
    changes: list[str] = field(default_factory=list)

    def __str__(self) -> str:
        where = self.class_name if not self.member else f"{self.class_name}#{self.member}"
        return f"{where}: {', '.join(self.changes)}"


@dataclass
class ModuleResult:
    module: str
    previous: str
    compared: int
    findings: list[Finding]
    additions: int

    @property
    def compatible(self) -> bool:
        return not self.findings


def _incompatible(element: ET.Element) -> bool:
    return element.get("binaryCompatible") == "false" or element.get("sourceCompatible") == "false"


def _change_types(element: ET.Element) -> list[str]:
    changes = element.find("compatibilityChanges")
    if changes is None:
        return []
    return [c.get("type") or "?" for c in changes if _incompatible(c)]


def _member_key(member: ET.Element, member_tag: str) -> MemberKey:
    if member_tag == "field":
        return (member.get("name") or "", None)
    count = len(member.findall("./parameters/parameter"))
    return ("<init>" if member_tag == "constructor" else member.get("name") or "", count)


def evaluate_report(
    xml_path: Path,
    surface: dict[str, frozenset],
    internal: set[str] | frozenset = frozenset(),
    new_kept: set[str] | frozenset = frozenset(),
) -> tuple[list[Finding], int]:
    """Return ``(incompatible findings, number of classes with additions only)`` for a
    japicmp report over two un-minified jars.

    ``surface`` is :func:`kept_surface` for the previous release: a class outside it is
    skipped, and in a class inside it a member counts when its key is in the surface or
    when it is new. ``internal`` is the build's classes the previous release does not
    keep by name; an interface or superclass change that names only such classes is
    skipped. ``new_kept`` is the classes this branch's release jar keeps by name; a new
    one counts towards the additions.
    """
    root = ET.parse(xml_path).getroot()
    findings: list[Finding] = []
    additions = 0
    for clazz in root.iter("class"):
        class_name = clazz.get("fullyQualifiedName") or "?"
        if class_name not in surface:
            if class_name in new_kept and clazz.get("changeStatus") == "NEW" and not _incompatible(clazz):
                additions += 1
            continue

        class_changes = [
            t for t in _change_types(clazz) if not t.startswith(("INTERFACE_", "SUPERCLASS_"))
        ]
        for interface in clazz.iterfind("./interfaces/interface"):
            if _incompatible(interface) and (interface.get("fullyQualifiedName") or "") not in internal:
                class_changes.extend(_change_types(interface) or [f"INTERFACE_{interface.get('changeStatus')}"])
        superclass = clazz.find("superclass")
        if superclass is not None and _incompatible(superclass):
            names = [n for n in (superclass.get("superclassOld"), superclass.get("superclassNew")) if n]
            if not names or not all(n in internal for n in names):
                class_changes.extend(_change_types(superclass) or ["SUPERCLASS_MODIFIED"])
        if class_changes:
            findings.append(Finding(class_name, "", sorted(set(class_changes))))

        kept_members = surface[class_name]
        for section, member_tag in (("methods", "method"), ("constructors", "constructor"), ("fields", "field")):
            for member in clazz.iterfind(f"./{section}/{member_tag}"):
                if not _incompatible(member):
                    continue
                key = _member_key(member, member_tag)
                if key not in kept_members and member.get("changeStatus") != "NEW":
                    continue
                findings.append(Finding(class_name, key[0], sorted(set(_change_types(member))) or ["INCOMPATIBLE"]))

        if not class_changes and clazz.get("changeStatus") in ("NEW", "MODIFIED") and not _incompatible(clazz):
            additions += 1
    return findings, additions


def keep_findings(
    surface: dict[str, frozenset], new_unminified: dict[str, ClassInfo], new_release: dict[str, ClassInfo]
) -> list[Finding]:
    """Each class and member of ``surface`` that this branch still declares but its
    release jar no longer keeps under its own name. One this branch no longer declares
    is japicmp's removal, so it is not repeated here."""
    findings: list[Finding] = []
    for class_name in sorted(surface):
        if class_name not in new_unminified:
            continue
        if class_name not in new_release:
            findings.append(Finding(class_name, "", ["CLASS_NO_LONGER_KEPT"]))
            continue
        lost = (surface[class_name] & new_unminified[class_name].members) - new_release[class_name].members
        for name, count in sorted(lost, key=lambda key: (key[0], -1 if key[1] is None else key[1])):
            findings.append(Finding(class_name, name, ["MEMBER_NO_LONGER_KEPT"]))
    return findings


def compare(
    module: str, previous: str, previous_jar: Path, japicmp: Path, work: Path, output_dir: Path
) -> ModuleResult:
    """Run japicmp for one module over the previous release's un-minified jar and this branch's."""
    module_dir = work / module
    module_dir.mkdir(parents=True, exist_ok=True)

    published_aar = download_verified(
        f"{MAVEN_CENTRAL}/{GROUP_PATH}/{module}/{previous}/{module}-{previous}.aar",
        module_dir / f"{module}-{previous}.aar",
    )
    published = read_jar(extract_classes_jar(published_aar, module_dir / "published.jar"))
    new_release = read_jar(extract_classes_jar(local_release_aar(module), module_dir / "release.jar"))
    new_raw = unminified_jar(REPO_ROOT, module)
    old_full, new_full = read_jar(previous_jar), read_jar(new_raw)

    surface = kept_surface(published, old_full)
    internal = (old_full.keys() | new_full.keys()) - surface.keys()
    new_kept = new_release.keys() & new_full.keys()
    log(f"{module}: {previous} keeps {len(surface)} public classes by name, of {len(old_full)} built without R8")

    old_jar, new_jar = module_dir / "old.jar", module_dir / "new.jar"
    filter_jar(previous_jar, old_jar)
    filter_jar(new_raw, new_jar)
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

    findings, additions = evaluate_report(xml, surface, internal, new_kept)
    findings.extend(keep_findings(surface, new_full, new_release))
    if findings:
        print(f"{module}: INCOMPATIBLE with {previous}")
        for finding in findings:
            print(f"  {finding}")
    else:
        print(f"{module}: compatible with {previous} ({additions} classes with additions only)")
    log(f"{module}: report at {html}")
    return ModuleResult(module=module, previous=previous, compared=len(surface), findings=findings, additions=additions)


def render_report(results: list[ModuleResult]) -> str:
    """Render the per-module results as the short markdown both the job summary and the
    pull request comment use. Quick to read by default: one line per module, with the
    findings behind a disclosure only when a module is incompatible."""
    lines = ["<!-- binary-compatibility-report -->", "### Binary compatibility", ""]
    for result in results:
        compared_word = "class" if result.compared == 1 else "classes"
        if result.compatible:
            lines.append(
                f"- ✅ `{result.module}` is compatible with {result.previous} "
                f"({result.compared} {compared_word} compared, {result.additions} with additions only)."
            )
        else:
            affected_classes = len({finding.class_name for finding in result.findings})
            findings_word = "finding" if len(result.findings) == 1 else "findings"
            lines.append(
                f"- ❌ `{result.module}` is **incompatible** with {result.previous}: "
                f"{len(result.findings)} {findings_word} across {affected_classes} of {result.compared} "
                f"compared {compared_word}."
            )
            lines.append("")
            lines.append("  <details><summary>Findings</summary>")
            lines.append("")
            for finding in result.findings:
                lines.append(f"  - `{finding}`")
            lines.append("")
            lines.append("  </details>")
    lines.append("")
    lines.append(
        "Compares this branch with the last version published to Maven Central, both built "
        "without R8, over the public classes and members that release keeps under their own "
        "names. It also checks that this branch's release AAR still keeps them (see the script "
        "docstring). Full japicmp HTML/XML reports are attached as the `api-compat-reports` "
        "workflow artifact. This check is not in the required-check set yet, so a red result "
        "here does not block merging."
    )
    return "\n".join(lines) + "\n"


def write_report(report: str, comment_file: str | None) -> None:
    """Write ``report`` to ``GITHUB_STEP_SUMMARY`` (if set) and to ``comment_file`` (if given)."""
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as out:
            out.write(report)
    if comment_file:
        Path(comment_file).write_text(report, encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--module", action="append", choices=MODULES, help="module to compare (default: both)")
    parser.add_argument("--previous", help="published version to compare against (default: latest on Maven Central)")
    parser.add_argument(
        "--previous-source",
        type=Path,
        help="a checkout of the previous release's tag, used for every module (default: download its source)",
    )
    parser.add_argument("--skip-build", action="store_true", help="reuse this branch's existing build outputs")
    parser.add_argument("--output-dir", default="build/api-compat", help="where to write the japicmp reports")
    parser.add_argument("--comment-file", help="write the markdown summary here as well, for posting as a pull request comment")
    args = parser.parse_args(argv)

    modules = tuple(args.module) if args.module else MODULES
    output_dir = (REPO_ROOT / args.output_dir).resolve()
    cache_dir = Path(os.environ.get("API_COMPAT_CACHE", tempfile.gettempdir())) / "mparticle-api-compat"
    cache_dir.mkdir(parents=True, exist_ok=True)

    try:
        if not args.skip_build:
            build_branch(modules)
        japicmp = japicmp_jar(cache_dir)
        results: list[ModuleResult] = []
        with tempfile.TemporaryDirectory(prefix="api-compat-") as tmp:
            work = Path(tmp)
            previous = {module: args.previous or latest_published_version(module) for module in modules}
            previous_jars: dict[str, Path] = {}
            for version in sorted(set(previous.values())):
                built = tuple(module for module in modules if previous[module] == version)
                previous_jars.update(previous_unminified_jars(version, built, args.previous_source, work, cache_dir))
            for module in modules:
                results.append(compare(module, previous[module], previous_jars[module], japicmp, work, output_dir))
    except (
        subprocess.CalledProcessError, RuntimeError, OSError, zipfile.BadZipFile, tarfile.TarError, KeyError,
        ET.ParseError,
    ) as error:
        # Write the comment and summary even on a tooling failure, not just an incompatible
        # result, so the workflow's "post a comment" steps can run unconditionally afterwards
        # without needing to check whether this step produced a file.
        log(f"error: {error}")
        write_report(f"<!-- binary-compatibility-report -->\n### Binary compatibility\n\n"
                      f"⚠️ Could not complete the comparison: `{error}`. See the job log for details.\n",
                      args.comment_file)
        return 1

    report = render_report(results)
    write_report(report, args.comment_file)
    all_compatible = all(result.compatible for result in results)
    return 0 if all_compatible else 2


if __name__ == "__main__":
    os.chdir(REPO_ROOT)
    try:
        sys.exit(main(sys.argv[1:]))
    except (subprocess.CalledProcessError, RuntimeError, OSError, zipfile.BadZipFile, tarfile.TarError, KeyError,
            ET.ParseError) as error:
        log(f"error: {error}")
        sys.exit(1)
