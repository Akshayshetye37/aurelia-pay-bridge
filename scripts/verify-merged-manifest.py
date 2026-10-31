#!/usr/bin/env python3
"""Validate security- and integration-critical declarations in AGP's merged manifest."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = Path("app/build/intermediates")


def fail(message: str) -> None:
    raise SystemExit(f"ERROR: merged manifest check failed: {message}")


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)
    print(f"PASS: {message}")


def attr(element: ET.Element, name: str):
    return element.get(ANDROID + name)


def main() -> int:
    candidates = list(ROOT.glob("**/merged_manifest/**/AndroidManifest.xml"))
    if not candidates:
        candidates = list(ROOT.glob("**/merged_manifests/**/AndroidManifest.xml"))
    if not candidates:
        print(
            "ERROR: AGP merged AndroidManifest.xml not found under app/build/intermediates",
            file=sys.stderr,
        )
        return 2

    # AGP may retain variant-specific outputs. This CI invocation explicitly
    # processes the debug main manifest; prefer that path to avoid validating an
    # unrelated stale/release intermediate left in a reused workspace.
    candidates.sort(
        key=lambda path: (
            "debug" in path.as_posix().lower(),
            path.stat().st_mtime,
        ),
        reverse=True,
    )
    manifest_path = candidates[0]
    try:
        root = ET.parse(manifest_path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"cannot parse merged manifest {manifest_path}: {exc}")

    application = root.find("application")
    require(application is not None, "application element retained")
    assert application is not None

    permissions = {
        node.get(ANDROID + "name"): node
        for node in root.findall("uses-permission")
    }
    require(
        "android.permission.INTERNET" in permissions,
        "INTERNET permission retained",
    )

    defined = {
        node.get(ANDROID + "name"): node
        for node in root.findall("permission")
    }
    bridge_permission = "com.aurelia.paybridge.permission.BIND_PAY_BRIDGE"
    require(bridge_permission in defined, "Bridge IPC permission retained")
    require(
        attr(defined[bridge_permission], "protectionLevel") == "signature",
        "Bridge IPC permission remains signature-protected",
    )

    require(
        attr(application, "label") == "Aurelia Pay Bridge",
        "application label is correct",
    )
    require(
        attr(application, "allowBackup") == "false",
        "allowBackup remains disabled",
    )
    require(
        attr(application, "usesCleartextTraffic") == "false",
        "cleartext traffic remains disabled",
    )
    require(bool(attr(application, "theme")), "application theme retained")

    bridge_services = [
        service
        for service in application.findall("service")
        if (attr(service, "name") or "").endswith("PayBridgeService")
    ]
    require(len(bridge_services) == 1, "exactly one PayBridgeService retained")
    service = bridge_services[0]
    require(
        attr(service, "exported") == "true",
        "Bridge service remains explicitly exported",
    )
    require(
        attr(service, "permission") == bridge_permission,
        "Bridge service retains signature permission",
    )

    queries = root.find("queries")
    query_packages = (
        {
            node.get(ANDROID + "name")
            for node in queries.findall("package")
        }
        if queries is not None
        else set()
    )
    require(
        "com.aurelia.pms" in query_packages,
        "PMS package visibility query retained",
    )

    launcher = any(
        attr(activity, "name")
        and attr(activity, "exported") == "true"
        and any(
            action.get(ANDROID + "name") == "android.intent.action.MAIN"
            for intent_filter in activity.findall("intent-filter")
            for action in intent_filter.findall("action")
        )
        for activity in application.findall("activity")
    )
    require(launcher, "exported launcher activity retained")
    print(f"Validated merged manifest: {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
