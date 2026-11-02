"""Prepare a deterministic, allowlisted support refresh from pinned package evidence.

Never executes downloaded binaries and never reads the purchased PICO-8 archive.
The original support archive is retained in the pinned upstream APK.
"""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import tarfile
import zipfile

import audit

REPO = Path(__file__).resolve().parents[2]
MANIFEST = REPO / 'tools/dependencies/runtime-support.json'


def compose(original, manifest, replacements):
    audit.checked(original, manifest['original_bootstrap'])
    rows = {r['path']: r for r in manifest['files']}
    if len(rows) != len(manifest['files']) or rows.keys() != replacements.keys():
        raise ValueError('Duplicate or incomplete replacement inventory')
    for name, row in rows.items():
        audit.local_path(Path('package-review'), name)
        if not name.startswith('package/'):
            raise ValueError('Replacement outside support package')
        audit.checked(replacements[name], row)
    raw = io.BytesIO()
    seen = set()
    with tarfile.open(fileobj=io.BytesIO(original)) as source, tarfile.open(fileobj=raw, mode='w') as target:
        for member in source:
            if member.name in seen:
                raise ValueError('Duplicate original support entry')
            seen.add(member.name)
            stream = source.extractfile(member) if member.isfile() else None
            if member.name in rows:
                row = rows[member.name]
                if not member.isfile() or 'previous_sha256' not in row:
                    raise ValueError('Unexpected existing replacement target')
                if hashlib.sha256(stream.read()).hexdigest() != row['previous_sha256']:
                    raise ValueError('Original support file changed')
                member.size = row['bytes']
                stream = io.BytesIO(replacements[member.name])
            target.addfile(member, stream)
        for name in sorted(rows.keys() - seen):
            row = rows[name]
            if 'previous_sha256' in row:
                raise ValueError('Missing original replacement target')
            member = tarfile.TarInfo(name)
            member.size = row['bytes']
            member.mode = 0o755
            target.addfile(member, io.BytesIO(replacements[name]))
    return gzip.compress(raw.getvalue(), mtime=0)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, default=REPO / '.local/downloads/pico8-frontend.apk')
    parser.add_argument('--evidence', type=Path, default=REPO / '.local/dependency-audit/evidence')
    parser.add_argument('--output', type=Path, default=REPO / '.local/dependency-audit/runtime-support.tar.gz')
    parser.add_argument('--offline', action='store_true')
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding='utf-8'))
    lock = audit.load_lock()
    replacements = {}
    for evidence in lock['evidence']:
        rows = [r for r in manifest['files'] if r['evidence_id'] == evidence['id']]
        if not rows:
            continue
        data = audit.download(evidence, audit.local_path(args.evidence, evidence['sha256']), args.offline).read_bytes()
        members = audit.evidence_members(data, evidence['format'], {r['evidence_member'] for r in rows})
        replacements.update({r['path']: members[r['evidence_member']] for r in rows})
    with zipfile.ZipFile(args.upstream) as z:
        output = compose(z.read('assets/package.dat'), manifest, replacements)
    audit.checked(output, manifest['output'])
    if args.output.exists():
        audit.checked(args.output.read_bytes(), manifest['output'])
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(output)
    print('Pinned support refresh verified:', len(replacements), 'files;', audit.sha(output))


if __name__ == '__main__':
    main()
