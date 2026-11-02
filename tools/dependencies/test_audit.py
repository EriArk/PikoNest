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
from signed_catalog import verify_index


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

    def test_unknown_native_source_blocks_even_if_gate_list_was_cleared(self):
        lock = self.lock(); lock['open_gates'] = []
        self.assertTrue(audit.incomplete(lock))

    def test_binary_matching_is_not_a_universal_license_requirement(self):
        lock = self.lock(); lock['open_gates'] = []
        lock['native_files'][0]['status'] = 'permissive-notices-reviewed'
        self.assertFalse(audit.incomplete(lock))

    def test_unrecognized_review_status_does_not_clear_completion(self):
        lock = self.lock(); lock['open_gates'] = []
        lock['native_files'][0]['status'] = 'unrecognized-status'
        self.assertTrue(audit.incomplete(lock))

    def test_signature_does_not_replace_index_and_package_hash_checks(self):
        index = b'Package: demo\nVersion: 1\nFilename: pool/demo.deb\nSize: 2\nSHA256: abc\n'
        release = 'Date: historical\nSHA256:\n %s %d main/Packages\n' % (audit.sha(index), len(index))
        evidence = dict(id='demo', package='demo', version='1', catalog_filename='pool/demo.deb', bytes=2, sha256='abc')
        self.assertEqual(verify_index(release, index, 'main/Packages', [evidence]), 'historical')
        with self.assertRaisesRegex(ValueError, 'index hash/size'):
            verify_index(release, index + b'changed', 'main/Packages', [evidence])
        with self.assertRaisesRegex(ValueError, 'disagrees'):
            verify_index(release, index, 'main/Packages', [dict(evidence, version='2')])

    def test_signed_index_cannot_omit_or_duplicate_package_record(self):
        index = b'Package: demo\nVersion: 1\nFilename: pool/demo.deb\nSize: 2\nSHA256: abc\n'
        evidence = dict(id='demo', package='demo', version='1', catalog_filename='pool/demo.deb', bytes=2, sha256='abc')
        for data in [b'Package: unrelated\n', index + b'\n' + index]:
            release = 'SHA256:\n %s %d main/Packages\n' % (audit.sha(data), len(data))
            with self.assertRaisesRegex(ValueError, 'Missing/duplicate package'):
                verify_index(release, data, 'main/Packages', [evidence])

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
