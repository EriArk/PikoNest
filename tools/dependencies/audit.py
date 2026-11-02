"""Pinned native provenance and source-material checks; never installs or publishes.

Python 3.12+, standard library except zstandard==0.25.0 for .deb evidence.
Source archives are copied as opaque bytes, never extracted or executed.
"""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import tarfile
import tempfile
import urllib.request
import zipfile

REPO = Path(__file__).resolve().parents[2]
LOCK = REPO / 'docs/release/beta1/native-source-lock.json'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def checked(data, record):
    if len(data) != record['bytes'] or sha(data) != record['sha256']:
        raise ValueError('Size/hash mismatch: ' + record.get('path', record.get('id', 'input')))
    return data


def local_path(root, name):
    if not re.fullmatch(r'[A-Za-z0-9_.+/-]+', name):
        raise ValueError('Unsafe material path: ' + name)
    parts = PurePosixPath(name).parts
    if not parts or name.startswith('/') or any(p in ('.', '..') for p in parts):
        raise ValueError('Unsafe material path: ' + name)
    root = root.resolve()
    path = root.joinpath(*parts)
    if not path.resolve().is_relative_to(root):
        raise ValueError('Material path escapes destination')
    current = path
    while current != root:
        if current.is_symlink():
            raise ValueError('Symlink in material destination')
        current = current.parent
    return path


def load_lock(path=LOCK):
    lock = json.loads(path.read_text(encoding='utf-8'))
    if lock['schema'] != 1:
        raise ValueError('Unknown lock schema')
    for collection, key in [('native_files', 'path'), ('evidence', 'id'), ('materials', 'path')]:
        rows = lock[collection]
        if len({r[key] for r in rows}) != len(rows):
            raise ValueError('Duplicate ' + collection)
        for r in rows:
            if not re.fullmatch('[0-9a-f]{64}', r['sha256']) or r['bytes'] < 0:
                raise ValueError('Invalid checksum record')
    ids = {r['id'] for r in lock['evidence']}
    for r in lock['native_files']:
        if r['status'] in {'official-binary-match', 'archived-package-match'} and r.get('evidence_id') not in ids:
            raise ValueError('Missing evidence for ' + r['path'])
    return lock


def apk_native(apk):
    found = {}
    with zipfile.ZipFile(apk) as z:
        if len(set(z.namelist())) != len(z.namelist()):
            raise ValueError('Duplicate APK entries')
        for item in z.infolist():
            if not item.is_dir():
                with z.open(item) as stream:
                    prefix = stream.read(4)
                    if prefix == b'\x7fELF':
                        if item.file_size > 256 * 1024 * 1024:
                            raise ValueError('Oversize APK library')
                        found['apk:' + item.filename] = prefix + stream.read()
        with tarfile.open(fileobj=io.BytesIO(z.read('assets/package.dat'))) as t:
            seen = set()
            total = 0
            for m in t:
                if m.name in seen:
                    raise ValueError('Duplicate bootstrap entry: ' + m.name)
                seen.add(m.name)
                total += m.size
                if len(seen) > 4096 or total > 256 * 1024 * 1024:
                    raise ValueError('Oversize bootstrap')
                if m.isfile():
                    data = t.extractfile(m).read()
                    if data.startswith(b'\x7fELF'):
                        found['bootstrap:' + m.name] = data
    return found


def verify_native(apk, lock):
    found = apk_native(apk)
    expected = {r['path']: r for r in lock['native_files']}
    if found.keys() != expected.keys():
        raise ValueError('Native inventory changed; missing=' + str(sorted(expected.keys() - found.keys()))
                         + '; added=' + str(sorted(found.keys() - expected.keys())))
    for name, data in found.items():
        checked(data, expected[name])
    return Counter(r['status'] for r in expected.values())


def download(record, path, offline=False):
    if path.exists():
        checked(path.read_bytes(), record)
        return path
    if offline:
        raise ValueError('Missing cached material: ' + str(path))
    if not record['url'].startswith('https://'):
        raise ValueError('Only HTTPS downloads are allowed')
    path.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(record['url'], headers={'User-Agent': 'PikoNest-source-audit/1'})
    with urllib.request.urlopen(request, timeout=90) as response:
        if not response.geturl().startswith('https://'):
            raise ValueError('Insecure download redirect')
        data = checked(response.read(record['bytes'] + 1), record)
    # Existing mismatched files are retained. Interrupted downloads are never accepted.
    with tempfile.NamedTemporaryFile(dir=path.parent, delete=False, suffix='.partial') as stream:
        temp = Path(stream.name)
        stream.write(data)
    try:
        if path.exists():
            checked(path.read_bytes(), record)
        else:
            temp.replace(path)
    finally:
        temp.unlink(missing_ok=True)
    return path


def ar_members(data):
    if data[:8] != b'!<arch>\n':
        raise ValueError('Not a Debian ar archive')
    pos = 8
    while pos < len(data):
        header = data[pos:pos + 60]
        if len(header) != 60 or header[58:] != b'`\n':
            raise ValueError('Broken ar header')
        size = int(header[48:58])
        pos += 60
        if size < 0 or pos + size > len(data):
            raise ValueError('Truncated ar member')
        yield header[:16].decode('ascii').strip().rstrip('/'), data[pos:pos + size]
        pos += size + size % 2


def evidence_members(data, kind, wanted, package=None):
    if kind == 'zip-deb':
        if not package:
            raise ValueError('Missing nested package record')
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            if z.namelist().count(package['member']) != 1:
                raise ValueError('Missing/duplicate nested package')
            data = checked(z.read(package['member']), package)
        return evidence_members(data, 'deb', wanted)
    if kind == 'zip':
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            return {n: z.read(n) for n in wanted}
    if kind != 'deb':
        raise ValueError('Unknown evidence format')
    for name, payload in ar_members(data):
        if name.startswith('data.tar'):
            if name.endswith('.zst'):
                import zstandard
                payload = zstandard.ZstdDecompressor().stream_reader(io.BytesIO(payload)).read()
            with tarfile.open(fileobj=io.BytesIO(payload)) as t:
                return {n: t.extractfile(n).read() for n in wanted}
    raise ValueError('Missing Debian payload')


def verify_evidence(lock, directory, offline=False):
    for evidence in lock['evidence']:
        rows = [r for r in lock['native_files'] if r.get('evidence_id') == evidence['id']]
        path = local_path(directory, evidence['sha256'])
        data = download(evidence, path, offline).read_bytes()
        found = evidence_members(data, evidence['format'], {r['evidence_member'] for r in rows},
                                 evidence.get('package_record'))
        for row in rows:
            checked(found[row['evidence_member']], row)
    return dict(Counter(r['status'] for r in lock['native_files'] if r.get('evidence_id')))


def verify_catalogs(lock, directory, executable, offline=False):
    from signed_catalog import verify_signature, verify_index
    evidence = {r['id']: r for r in lock['evidence']}
    results = []
    for catalog in lock.get('catalogs', []):
        def read(record):
            return download(record, local_path(directory, record['sha256']), offline).read_bytes()
        anchor = read(evidence[catalog['key']['evidence_id']])
        with zipfile.ZipFile(io.BytesIO(anchor)) as z:
            key = checked(z.read(catalog['key']['member']), catalog['key'])
        release = verify_signature(read(catalog['release']), key, catalog['key']['fingerprint'], executable)
        packages = [r for r in evidence.values() if r.get('catalog_id') == catalog['id']]
        date = verify_index(release, read(catalog['index']), catalog['index_path'], packages)
        results.append({'id': catalog['id'], 'signed_date': date, 'packages': len(packages),
                        'fingerprint': catalog['key']['fingerprint']})
    return results


def incomplete(lock):
    # A matching original package is one proof route, not a license requirement
    # to reproduce all MIT/zlib binaries. Unknown source builds still fail closed.
    reviewed = {'official-binary-match', 'permissive-notices-reviewed', 'source-built-reviewed'}
    return bool(lock['open_gates']) or any(r['status'] not in reviewed for r in lock['native_files'])


def source_materials(lock, directory, offline=False):
    def one(row):
        return download(row, local_path(directory, row['path']), offline)
    with ThreadPoolExecutor(max_workers=4) as pool:
        return list(pool.map(one, lock['materials']))


def bundle(lock, directory, output):
    source_materials(lock, directory, offline=True)
    notice = checked((REPO / lock['notice_asset']['path']).read_bytes(), lock['notice_asset'])
    files = {'native-source-lock.json': (json.dumps(lock, indent=2) + '\n').encode(),
             'NATIVE-NOTICES.txt': notice,
             'README.md': (REPO / 'tools/dependencies/README.md').read_text(encoding='utf-8').encode()}
    for name, row in lock.get('bundle_files', {}).items():
        local_path(Path('.'), name)  # Validate the archive-relative name too.
        if name in files or name.startswith('sources/'):
            raise ValueError('Duplicate/reserved bundle file')
        files[name] = checked(local_path(REPO, row['path']).read_bytes(), row)
    if output.exists():
        raise ValueError('Output already exists; use a new bundle filename')
    output.parent.mkdir(parents=True, exist_ok=True)
    # Only the locked source materials enter this archive. Evidence binaries,
    # arbitrary nearby files, user runtime archives and signing keys never enter it.
    with zipfile.ZipFile(output, 'x', compression=zipfile.ZIP_STORED) as z:
        def entry(name):
            info = zipfile.ZipInfo(name)
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            return info
        for name, data in sorted(files.items()):
            z.writestr(entry(name), data)
        for row in sorted(lock['materials'], key=lambda r: r['path']):
            data = checked(local_path(directory, row['path']).read_bytes(), row)
            z.writestr(entry('sources/' + row['path']), data)
    with zipfile.ZipFile(output) as z:
        if z.testzip() is not None:
            raise ValueError('Source bundle readback failed')
        for row in lock['materials']:
            checked(z.read('sources/' + row['path']), row)
    return sha(output.read_bytes())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--lock', type=Path, default=LOCK)
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--materials', type=Path, help='Fetch/verify locked source materials here')
    parser.add_argument('--evidence', type=Path, help='Fetch/verify pinned binary evidence here')
    parser.add_argument('--gpg', type=Path, help='Also verify recorded signed catalogs; requires --evidence')
    parser.add_argument('--offline', action='store_true')
    parser.add_argument('--bundle', type=Path)
    parser.add_argument('--require-complete', action='store_true', help='Fail while any distribution gates remain')
    args = parser.parse_args()
    lock = load_lock(args.lock)
    checked((REPO / lock['notice_asset']['path']).read_bytes(), lock['notice_asset'])
    if args.apk:
        print('APK native inventory:', dict(verify_native(args.apk, lock)))
    if args.evidence:
        print('Binary matches verified by evidence class:', verify_evidence(lock, args.evidence, args.offline))
    if args.gpg:
        if not args.evidence:
            parser.error('--gpg requires --evidence')
        print('Signed catalogs verified:', verify_catalogs(lock, args.evidence, args.gpg, args.offline))
    if args.materials:
        print('Source material files verified:', len(source_materials(lock, args.materials, args.offline)))
    if args.bundle:
        if not args.materials:
            parser.error('--bundle requires --materials')
        print('Source-material bundle SHA256:', bundle(lock, args.materials, args.bundle))
    print('Distribution review: INCOMPLETE' if incomplete(lock) else 'Distribution review: no recorded open gates')
    for gate in lock['open_gates']:
        print('- ' + gate)
    if args.require_complete and incomplete(lock):
        raise SystemExit(2)


if __name__ == '__main__':
    main()
