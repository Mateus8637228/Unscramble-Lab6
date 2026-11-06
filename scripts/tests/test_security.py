import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import security
import pre_push

class PathAndPushTests(unittest.TestCase):
    def test_blocks_credentials_and_machine_files(self):
        for name in ["local.properties", ".env", ".env.production", "private.pem", "upload.jks", ".ssh/mac", "build/generated/file"]:
            with self.subTest(name=name):
                self.assertTrue(security.forbidden_path(name))

    def test_allows_sources_resources_and_wrapper(self):
        for name in ["app/src/main/MainActivity.kt", "app/src/main/res/drawable/image.jpg", "gradle/wrapper/gradle-wrapper.jar"]:
            self.assertFalse(security.forbidden_path(name))

    def test_missing_gitleaks_blocks_instead_of_skipping(self):
        with patch.dict("os.environ", {"GITLEAKS_BIN": "/does-not-exist/gitleaks"}):
            with self.assertRaises(RuntimeError):
                security.gitleaks_binary()

    def test_dirty_tree_blocks_push(self):
        with patch("subprocess.check_output", return_value=b" M app/source.kt"):
            with self.assertRaises(RuntimeError):
                pre_push.require_clean()

    def test_only_tested_head_branch_is_allowed(self):
        head = "a" * 40
        pre_push.validate_refs([f"refs/heads/main {head} refs/heads/main {'0' * 40}"], head)
        with self.assertRaises(RuntimeError):
            pre_push.validate_refs([f"refs/heads/other {'b' * 40} refs/heads/other {'0' * 40}"], head)

    def test_nonzero_subprocess_result_blocks_push(self):
        with patch("subprocess.run", side_effect=subprocess.CalledProcessError(1, "gradlew")):
            with self.assertRaises(subprocess.CalledProcessError):
                pre_push.checked_run(["gradlew"], {})

    def test_unscramble_requires_coverage_and_device_tests(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source = root / "app/src/main/java/com/example/unscramble/ui/GameViewModel.kt"
            source.parent.mkdir(parents=True)
            source.touch()
            tasks = pre_push.gradle_tasks(root)
            self.assertIn(":app:verifyGameViewModelCoverage", tasks)
            self.assertIn(":app:connectedDebugAndroidTest", tasks)

class ScannerIntegrationTests(unittest.TestCase):
    def run_scan(self, text):
        with tempfile.TemporaryDirectory() as temporary:
            base = Path(temporary)
            snapshot = base / "snapshot"
            snapshot.mkdir()
            (snapshot / "example.txt").write_text(text)
            return subprocess.run([
                security.gitleaks_binary(), "dir", str(snapshot), "--config", str(security.ROOT / ".gitleaks.toml"),
                "--redact=100", "--no-banner", "--no-color", "--ignore-gitleaks-allow",
                "--gitleaks-ignore-path", str(base / "absent-ignore"),
            ], capture_output=True).returncode

    def test_clean_source_is_accepted(self):
        self.assertEqual(0, self.run_scan("class MainActivity\n"))

    def test_synthetic_api_token_is_rejected_even_with_allow_comment(self):
        # Generated test-only sentinel, never a usable credential or a committed token.
        sentinel = "gh" + "p_" + "Ab3Cd4Ef5Gh6Ij7Kl8Mn9Op0Qr1St2Uv3Wx4"
        self.assertEqual(1, self.run_scan('api_key = "' + sentinel + '" # gitleaks:allow\n'))

    def test_literal_ip_address_is_rejected(self):
        address = ".".join(map(str, [192, 0, 2, 45]))
        self.assertEqual(1, self.run_scan("server=" + address))

    def test_private_key_header_is_rejected(self):
        header = "-----" + "BEGIN RSA PRIVATE KEY" + "-----"
        footer = "-----" + "END RSA PRIVATE KEY" + "-----"
        self.assertEqual(1, self.run_scan(header + "\n" + "QUJD" * 30 + "\n" + footer))

if __name__ == "__main__":
    unittest.main()
