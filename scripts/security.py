#!/usr/bin/env python3
"""Fail closed: scan publishable files and all local Git history without printing secrets."""
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def gitleaks_binary():
    configured = os.environ.get("GITLEAKS_BIN")
    candidates = [configured] if configured else [shutil.which("gitleaks"), str(Path.home() / ".local/bin/gitleaks")]
    for candidate in candidates:
        if candidate and Path(candidate).is_file() and os.access(candidate, os.X_OK):
            return candidate
    raise RuntimeError("Gitleaks is required. Install it, or set GITLEAKS_BIN. Push blocked.")

def forbidden_path(name):
    path = Path(name)
    lower = path.name.lower()
    return (
        lower in {"local.properties", "credentials.json", "secrets.properties", "known_hosts", "id_rsa", "id_ed25519", "id_ecdsa"}
        or lower == ".env" or lower.startswith(".env.")
        or lower.endswith((".pem", ".key", ".p12", ".pfx", ".jks", ".keystore"))
        or any(part in {".ssh", ".gradle", "build", ".security-reports"} for part in path.parts)
    )

def publishable_paths(root=ROOT):
    data = subprocess.check_output(["git", "ls-files", "--cached", "--others", "--exclude-standard", "-z"], cwd=root)
    paths = sorted(set(os.fsdecode(name) for name in data.split(b"\0") if name))
    # Deleted files are not part of the next snapshot; history is scanned separately.
    return [name for name in paths if (root / name).exists() or (root / name).is_symlink()]

def run_scanner(binary, arguments, config, ignore_directory, report):
    command = [binary, *arguments, "--config", str(config), "--redact=100", "--no-banner", "--no-color",
               "--ignore-gitleaks-allow", "--gitleaks-ignore-path", str(ignore_directory),
               "--report-format", "json", "--report-path", str(report)]
    result = subprocess.run(command, cwd=ROOT, check=False)
    if result.returncode:
        raise RuntimeError("Gitleaks found a problem or could not complete the scan. Push blocked. See the local redacted report.")

def scan(root=ROOT):
    binary = gitleaks_binary()
    config = root / ".gitleaks.toml"
    reports = root / ".security-reports"
    reports.mkdir(exist_ok=True)
    names = publishable_paths(root)
    blocked = [name for name in names if forbidden_path(name)]
    if blocked:
        raise RuntimeError("Forbidden publishable file paths: " + ", ".join(blocked))
    with tempfile.TemporaryDirectory(prefix="android-lab-security-") as temporary:
        base = Path(temporary)
        snapshot = base / "snapshot"
        snapshot.mkdir()
        empty_ignores = base / "no-project-ignore-list"
        empty_ignores.mkdir()
        for name in names:
            source = root / name
            if not source.resolve().is_relative_to(root.resolve()):
                raise RuntimeError("A publishable symlink leaves the repository: " + name)
            destination = snapshot / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
        run_scanner(binary, ["dir", str(snapshot)], config, empty_ignores, reports / "files.json")
        head = subprocess.run(["git", "rev-parse", "--verify", "HEAD"], cwd=root, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if head.returncode == 0:
            run_scanner(binary, ["git", str(root), "--log-opts=--all --full-history"], config, empty_ignores, reports / "history.json")
    print("Gitleaks: publishable files and available Git history are clean.", flush=True)

if __name__ == "__main__":
    try:
        scan()
    except (RuntimeError, OSError, subprocess.SubprocessError) as error:
        raise SystemExit(str(error))
