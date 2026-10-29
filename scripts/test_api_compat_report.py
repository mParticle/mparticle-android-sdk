"""Unit tests for scripts/api_compat_report.py.

Run with ``python3 -m unittest scripts/test_api_compat_report.py``.
"""

from __future__ import annotations

import importlib.util
import io
import struct
import sys
import tarfile
import tempfile
import unittest
import urllib.error
import zipfile
from pathlib import Path
from unittest import mock

_SPEC = importlib.util.spec_from_file_location(
    "api_compat_report", Path(__file__).resolve().parent / "api_compat_report.py"
)
report = importlib.util.module_from_spec(_SPEC)
sys.modules[_SPEC.name] = report
_SPEC.loader.exec_module(report)

PUBLIC, PROTECTED, PRIVATE, STATIC, FINAL, SYNTHETIC = 0x1, 0x4, 0x2, 0x8, 0x10, 0x1000


def class_file(name, super_name="java/lang/Object", interfaces=(), fields=(), methods=(), access=0x21, long_constant=False):
    """A minimal class file: a constant pool, the class header and its fields and methods, each member
    with one attribute of four bytes, as a real Code or ConstantValue attribute would follow it."""
    pool: list = []
    entries: dict = {}

    def utf8(text):
        if ("utf8", text) not in entries:
            encoded = text.encode("utf-8")
            pool.append(b"\x01" + struct.pack(">H", len(encoded)) + encoded)
            entries[("utf8", text)] = len(pool)
        return entries[("utf8", text)]

    def klass(text):
        if ("class", text) not in entries:
            pool.append(b"\x07" + struct.pack(">H", utf8(text)))
            entries[("class", text)] = len(pool)
        return entries[("class", text)]

    if long_constant:
        pool.extend([b"\x05" + struct.pack(">q", 42), None])
    this, superclass = klass(name), klass(super_name)
    faces = [klass(i) for i in interfaces]
    attribute = utf8("Attribute")

    def members(items):
        out = struct.pack(">H", len(items))
        for flags, member, descriptor in items:
            out += struct.pack(">HHHH", flags, utf8(member), utf8(descriptor), 1)
            out += struct.pack(">HI", attribute, 4) + b"\x00\x01\x02\x03"
        return out

    body = members(fields) + members(methods) + struct.pack(">H", 0)
    header = b"\xca\xfe\xba\xbe" + struct.pack(">HHH", 0, 52, len(pool) + 1) + b"".join(e for e in pool if e)
    header += struct.pack(">HHHH", access, this, superclass, len(faces))
    return header + b"".join(struct.pack(">H", f) for f in faces) + body


def write_jar(path: Path, classes: dict) -> Path:
    with zipfile.ZipFile(path, "w") as archive:
        for name, data in classes.items():
            archive.writestr(name.replace(".", "/") + ".class", data)
    return path


CONFIG_MANAGER = "com.mparticle.internal.ConfigManager"
UPLOAD_SETTINGS = "com.mparticle.internal.UploadSettings"


def config_manager(upload_settings_type: str, extra_methods=()):
    """ConfigManager as a build writes it: R8 keeps it and setLastUploadSettings by name, and the
    parameter names UploadSettings under whatever name that build gave it."""
    return class_file(
        CONFIG_MANAGER.replace(".", "/"),
        fields=((PUBLIC | STATIC | FINAL, "PREFERENCES_FILE", "Ljava/lang/String;"),),
        methods=(
            (PUBLIC, "<init>", "(Landroid/content/Context;)V"),
            (PUBLIC, "setLastUploadSettings", f"(L{upload_settings_type};)V"),
            (PUBLIC, "getNetworkOptions", "()Lcom/mparticle/networking/NetworkOptions;"),
            *extra_methods,
        ),
    )


def upload_settings(name: str):
    return class_file(name, fields=((0x12, "a", "Ljava/lang/String;"), (0x12, "b", "J")), long_constant=True)


# The 6.1.9 AAR on Maven Central: R8 named UploadSettings u0 and renamed refresh() to a().
PUBLISHED = {
    CONFIG_MANAGER: config_manager("com/mparticle/u0", extra_methods=((PUBLIC, "a", "()V"),)),
    "com.mparticle.u0": upload_settings("com/mparticle/u0"),
}
# 6.1.9 and this branch built without R8: every class has its own name.
UNMINIFIED = {
    CONFIG_MANAGER: config_manager(UPLOAD_SETTINGS.replace(".", "/"), extra_methods=((PUBLIC, "refresh", "()V"),)),
    UPLOAD_SETTINGS: upload_settings(UPLOAD_SETTINGS.replace(".", "/")),
}
# This branch's release AAR: R8 now names UploadSettings t0.
RELEASE = {
    CONFIG_MANAGER: config_manager("com/mparticle/t0", extra_methods=((PUBLIC, "a", "()V"),)),
    "com.mparticle.t0": upload_settings("com/mparticle/t0"),
}


def read(classes: dict) -> dict:
    with tempfile.TemporaryDirectory() as tmp:
        return report.read_jar(write_jar(Path(tmp) / "classes.jar", classes))


class ParameterCountTest(unittest.TestCase):
    def test_counts_each_parameter_once(self) -> None:
        self.assertEqual(report.parameter_count("()V"), 0)
        self.assertEqual(report.parameter_count("(I)V"), 1)
        self.assertEqual(report.parameter_count("(I[JLjava/lang/String;[[Lcom/mparticle/u0;Z)Ljava/lang/Object;"), 5)


class ReadClassTest(unittest.TestCase):
    def test_reads_the_name_and_the_public_and_protected_members(self) -> None:
        data = class_file(
            "com/mparticle/MParticle$Builder",
            fields=((PUBLIC, "apiKey", "Ljava/lang/String;"), (PRIVATE, "secret", "Ljava/lang/String;")),
            methods=(
                (PUBLIC, "<init>", "(Landroid/content/Context;)V"),
                (STATIC, "<clinit>", "()V"),
                (PROTECTED, "build", "(IJ)Lcom/mparticle/MParticle;"),
                (PUBLIC | SYNTHETIC, "access$000", "(Lcom/mparticle/MParticle$Builder;)V"),
                (0, "packagePrivate", "()V"),
            ),
            long_constant=True,
        )
        name, info = report.read_class(data)
        self.assertEqual(name, "com.mparticle.MParticle$Builder")
        self.assertTrue(info.public)
        self.assertEqual(info.members, {("apiKey", None), ("<init>", 1), ("build", 2)})

    def test_a_class_without_the_public_flag_is_not_public(self) -> None:
        _, info = report.read_class(class_file("com/mparticle/internal/Helper", access=0x20))
        self.assertFalse(info.public)

    def test_bytes_that_are_not_a_class_file_are_refused(self) -> None:
        with self.assertRaises(ValueError):
            report.read_class(b"not a class")
        with self.assertRaises(ValueError):
            report.read_class(b"\xca\xfe\xba\xbe\x00\x00\x00\x34\x00\x05\x01")

    def test_read_jar_skips_build_config_and_unreadable_entries(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            jar = write_jar(Path(tmp) / "classes.jar", {
                "com.mparticle.BuildConfig": class_file("com/mparticle/BuildConfig"),
                "com.mparticle.Broken": b"\xca\xfe\xba\xbe",
                "com.mparticle.MParticle": class_file("com/mparticle/MParticle"),
            })
            self.assertEqual(set(report.read_jar(jar)), {"com.mparticle.MParticle"})


class KeptSurfaceTest(unittest.TestCase):
    def test_the_surface_is_what_the_published_jar_keeps_by_name(self) -> None:
        surface = report.kept_surface(read(PUBLISHED), read(UNMINIFIED))
        self.assertEqual(set(surface), {CONFIG_MANAGER})
        self.assertEqual(
            surface[CONFIG_MANAGER],
            {("PREFERENCES_FILE", None), ("<init>", 1), ("setLastUploadSettings", 1), ("getNetworkOptions", 0)},
        )

    def test_a_class_that_is_not_public_in_the_source_is_left_out(self) -> None:
        hidden = {"com.mparticle.internal.Helper": class_file("com/mparticle/internal/Helper", access=0x20)}
        self.assertEqual(report.kept_surface(read(hidden), read(hidden)), {})


class KeepFindingsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.surface = report.kept_surface(read(PUBLISHED), read(UNMINIFIED))

    def test_a_kept_method_whose_parameter_r8_renamed_differently_is_still_kept(self) -> None:
        self.assertEqual(report.keep_findings(self.surface, read(UNMINIFIED), read(RELEASE)), [])

    def test_a_member_no_longer_kept_by_name_is_reported(self) -> None:
        release = dict(RELEASE)
        release[CONFIG_MANAGER] = class_file(
            CONFIG_MANAGER.replace(".", "/"),
            fields=((PUBLIC | STATIC | FINAL, "PREFERENCES_FILE", "Ljava/lang/String;"),),
            methods=((PUBLIC, "<init>", "(Landroid/content/Context;)V"), (PUBLIC, "b", "(Lcom/mparticle/t0;)V")),
        )
        findings = [str(f) for f in report.keep_findings(self.surface, read(UNMINIFIED), read(release))]
        self.assertEqual(
            findings,
            [
                f"{CONFIG_MANAGER}#getNetworkOptions: MEMBER_NO_LONGER_KEPT",
                f"{CONFIG_MANAGER}#setLastUploadSettings: MEMBER_NO_LONGER_KEPT",
            ],
        )

    def test_a_class_no_longer_kept_by_name_is_reported(self) -> None:
        release = {"com.mparticle.internal.c0": class_file("com/mparticle/internal/c0")}
        findings = [str(f) for f in report.keep_findings(self.surface, read(UNMINIFIED), read(release))]
        self.assertEqual(findings, [f"{CONFIG_MANAGER}: CLASS_NO_LONGER_KEPT"])

    def test_a_class_or_member_removed_from_the_source_is_left_to_japicmp(self) -> None:
        self.assertEqual(report.keep_findings(self.surface, {}, {}), [])
        unminified = dict(UNMINIFIED)
        unminified[CONFIG_MANAGER] = class_file(CONFIG_MANAGER.replace(".", "/"))
        release = {CONFIG_MANAGER: class_file(CONFIG_MANAGER.replace(".", "/"))}
        self.assertEqual(report.keep_findings(self.surface, read(unminified), read(release)), [])


class EvaluateReportTest(unittest.TestCase):
    """A japicmp report over the two un-minified jars."""

    XML = """\
<japicmp accessModifier="PROTECTED" onlyModifications="true">
<classes>
<class fullyQualifiedName="com.mparticle.internal.ConfigManager" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges/>
  <fields>
    <field name="PREFERENCES_FILE" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="FIELD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </field>
  </fields>
  <methods>
    <method name="refresh" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
      <parameters/>
    </method>
    <method name="getNetworkOptions" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_NOW_FINAL" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
      <parameters/>
    </method>
    <method name="onUpload" changeStatus="NEW" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_ABSTRACT_ADDED_TO_CLASS" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
      <parameters><parameter type="java.lang.String"/></parameters>
    </method>
    <method name="describe" changeStatus="NEW" binaryCompatible="true" sourceCompatible="true"><compatibilityChanges/></method>
  </methods>
  <constructors>
    <constructor name="ConfigManager" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="CONSTRUCTOR_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
      <parameters><parameter type="android.content.Context"/></parameters>
    </constructor>
    <constructor name="ConfigManager" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="CONSTRUCTOR_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
      <parameters><parameter type="android.content.Context"/><parameter type="int"/></parameters>
    </constructor>
  </constructors>
  <interfaces>
    <interface fullyQualifiedName="com.mparticle.internal.UploadListener" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="INTERFACE_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </interface>
    <interface fullyQualifiedName="java.io.Serializable" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="INTERFACE_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </interface>
  </interfaces>
  <superclass superclassOld="com.mparticle.internal.BaseManager" superclassNew="java.lang.Object" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
    <compatibilityChanges><compatibilityChange type="SUPERCLASS_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
  </superclass>
</class>
<class fullyQualifiedName="com.mparticle.internal.UploadSettings" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges><compatibilityChange type="CLASS_NOW_FINAL" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
  <fields/><methods/><constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.MParticle" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges/>
  <superclass superclassOld="com.mparticle.internal.BaseManager" superclassNew="com.mparticle.internal.Base" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
    <compatibilityChanges><compatibilityChange type="SUPERCLASS_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
  </superclass>
  <fields/><methods/><constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.rokt.RoktSession" changeStatus="NEW" binaryCompatible="true" sourceCompatible="true">
  <compatibilityChanges/><fields/><methods/><constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.internal.NewHelper" changeStatus="NEW" binaryCompatible="true" sourceCompatible="true">
  <compatibilityChanges/><fields/><methods/><constructors/><interfaces/>
</class>
</classes>
</japicmp>
"""

    SURFACE = {
        CONFIG_MANAGER: frozenset(
            {("PREFERENCES_FILE", None), ("<init>", 1), ("setLastUploadSettings", 1), ("getNetworkOptions", 0)}
        ),
        "com.mparticle.MParticle": frozenset(),
    }
    INTERNAL = {
        UPLOAD_SETTINGS,
        "com.mparticle.internal.UploadListener",
        "com.mparticle.internal.BaseManager",
        "com.mparticle.internal.Base",
        "com.mparticle.internal.NewHelper",
    }

    def setUp(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            xml = Path(tmp) / "report.xml"
            xml.write_text(self.XML, encoding="utf-8")
            findings, self.additions = report.evaluate_report(
                xml, self.SURFACE, self.INTERNAL, {"com.mparticle.rokt.RoktSession"}
            )
        self.findings = [str(f) for f in findings]

    def test_members_the_previous_release_keeps_by_name_are_reported(self) -> None:
        self.assertIn(f"{CONFIG_MANAGER}#PREFERENCES_FILE: FIELD_REMOVED", self.findings)
        self.assertIn(f"{CONFIG_MANAGER}#getNetworkOptions: METHOD_NOW_FINAL", self.findings)
        self.assertIn(f"{CONFIG_MANAGER}#<init>: CONSTRUCTOR_REMOVED", self.findings)

    def test_a_member_r8_renamed_in_the_previous_release_is_skipped(self) -> None:
        self.assertFalse(any("#refresh" in f for f in self.findings))
        # Only the one-parameter constructor is kept by name.
        self.assertEqual(sum("#<init>" in f for f in self.findings), 1)

    def test_an_incompatible_new_member_of_a_kept_class_is_reported(self) -> None:
        self.assertIn(f"{CONFIG_MANAGER}#onUpload: METHOD_ABSTRACT_ADDED_TO_CLASS", self.findings)
        self.assertFalse(any("#describe" in f for f in self.findings))

    def test_a_class_the_previous_release_does_not_keep_by_name_is_skipped(self) -> None:
        self.assertFalse(any(f.startswith(UPLOAD_SETTINGS) for f in self.findings))

    def test_interface_and_superclass_changes_count_unless_only_internal_classes_are_named(self) -> None:
        self.assertIn(f"{CONFIG_MANAGER}: INTERFACE_REMOVED, SUPERCLASS_REMOVED", self.findings)
        self.assertFalse(any(f.startswith("com.mparticle.MParticle") for f in self.findings))

    def test_only_new_classes_kept_by_name_count_as_additions(self) -> None:
        self.assertEqual(self.additions, 1)

    def test_nothing_else_is_reported(self) -> None:
        self.assertEqual(len(self.findings), 5)


class FilterJarTest(unittest.TestCase):
    def test_build_config_is_dropped(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            source = write_jar(Path(tmp) / "in.jar", {
                "com.mparticle.BuildConfig": class_file("com/mparticle/BuildConfig"),
                "com.mparticle.MParticle": class_file("com/mparticle/MParticle"),
            })
            self.assertEqual(report.filter_jar(source, Path(tmp) / "out.jar"), 1)
            with zipfile.ZipFile(Path(tmp) / "out.jar") as out:
                self.assertEqual(out.namelist(), ["com/mparticle/MParticle.class"])


class UnminifiedJarTest(unittest.TestCase):
    def test_finds_the_one_jar_the_task_wrote(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            jar = Path(tmp) / "android-core/build/intermediates/compile_library_classes_jar/release"
            jar = jar / report.UNMINIFIED_TASK / "classes.jar"
            jar.parent.mkdir(parents=True)
            jar.write_bytes(b"")
            self.assertEqual(report.unminified_jar(Path(tmp), "android-core"), jar)

    def test_finds_the_jar_where_agp_7_writes_it(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            jar = Path(tmp) / "android-core/build/intermediates/compile_library_classes_jar/release/classes.jar"
            jar.parent.mkdir(parents=True)
            jar.write_bytes(b"")
            other = Path(tmp) / "android-core/build/intermediates/runtime_library_classes_jar/release/classes.jar"
            other.parent.mkdir(parents=True)
            other.write_bytes(b"")
            self.assertEqual(report.unminified_jar(Path(tmp), "android-core"), jar)

    def test_no_jar_is_a_tooling_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp, self.assertRaises(RuntimeError):
            report.unminified_jar(Path(tmp), "android-core")


def source_archive(path: Path, files: dict, commit: str | None = "5d8ddf31c90ab3e7bae1353e7496984d07c47591",
                   extra=()) -> Path:
    """A source archive in the shape GitHub serves: one top-level directory and the commit in
    the global header."""
    headers = {"comment": commit} if commit else {}
    with tarfile.open(path, "w:gz", format=tarfile.PAX_FORMAT, pax_headers=headers) as tar:
        for name, text in files.items():
            data = text.encode("utf-8")
            info = tarfile.TarInfo(name)
            info.size = len(data)
            info.mode = 0o755 if name.endswith("gradlew") else 0o644
            tar.addfile(info, io.BytesIO(data))
        for info in extra:
            tar.addfile(info)
    return path


class ExtractSourceTest(unittest.TestCase):
    def test_returns_the_project_root_and_the_commit(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            archive = source_archive(Path(tmp) / "a.tar.gz", {"sdk-6.1.9/VERSION": "6.1.9\n", "sdk-6.1.9/gradlew": "#!/bin/sh\n"})
            root, commit = report.extract_source(archive, Path(tmp) / "out")
            self.assertEqual(root, Path(tmp) / "out" / "sdk-6.1.9")
            self.assertEqual((root / "VERSION").read_text(), "6.1.9\n")
            self.assertEqual(commit, "5d8ddf31c90ab3e7bae1353e7496984d07c47591")

    def test_refuses_a_path_outside_the_project_or_a_link(self) -> None:
        link = tarfile.TarInfo("sdk/link")
        link.type, link.linkname = tarfile.SYMTYPE, "/etc/passwd"
        for files, extra in (({"sdk/VERSION": "1", "sdk/../escape": "x"}, ()), ({"sdk/VERSION": "1"}, (link,)),
                             ({"sdk/VERSION": "1", "other/VERSION": "1"}, ())):
            with self.subTest(files=files, extra=extra), tempfile.TemporaryDirectory() as tmp:
                archive = source_archive(Path(tmp) / "a.tar.gz", files, extra=extra)
                with self.assertRaises(RuntimeError):
                    report.extract_source(archive, Path(tmp) / "out")
                self.assertFalse((Path(tmp) / "out").exists())


class DownloadReleaseSourceTest(unittest.TestCase):
    def _serve(self, archives: dict):
        def urlopen(url, timeout):
            if url not in archives:
                raise urllib.error.HTTPError(url, 404, "Not Found", None, None)
            return open(archives[url], "rb")
        return mock.patch.object(report.urllib.request, "urlopen", side_effect=urlopen)

    def test_falls_back_to_the_v_prefixed_tag(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            archive = source_archive(Path(tmp) / "src.tar.gz", {"sdk-5.80.0/VERSION": "5.80.0\n"})
            work = Path(tmp) / "work"
            work.mkdir()
            with self._serve({report.SOURCE_ARCHIVE.format(tag="v5.80.0"): archive}):
                root, commit = report.download_release_source("5.80.0", work)
            self.assertEqual((root / "VERSION").read_text().strip(), "5.80.0")
            self.assertTrue(commit)

    def test_a_tag_with_another_version_is_a_tooling_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            archive = source_archive(Path(tmp) / "src.tar.gz", {"sdk/VERSION": "6.1.8\n"})
            work = Path(tmp) / "work"
            work.mkdir()
            with self._serve({report.SOURCE_ARCHIVE.format(tag="6.1.9"): archive}), self.assertRaises(RuntimeError):
                report.download_release_source("6.1.9", work)

    def test_no_tag_is_a_tooling_error(self) -> None:
        with tempfile.TemporaryDirectory() as tmp, self._serve({}), self.assertRaises(RuntimeError):
            report.download_release_source("6.1.9", Path(tmp))


class PreviousUnminifiedJarsTest(unittest.TestCase):
    def test_builds_the_tag_once_and_caches_the_jars_by_version(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            source = Path(tmp) / "sdk-6.1.9"
            source.mkdir()
            (source / "gradlew").write_text("#!/bin/sh\n")
            cache, work = Path(tmp) / "cache", Path(tmp) / "work"
            cache.mkdir()
            work.mkdir()
            calls = []

            def gradle(cmd, cwd, env=None):
                calls.append((cmd, cwd, env))
                for module in report.MODULES:
                    jar = cwd / module / "build/intermediates/compile_library_classes_jar/release"
                    jar = jar / report.UNMINIFIED_TASK / "classes.jar"
                    jar.parent.mkdir(parents=True)
                    jar.write_bytes(module.encode())

            commit = "5d8ddf31c90ab3e7bae1353e7496984d07c47591"
            with mock.patch.object(report, "download_release_source", return_value=(source, commit)), \
                    mock.patch.object(report, "run", side_effect=gradle):
                first = report.previous_unminified_jars("6.1.9", report.MODULES, None, work, cache)
                second = report.previous_unminified_jars("6.1.9", report.MODULES, None, work, cache)

            self.assertEqual(len(calls), 1)
            cmd, cwd, env = calls[0]
            self.assertEqual(cwd, source)
            self.assertIn("-PVERSION=6.1.9", cmd)
            self.assertIn(f":android-core:{report.UNMINIFIED_TASK}", cmd)
            self.assertEqual(env["MP_GIT_SHA"], commit)
            self.assertEqual(first, second)
            self.assertEqual(first["android-core"].read_bytes(), b"android-core")


class RenderReportTest(unittest.TestCase):
    def test_compatible_module_is_one_line_with_no_disclosure(self) -> None:
        result = report.ModuleResult(module="android-kit-base", previous="6.1.2", compared=44, findings=[], additions=2)
        text = report.render_report([result])
        self.assertIn("<!-- binary-compatibility-report -->", text)
        self.assertIn("✅ `android-kit-base` is compatible with 6.1.2 (44 classes compared, 2 with additions only).", text)
        self.assertNotIn("<details>", text)

    def test_incompatible_module_lists_findings_behind_a_disclosure(self) -> None:
        finding = report.Finding("com.mparticle.AttributionError", "getMessage", ["METHOD_REMOVED"])
        result = report.ModuleResult(module="android-core", previous="6.1.2", compared=115, findings=[finding], additions=0)
        text = report.render_report([result])
        self.assertIn("❌ `android-core` is **incompatible** with 6.1.2: 1 finding across 1 of 115 compared classes.", text)
        self.assertIn("<details><summary>Findings</summary>", text)
        self.assertIn("com.mparticle.AttributionError#getMessage: METHOD_REMOVED", text)

    def test_compatible_module_with_one_compared_class_is_singular(self) -> None:
        result = report.ModuleResult(module="android-kit-base", previous="6.1.2", compared=1, findings=[], additions=0)
        text = report.render_report([result])
        self.assertIn("compatible with 6.1.2 (1 class compared, 0 with additions only).", text)

    def test_renders_one_line_per_module(self) -> None:
        results = [
            report.ModuleResult(module="android-core", previous="6.1.2", compared=115, findings=[], additions=0),
            report.ModuleResult(module="android-kit-base", previous="6.1.2", compared=44, findings=[], additions=0),
        ]
        text = report.render_report(results)
        self.assertIn("`android-core`", text)
        self.assertIn("`android-kit-base`", text)


class MainErrorHandlingTest(unittest.TestCase):
    def test_tooling_error_still_writes_the_comment_file_and_returns_1(self) -> None:
        # japicmp_jar runs even with --skip-build, so patching it exercises the error path
        # without touching the network or Gradle.
        with tempfile.TemporaryDirectory() as tmp:
            comment_file = Path(tmp) / "comment.md"
            with mock.patch.object(report, "japicmp_jar", side_effect=RuntimeError("boom")):
                exit_code = report.main(["--skip-build", "--comment-file", str(comment_file)])
            self.assertEqual(exit_code, 1)
            text = comment_file.read_text(encoding="utf-8")
            self.assertIn("<!-- binary-compatibility-report -->", text)
            self.assertIn("boom", text)


if __name__ == "__main__":
    unittest.main()
