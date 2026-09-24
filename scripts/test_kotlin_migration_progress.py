"""Unit tests for scripts/kotlin_migration_progress.py.

Run with ``python3 -m unittest scripts/test_kotlin_migration_progress.py``.
"""

from __future__ import annotations

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

_SPEC = importlib.util.spec_from_file_location(
    "kotlin_migration_progress", Path(__file__).resolve().parent / "kotlin_migration_progress.py"
)
progress = importlib.util.module_from_spec(_SPEC)
sys.modules[_SPEC.name] = progress
_SPEC.loader.exec_module(progress)


def _write(root: Path, relative: str, lines: int) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(f"line {i}" for i in range(lines)) + "\n", encoding="utf-8")


class MeasureTest(unittest.TestCase):
    def setUp(self) -> None:
        self._tmp = tempfile.TemporaryDirectory()
        self.root = Path(self._tmp.name)
        _write(self.root, "android-core/src/main/java/com/mparticle/MParticle.java", 100)
        _write(self.root, "android-core/src/main/java/com/mparticle/internal/ConfigManager.java", 40)
        _write(self.root, "android-core/src/main/kotlin/com/mparticle/internal/Constants.kt", 30)
        _write(self.root, "android-core/src/main/java/com/mparticle/package-info.java", 3)
        _write(self.root, "android-core/src/test/java/com/mparticle/Ignored.java", 500)
        _write(self.root, "android-kit-base/src/main/java/com/mparticle/kits/KitIntegration.java", 50)
        _write(self.root, "android-kit-base/src/main/kotlin/com/mparticle/kits/KitOptions.kt", 20)
        _write(
            self.root,
            "scripts/kotlin-migration-facades.txt",
            0,
        )
        (self.root / "scripts/kotlin-migration-facades.txt").write_text(
            "# comment\n"
            "android-core/src/main/java/com/mparticle/MParticle.java\n"
            "*/package-info.java\n"
            "android-kit-base/src/main/java/com/mparticle/kits/KitIntegration.java\n",
            encoding="utf-8",
        )

    def tearDown(self) -> None:
        self._tmp.cleanup()

    def test_counts_only_main_source_sets(self) -> None:
        facades = progress.load_facades(self.root / "scripts/kotlin-migration-facades.txt")
        results = progress.measure(self.root, facades)
        core, kit = results
        self.assertEqual(core.java_loc, 143)
        self.assertEqual(core.kotlin_loc, 30)
        self.assertEqual(core.java_files, 3)
        self.assertEqual(kit.java_loc, 50)
        self.assertEqual(kit.kotlin_loc, 20)

    def test_facade_and_package_info_are_excluded_from_java_left(self) -> None:
        facades = progress.load_facades(self.root / "scripts/kotlin-migration-facades.txt")
        core, kit = progress.measure(self.root, facades)
        self.assertEqual(core.facade_loc, 103)
        self.assertEqual(core.java_left, 40)
        self.assertEqual(kit.facade_loc, 50)
        self.assertEqual(kit.java_left, 0)

    def test_totals_and_renderers(self) -> None:
        facades = progress.load_facades(self.root / "scripts/kotlin-migration-facades.txt")
        results = progress.measure(self.root, facades)
        total = progress.totals(results)
        self.assertEqual(total.java_left, 40)
        self.assertAlmostEqual(total.kotlin_share, 100.0 * 50 / 243, places=3)
        env = dict(line.split("=", 1) for line in progress.render_env(results).splitlines())
        self.assertEqual(env["JAVA_LEFT_LOC"], "40")
        self.assertEqual(env["KOTLIN_FILES"], "2")
        markdown = progress.render_markdown(results)
        self.assertIn("| `android-core` | 30 | 143 | 103 | **40** |", markdown)
        self.assertIn("Conversion progress:", markdown)

    def test_conversion_progress_is_clamped(self) -> None:
        self.assertEqual(progress.conversion_progress(progress.BASELINE_JAVA_LEFT), 0.0)
        self.assertEqual(progress.conversion_progress(0), 100.0)
        self.assertEqual(progress.conversion_progress(progress.BASELINE_JAVA_LEFT * 2), 0.0)

    def test_missing_facade_list_counts_everything_as_convertible(self) -> None:
        core, _ = progress.measure(self.root, progress.load_facades(self.root / "missing.txt"))
        self.assertEqual(core.java_left, core.java_loc)

    def test_missing_module_directory_is_an_error_not_a_zero(self) -> None:
        with tempfile.TemporaryDirectory() as empty:
            with self.assertRaises(FileNotFoundError):
                progress.measure(Path(empty), [])
            self.assertEqual(progress.main(["--root", empty, "--format", "env"]), 1)


if __name__ == "__main__":
    unittest.main()
