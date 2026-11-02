import io
import tarfile
import unittest

import audit
import runtime_support


class SupportRefreshTests(unittest.TestCase):
    def fixture(self):
        raw = io.BytesIO()
        with tarfile.open(fileobj=raw, mode='w:gz') as t:
            for name, data in [('package/native', b'old'), ('package/preserve', b'untouched')]:
                m = tarfile.TarInfo(name); m.size = len(data)
                t.addfile(m, io.BytesIO(data))
            m = tarfile.TarInfo('package/link'); m.type = tarfile.SYMTYPE; m.linkname = 'native'
            t.addfile(m)
        original = raw.getvalue()
        manifest = dict(original_bootstrap=dict(bytes=len(original), sha256=audit.sha(original)), files=[
            dict(path='package/native', bytes=3, sha256=audit.sha(b'new'), previous_sha256=audit.sha(b'old')),
            dict(path='package/added', bytes=3, sha256=audit.sha(b'add'))])
        return original, manifest, {'package/native': b'new', 'package/added': b'add'}

    def test_only_allowlisted_files_change_and_output_is_deterministic(self):
        original, manifest, files = self.fixture()
        result = runtime_support.compose(original, manifest, files)
        self.assertEqual(result, runtime_support.compose(original, manifest, files))
        with tarfile.open(fileobj=io.BytesIO(result)) as t:
            self.assertEqual(t.extractfile('package/native').read(), b'new')
            self.assertEqual(t.extractfile('package/added').read(), b'add')
            self.assertEqual(t.extractfile('package/preserve').read(), b'untouched')
            self.assertEqual(t.getmember('package/link').linkname, 'native')

    def test_changed_original_and_replacement_are_rejected(self):
        original, manifest, files = self.fixture()
        for data, replacements in [(original + b'x', files), (original, dict(files, **{'package/native': b'bad'}))]:
            with self.assertRaisesRegex(ValueError, 'hash mismatch'):
                runtime_support.compose(data, manifest, replacements)
        manifest['files'][0]['previous_sha256'] = '0' * 64
        with self.assertRaisesRegex(ValueError, 'Original support file changed'):
            runtime_support.compose(original, manifest, files)

    def test_missing_or_duplicate_inventory_fails_closed(self):
        original, manifest, files = self.fixture()
        with self.assertRaisesRegex(ValueError, 'incomplete'):
            runtime_support.compose(original, manifest, {})
        manifest['files'].append(manifest['files'][0])
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            runtime_support.compose(original, manifest, files)


if __name__ == '__main__':
    unittest.main()
