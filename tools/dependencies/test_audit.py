"""Focused negative checks for the distribution-evidence boundary."""
import io
import json
from pathlib import Path
import tempfile
import tarfile
import unittest
from unittest.mock import patch
import zipfile

import audit


class AuditTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.addCleanup(self.temp.cleanup)

    def apk(self, extra=None, changed=False, duplicate=False):
        payload = io.BytesIO()
        with tarfile.open(fileobj=payload, mode='w:gz') as t:
            data = b'\x7fELFchanged' if changed else b'\x7fELFfixture'
            m = tarfile.TarInfo('package/native'); m.size = len(data)
            t.addfile(m, io.BytesIO(data))
            if duplicate:
                t.addfile(m, io.BytesIO(data))
        path = self.root / 'fixture.apk'
        with zipfile.ZipFile(path, 'w') as z:
            z.writestr('assets/package.dat', payload.getvalue())
            if extra:
                z.writestr(extra, b'\x7fELFhidden')
        return path

    def lock(self):
        return {'native_files': [dict(path='bootstrap:package/native', bytes=11,
                                     sha256=audit.sha(b'\x7fELFfixture'), status='unresolved-package-build')]}

    def test_inventory_does_not_mean_source_clearance(self):
        self.assertEqual(audit.verify_native(self.apk(), self.lock()), {'unresolved-package-build': 1})

    def test_changed_native_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'hash mismatch'):
            audit.verify_native(self.apk(changed=True), self.lock())

    def test_unlisted_native_outside_lib_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'inventory changed'):
            audit.verify_native(self.apk(extra='assets/surprise'), self.lock())

    def test_duplicate_bootstrap_entry_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Duplicate bootstrap'):
            audit.verify_native(self.apk(duplicate=True), self.lock())

    def test_material_path_cannot_escape(self):
        for name in ['../secret', '/absolute', 'C:/secret', 'a/../../b', 'a\\b']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                audit.local_path(self.root, name)

    def test_corrupt_cached_source_is_preserved_and_no_network(self):
        path = self.root / 'input'; path.write_bytes(b'corrupt')
        record = dict(path='input', bytes=2, sha256=audit.sha(b'ok'), url='https://example.invalid/source')
        with patch('urllib.request.urlopen') as network, self.assertRaises(ValueError):
            audit.download(record, path)
        network.assert_not_called()
        self.assertEqual(path.read_bytes(), b'corrupt')

    def test_offline_missing_source_never_fetches(self):
        with patch('urllib.request.urlopen') as network, self.assertRaisesRegex(ValueError, 'Missing cached'):
            audit.download({}, self.root / 'missing', offline=True)
        network.assert_not_called()

    def test_truncated_debian_archive_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Broken ar header'):
            list(audit.ar_members(b'!<arch>\nshort'))

    def test_duplicate_manifest_records_are_rejected(self):
        lock = audit.load_lock()
        lock['materials'].append(lock['materials'][0])
        path = self.root / 'lock.json'; path.write_text(json.dumps(lock))
        with self.assertRaisesRegex(ValueError, 'Duplicate materials'):
            audit.load_lock(path)

    def test_bundle_excludes_adjacent_private_files_and_is_deterministic(self):
        lock = audit.load_lock()
        data = b'locked source fixture'
        lock['materials'] = [dict(path='source/archive.tar', bytes=len(data), sha256=audit.sha(data))]
        path = self.root / 'source/archive.tar'; path.parent.mkdir(); path.write_bytes(data)
        (self.root / 'pico8_64').write_bytes(b'must not be included')
        first, second = self.root / 'one.zip', self.root / 'two.zip'
        self.assertEqual(audit.bundle(lock, self.root, first), audit.bundle(lock, self.root, second))
        with zipfile.ZipFile(first) as z:
            self.assertEqual(set(z.namelist()), {'native-source-lock.json', 'NATIVE-NOTICES.txt',
                                               'README.md', 'sources/source/archive.tar'})
        with self.assertRaisesRegex(ValueError, 'already exists'):
            audit.bundle(lock, self.root, first)


if __name__ == '__main__':
    unittest.main()
