#!/usr/bin/env python3
"""Validate security- and integration-critical declarations in AGP's merged manifest."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = Path("app/build/intermediates")

candidates = list(ROOT.glob("**/merged_manifest/**/AndroidManifest.xml"))
if not candidates:
    candidates = list(ROOT.glob("**/merged_manifests/**/AndroidManifest.xml"))
if not candidates:
    print("ERROR: AGP merged AndroidManifest.xml not found under app/build/intermediates", file=sys.stderr)
    sys.exit(2)

manifest_path = max(candidates, key=lambda p: p.stat().st_mtime)
root = ET.parse(manifest_path).getroot()
application = root.find("application")
if application is None:
    raise SystemExit("ERROR: merged manifest has no <application>")

def attr(element, name):
    return element.get(ANDROID + name)

def require(condition, message):
    if not condition:
        raise SystemExit("ERROR: merged manifest check failed: " + message)
    print("PASS: " + message)

permissions = {node.get(ANDROID + "name"): node for node in root.findall("uses-permission")}
require("android.permission.INTERNET" in permissions, "INTERNET permission retained")

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

require(attr(application, "label") == "Aurelia Pay Bridge", "application label is correct")
require(attr(application, "allowBackup") == "false", "allowBackup remains disabled")
require(attr(application, "usesCleartextTraffic") == "false", "cleartext traffic remains disabled")
require(bool(attr(application, "theme")), "application theme retained")

services = application.findall("service")
bridge_services = [
    service for service in services
    if (attr(service, "name") or "").endswith("PayBridgeService")
]
require(len(bridge_services) == 1, "exactly one PayBridgeService retained")
service = bridge_services[0]
require(attr(service, "exported") == "true", "Bridge service remains explicitly exported")
require(attr(service, "permission") == bridge_permission, "Bridge service retains signature permission")

queries = root.find("queries")
query_packages = set()
if queries is not None:
    query_packages = {
        node.get(ANDROID + "name")
        for node in queries.findall("package")
    }
require("com.aurelia.pms" in query_packages, "PMS package visibility query retained")

activities = application.findall("activity")
launcher = any(
    attr(activity, "name") and attr(activity, "exported") == "true"
    and any(
        action.get(ANDROID + "name") == "android.intent.action.MAIN"
        for intent_filter in activity.findall("intent-filter")
        for action in intent_filter.findall("action")
    )
    for activity in activities
)
require(launcher, "exported launcher activity retained")
print(f"Validated merged manifest: {manifest_path}")
