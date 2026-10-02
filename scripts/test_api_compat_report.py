"""Unit tests for scripts/api_compat_report.py.

Run with ``python3 -m unittest scripts/test_api_compat_report.py``.
"""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

_SPEC = importlib.util.spec_from_file_location(
    "api_compat_report", Path(__file__).resolve().parent / "api_compat_report.py"
)
report = importlib.util.module_from_spec(_SPEC)
sys.modules[_SPEC.name] = report
_SPEC.loader.exec_module(report)


class ClassNameFilterTest(unittest.TestCase):
    def test_r8_renamed_classes_are_dropped(self) -> None:
        for name in (
            "com/mparticle/a.class",
            "com/mparticle/a1$b.class",
            "com/mparticle/d0.class",
            "com/mparticle/MParticle$a.class",
            "com/mparticle/internal/CoreCallbacks$KitListener$b.class",
            "com/mparticle/MPEvent$1.class",
            "com.mparticle.identity.a",
        ):
            with self.subTest(name=name):
                self.assertTrue(report.is_obfuscated_class(name))

    def test_named_classes_are_kept(self) -> None:
        for name in (
            "com/mparticle/MParticle.class",
            "com/mparticle/MParticle$Builder.class",
            "com/mparticle/MParticle$EventType.class",
            "com/mparticle/rokt/RoktSession.class",
            "com.mparticle.identity.IdentityApi",
            "com/mparticle/internal/MPUtility$AdIdInfo.class",
        ):
            with self.subTest(name=name):
                self.assertFalse(report.is_obfuscated_class(name))


class MappingTest(unittest.TestCase):
    MAPPING = """\
com.mparticle.internal.ConfigManager -> com.mparticle.internal.ConfigManager:
# {"id":"sourceFile","fileName":"ConfigManager.java"}
    android.content.Context mContext -> a
    1:5:void refresh():100:104 -> b
    com.mparticle.networking.NetworkOptions getNetworkOptions() -> getNetworkOptions
com.mparticle.internal.MPUtility$AdIdInfo -> com.mparticle.internal.MPUtility$AdIdInfo:
    1:4:void <init>(java.lang.String):210:213 -> <init>
    java.lang.String id -> id
com.mparticle.a -> com.mparticle.b:
    java.lang.String secret -> a
    1:1:void com.mparticle.internal.Helper.inlined():10:10 -> <init>
"""

    def test_only_classes_with_renamed_members_are_listed(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            mapping = Path(tmp) / "mapping.txt"
            mapping.write_text(self.MAPPING, encoding="utf-8")
            renamed = report.load_renamed_members(mapping)
        self.assertEqual(renamed["com.mparticle.internal.ConfigManager"], {"a", "b"})
        self.assertNotIn("com.mparticle.internal.MPUtility$AdIdInfo", renamed)
        self.assertEqual(renamed["com.mparticle.b"], {"a"})
        self.assertNotIn("com.mparticle.a", renamed)

    def test_missing_mapping_means_no_filtering(self) -> None:
        self.assertEqual(report.load_renamed_members(None), {})


class EvaluateReportTest(unittest.TestCase):
    XML = """\
<japicmp accessModifier="PROTECTED" onlyModifications="true">
<classes>
<class fullyQualifiedName="com.mparticle.internal.MPUtility$AdIdInfo" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges/>
  <fields>
    <field name="id" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="FIELD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </field>
  </fields>
  <methods/><constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.internal.ConfigManager" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges/>
  <fields/>
  <methods>
    <method name="b" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </method>
    <method name="c" changeStatus="NEW" binaryCompatible="true" sourceCompatible="true"><compatibilityChanges/></method>
    <method name="z" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </method>
    <method name="getNetworkOptions" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="METHOD_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </method>
  </methods>
  <constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.identity.a" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges><compatibilityChange type="CLASS_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
  <fields/><methods/><constructors/><interfaces/>
</class>
<class fullyQualifiedName="com.mparticle.MParticle" changeStatus="MODIFIED" binaryCompatible="false" sourceCompatible="false">
  <compatibilityChanges/>
  <interfaces>
    <interface fullyQualifiedName="com.mparticle.h0" changeStatus="REMOVED" binaryCompatible="false" sourceCompatible="false">
      <compatibilityChanges><compatibilityChange type="INTERFACE_REMOVED" binaryCompatible="false" sourceCompatible="false"/></compatibilityChanges>
    </interface>
  </interfaces>
  <fields/><methods/><constructors/>
</class>
<class fullyQualifiedName="com.mparticle.rokt.RoktSession" changeStatus="NEW" binaryCompatible="true" sourceCompatible="true">
  <compatibilityChanges/><fields/><methods/><constructors/><interfaces/>
</class>
</classes>
</japicmp>
"""

    def _evaluate(self, renamed):
        with tempfile.TemporaryDirectory() as tmp:
            xml = Path(tmp) / "report.xml"
            xml.write_text(self.XML, encoding="utf-8")
            return report.evaluate_report(xml, renamed)

    def test_real_short_named_member_of_fully_kept_class_is_reported(self) -> None:
        findings, _ = self._evaluate({"com.mparticle.internal.ConfigManager": {"a", "b"}})
        self.assertIn("com.mparticle.internal.MPUtility$AdIdInfo#id: FIELD_REMOVED", [str(f) for f in findings])

    def test_r8_renamed_member_of_partially_kept_class_is_ignored(self) -> None:
        findings, _ = self._evaluate({"com.mparticle.internal.ConfigManager": {"a", "b"}})
        rendered = [str(f) for f in findings]
        self.assertNotIn("com.mparticle.internal.ConfigManager#b: METHOD_REMOVED", rendered)
        # The old side has no mapping, so an R8-shaped name that is not in the new mapping is
        # ignored too; this is the documented residual risk on partially kept classes.
        self.assertNotIn("com.mparticle.internal.ConfigManager#z: METHOD_REMOVED", rendered)
        self.assertIn("com.mparticle.internal.ConfigManager#getNetworkOptions: METHOD_REMOVED", rendered)

    def test_without_mapping_every_member_is_compared(self) -> None:
        findings, _ = self._evaluate({})
        rendered = [str(f) for f in findings]
        self.assertIn("com.mparticle.internal.ConfigManager#b: METHOD_REMOVED", rendered)
        self.assertIn("com.mparticle.internal.ConfigManager#z: METHOD_REMOVED", rendered)

    def test_obfuscated_class_and_obfuscated_interface_are_ignored(self) -> None:
        findings, additions = self._evaluate({})
        rendered = [str(f) for f in findings]
        self.assertFalse(any(f.startswith("com.mparticle.identity.a") for f in rendered))
        self.assertFalse(any(f.startswith("com.mparticle.MParticle") for f in rendered))
        self.assertEqual(additions, 1)


if __name__ == "__main__":
    unittest.main()
