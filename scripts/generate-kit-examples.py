#!/usr/bin/env python3
"""Generate Kotlin example apps for all kits in settings-kits.gradle."""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

KITS = [
    ("adjust", "adjust-5"),
    ("adobe", "adobe-5"),
    ("adobemedia", "adobemedia-5"),
    ("appsflyer", "appsflyer-6"),
    ("apptentive", "apptentive-6"),
    ("apptimize", "apptimize-3"),
    ("braze", "braze-38"),
    ("braze", "braze-39"),
    ("braze", "braze-40"),
    ("braze", "braze-41"),
    ("branch", "branch-5"),
    ("clevertap", "clevertap-7"),
    ("comscore", "comscore-6"),
    ("ga", "ga-23"),
    ("ga4", "ga4-23"),
    ("iterable", "iterable-3"),
    ("kochava", "kochava-5"),
    ("localytics", "localytics-6"),
    ("leanplum", "leanplum-7"),
    ("onetrust", "onetrust"),
    ("optimizely", "optimizely-3"),
    ("radar", "radar-3"),
    ("rokt", "rokt"),
    ("singular", "singular-12"),
]

SKIP_EXAMPLE_BUILDS = {
    # Custom Maven repos / transitive resolution from example apps
    "apptimize-3",
    "localytics-6",
    "singular-12",
    # Rokt pulls Compose artifacts that require AGP 8.6+ (repo uses 8.3.2)
    "rokt",
}

DISPLAY_NAMES = {
    "adjust": "Adjust",
    "adobe": "Adobe",
    "adobemedia": "Adobe Media",
    "appsflyer": "AppsFlyer",
    "apptentive": "Apptentive",
    "apptimize": "Apptimize",
    "braze": "Braze",
    "branch": "Branch",
    "clevertap": "CleverTap",
    "comscore": "Comscore",
    "ga": "GA",
    "ga4": "GA4",
    "iterable": "Iterable",
    "kochava": "Kochava",
    "localytics": "Localytics",
    "leanplum": "Leanplum",
    "onetrust": "OneTrust",
    "optimizely": "Optimizely",
    "radar": "Radar",
    "rokt": "Rokt",
    "singular": "Singular",
}

IC_LAUNCHER_FOREGROUND = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#3DDC84"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#00000000"
        android:pathData="M9,0L9,108"
        android:strokeWidth="0.8"
        android:strokeColor="#33FFFFFF" />
</vector>
"""

ADAPTIVE_ICON = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""

ACTIVITY_MAIN = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/app_name" />

</LinearLayout>
"""

ANDROID_MANIFEST = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".ExampleApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
"""


def read_kit_min_sdk(vendor: str, module: str) -> int:
    build_file = ROOT / "kits" / vendor / module / "build.gradle"
    match = re.search(r"minSdk(?:Version)?\s+(\d+)", build_file.read_text())
    return int(match.group(1)) if match else 21


def module_slug(module: str) -> str:
    return re.sub(r"[^a-zA-Z0-9]", "", module)


def package_name(vendor: str, module: str) -> str:
    vendor_modules = [m for v, m in KITS if v == vendor]
    if len(vendor_modules) == 1:
        return f"com.mparticle.kits.{vendor}.example.kotlin"
    return f"com.mparticle.kits.{vendor}.{module_slug(module)}.example.kotlin"


def display_name(vendor: str, module: str) -> str:
    base = DISPLAY_NAMES[vendor]
    vendor_modules = [m for v, m in KITS if v == vendor]
    if len(vendor_modules) == 1:
        return f"{base} Kit Kotlin Example"
    suffix = module.split("-", 1)[-1]
    return f"{base} {suffix} Kit Kotlin Example"


def gradle_project(vendor: str, module: str) -> str:
    return f":kits:{vendor}:{module}"


def android_gradle_project(vendor: str, module: str) -> str:
    return f":kits:android-{vendor}:{module}"


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


def generate_example(vendor: str, module: str) -> None:
    pkg = package_name(vendor, module)
    pkg_path = pkg.replace(".", "/")
    min_sdk = read_kit_min_sdk(vendor, module)
    example_root = ROOT / "kits" / vendor / module / "example" / "example-kotlin"
    project_path = gradle_project(vendor, module)

    build_gradle = f"""plugins {{
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}}

android {{
    namespace '{pkg}'
    compileSdk 35

    defaultConfig {{
        applicationId '{pkg}'
        minSdk {min_sdk}
        targetSdk 35
        versionCode 1
        versionName '1.0'
    }}

    compileOptions {{
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }}

    kotlinOptions {{
        jvmTarget = '17'
    }}

    buildTypes {{
        release {{
            minifyEnabled false
        }}
    }}
}}

dependencies {{
    implementation project('{project_path}')
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
}}
"""

    example_application = f"""package {pkg}

import android.app.Application
import com.mparticle.MPEvent
import com.mparticle.MParticle
import com.mparticle.MParticleOptions

class ExampleApplication : Application() {{
    override fun onCreate() {{
        super.onCreate()
        val options =
            MParticleOptions
                .builder(this)
                .credentials(
                    "REPLACE WITH YOUR MPARTICLE API KEY",
                    "REPLACE WITH YOUR MPARTICLE API SECRET",
                ).logLevel(MParticle.LogLevel.VERBOSE)
                .build()
        MParticle.start(options)
        MParticle.getInstance()?.logEvent(
            MPEvent.Builder("foo", MParticle.EventType.Other).build(),
        )
    }}
}}
"""

    main_activity = f"""package {pkg}

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {{
    override fun onCreate(savedInstanceState: Bundle?) {{
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }}
}}
"""

    strings_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">{display_name(vendor, module)}</string>
</resources>
"""

    colors_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
"""

    write_file(example_root / "build.gradle", build_gradle)
    write_file(example_root / "src/main/AndroidManifest.xml", ANDROID_MANIFEST)
    write_file(example_root / f"src/main/kotlin/{pkg_path}/ExampleApplication.kt", example_application)
    write_file(example_root / f"src/main/kotlin/{pkg_path}/MainActivity.kt", main_activity)
    write_file(example_root / "src/main/res/values/strings.xml", strings_xml)
    write_file(example_root / "src/main/res/values/colors.xml", colors_xml)
    write_file(example_root / "src/main/res/layout/activity_main.xml", ACTIVITY_MAIN)
    write_file(example_root / "src/main/res/drawable/ic_launcher_foreground.xml", IC_LAUNCHER_FOREGROUND)
    write_file(example_root / "src/main/res/mipmap-anydpi-v26/ic_launcher.xml", ADAPTIVE_ICON)
    write_file(example_root / "src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml", ADAPTIVE_ICON)


def generate_settings_kit_examples() -> None:
    lines = [
        "// CI-only settings for kit sample apps. Keeps example modules out of settings-kits.gradle.",
        "apply from: 'settings-kits.gradle'",
        "",
    ]
    includes = []
    project_dirs = []
    for vendor, module in KITS:
        project_name = f"{gradle_project(vendor, module)}:example-kotlin"
        includes.append(f"'{project_name}'")
        project_dirs.append(
            f"project('{project_name}').projectDir = file('kits/{vendor}/{module}/example/example-kotlin')"
        )

    lines.append("include " + ",\n        ".join(includes))
    lines.append("")
    lines.extend(project_dirs)
    write_file(ROOT / "settings-kit-examples.gradle", "\n".join(lines) + "\n")


def generate_matrix() -> None:
    matrix = []
    for vendor, module in KITS:
        entry = {
            "name": module,
            "local_path": f"kits/{vendor}/{module}",
            "kit_project": android_gradle_project(vendor, module),
            "example_kotlin_project": f"{android_gradle_project(vendor, module)}:example-kotlin",
        }
        if module in SKIP_EXAMPLE_BUILDS:
            entry["skip_example_builds"] = True
        matrix.append(entry)
    write_file(ROOT / "kits/matrix.json", json.dumps(matrix, indent=2) + "\n")


def main() -> None:
    for vendor, module in KITS:
        generate_example(vendor, module)
    generate_settings_kit_examples()
    generate_matrix()
    print(f"Generated examples for {len(KITS)} kits")


if __name__ == "__main__":
    main()
