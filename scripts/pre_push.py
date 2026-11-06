#!/usr/bin/env python3
"""Mandatory pre-push gate, also used when Push is clicked in Fork."""
import os
import subprocess
import sys
from pathlib import Path

from security import ROOT, scan

def require_clean(root=ROOT):
    if subprocess.check_output(["git", "status", "--porcelain", "--untracked-files=all"], cwd=root).strip():
        raise RuntimeError("Commit all publishable changes before pushing. Tests must match the pushed commit.")

def validate_refs(lines, head):
    for line in lines:
        if not line.strip():
            continue
        fields = line.split()
        if len(fields) != 4 or fields[1] != head or not fields[0].startswith("refs/heads/"):
            raise RuntimeError("Only the checked-out, tested HEAD branch may be pushed by this gate.")

def gradle_tasks(root=ROOT):
    tasks = [":app:assembleDebug", ":app:testDebugUnitTest"]
    if (root / "app/src/main/java/com/example/unscramble/ui/GameViewModel.kt").exists():
        tasks.append(":app:verifyGameViewModelCoverage")
    if (root / "scripts/check_lifecycle.py").exists():
        return tasks
    return tasks + [":app:connectedDebugAndroidTest"]

def checked_run(command, environment):
    subprocess.run(command, cwd=ROOT, env=environment, check=True)

def main():
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    validate_refs(sys.stdin.readlines() if not sys.stdin.isatty() else [], head)
    require_clean()
    scan()
    checked_run([sys.executable, "-m", "unittest", "discover", "-s", "scripts/tests", "-v"], os.environ.copy())
    environment = os.environ.copy()
    studio = Path("/Applications/Android Studio.app/Contents/jbr/Contents/Home")
    if not environment.get("JAVA_HOME") and studio.exists():
        environment["JAVA_HOME"] = str(studio)
    sdk = Path.home() / "Library/Android/sdk"
    if not environment.get("ANDROID_HOME") and sdk.exists():
        environment["ANDROID_HOME"] = str(sdk)
    checked_run(["./gradlew", *gradle_tasks(), "--rerun-tasks", "--console=plain"], environment)
    if (ROOT / "scripts/check_lifecycle.py").exists():
        checked_run([sys.executable, "scripts/check_lifecycle.py"], environment)
    require_clean()
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip() != head:
        raise RuntimeError("HEAD changed during verification. Push blocked.")
    print("Pre-push passed: secrets, guard tests, build, unit tests and device scenarios.", flush=True)

if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError) as error:
        raise SystemExit(str(error))
