import importlib.util
import io
from pathlib import Path
import tarfile
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location("offhost_backup", Path(__file__).with_name("offhost-backup.py"))
BACKUP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BACKUP)
BUNDLE = "full_backup_20261109T075229Z_p6JYSe"
STAMP = "20261109T075229Z"


class OffhostBackupTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.destination = Path(temp.name)
        self.source = self.destination / BUNDLE
        self.source.mkdir()
        self.dump = self.source / f"sugang_helper_{STAMP}.sql.gz"
        self.dump.write_bytes(b"fixture SQL")
        uploads = self.source / f"uploads_{STAMP}.tar.gz"
        with tarfile.open(uploads, "w:gz") as archive:
            image = tarfile.TarInfo("fixture.png")
            image.size = 7
            archive.addfile(image, io.BytesIO(b"fixture"))
        manifest = self.source / "backup.manifest"
        manifest.write_text(
            f"schema=full-backup-v1\ncreated_at_utc={STAMP}\ndatabase=sugang_helper\n"
            f"dump_file={self.dump.name}\nuploads=included\nuploads_file={uploads.name}\n"
        )
        for path in (self.dump, uploads, manifest):
            BACKUP.checksum_path(path).write_text(f"{BACKUP.digest(path)}  {path.name}\n")

    def test_verified_bundle_is_published_and_acknowledged_once_per_run(self):
        # 준비, 실행, 검증
        with patch.object(BACKUP, "latest_bundle", return_value=BUNDLE), \
                patch.object(BACKUP, "download_bundle", side_effect=self.download) as download, \
                patch.object(BACKUP, "acknowledge") as acknowledge:
            BACKUP.sync(self.destination)
            BACKUP.sync(self.destination)
        archive = self.destination / f"{BUNDLE}.tar.gz"
        BACKUP.verify_checksum(archive)
        with tarfile.open(archive, "r:gz") as contents:
            self.assertEqual(contents.extractfile(f"{BUNDLE}/{self.dump.name}").read(), b"fixture SQL")
        self.assertEqual(download.call_count, 1)
        self.assertEqual(acknowledge.call_count, 2)
        acknowledge.assert_called_with(STAMP)

    def test_corrupt_input_is_neither_published_nor_acknowledged(self):
        self.dump.write_bytes(b"corrupt")
        with patch.object(BACKUP, "latest_bundle", return_value=BUNDLE), \
                patch.object(BACKUP, "download_bundle", side_effect=self.download), \
                patch.object(BACKUP, "acknowledge") as acknowledge:
            with self.assertRaises(ValueError):
                BACKUP.sync(self.destination)
        self.assertFalse((self.destination / f"{BUNDLE}.tar.gz").exists())
        acknowledge.assert_not_called()

    def test_corrupt_local_archive_is_replaced_with_verified_copy(self):
        archive = self.destination / f"{BUNDLE}.tar.gz"
        archive.write_bytes(b"corrupt")
        BACKUP.checksum_path(archive).write_text(f"{'0' * 64}  {archive.name}\n")
        with patch.object(BACKUP, "latest_bundle", return_value=BUNDLE), \
                patch.object(BACKUP, "download_bundle", side_effect=self.download), \
                patch.object(BACKUP, "acknowledge") as acknowledge:
            BACKUP.sync(self.destination)
        BACKUP.verify_checksum(archive)
        acknowledge.assert_called_once_with(STAMP)

    def test_rotation_keeps_three_verified_exports_and_preserves_invalid_entries(self):
        for suffix in ("000001", "000002", "000003", "000004"):
            self.download(BUNDLE[: -6] + suffix, self.destination)
        invalid = self.destination / "full_backup_20261109T075229Z_000005"
        invalid.mkdir()

        BACKUP.prune_exports(self.destination)

        self.assertEqual({path.name[-6:] for path in self.destination.iterdir()},
                         {BUNDLE[-6:], "000004", "000003", invalid.name[-6:]})

    def download(self, bundle, stage):
        for path in self.source.iterdir():
            target = stage / bundle / path.name
            target.parent.mkdir(exist_ok=True)
            target.write_bytes(path.read_bytes())


if __name__ == "__main__":
    unittest.main()
