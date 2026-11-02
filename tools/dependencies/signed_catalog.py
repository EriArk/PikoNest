"""Verify historical Termux catalog provenance without touching a user's keyring."""
import hashlib
from pathlib import Path
import subprocess
import tempfile


def fields(text):
    result = []
    for paragraph in text.strip().split('\n\n'):
        row = {}; key = None
        for line in paragraph.splitlines():
            if line.startswith((' ', '\t')) and key:
                row[key] += '\n' + line
            elif ':' in line:
                key, value = line.split(':', 1)
                if key in row:
                    raise ValueError('Duplicate package field: ' + key)
                row[key] = value.lstrip()
        if row:
            result.append(row)
    return result


def verify_index(release_text, index, index_path, evidence):
    release = fields(release_text)
    if len(release) != 1:
        raise ValueError('Invalid signed release body')
    checksums = [line.split() for line in release[0].get('SHA256', '').splitlines() if line.strip()]
    entries = [line for line in checksums if len(line) == 3 and line[2] == index_path]
    if len(entries) != 1:
        raise ValueError('Missing/duplicate signed index checksum')
    checksum, size, _ = entries[0]
    if hashlib.sha256(index).hexdigest() != checksum or len(index) != int(size):
        raise ValueError('Signed index hash/size mismatch')
    packages = fields(index.decode('utf-8'))
    for item in evidence:
        rows = [p for p in packages if p.get('Filename') == item['catalog_filename']]
        if len(rows) != 1:
            raise ValueError('Missing/duplicate package in signed index')
        row = rows[0]
        if (row['SHA256'] != item['sha256'] or int(row['Size']) != item['bytes']
                or row['Package'] != item['package'] or row['Version'] != item['version']):
            raise ValueError('Package disagrees with signed catalog: ' + item['id'])
    return release[0].get('Date', '')


def gpg_path(path, executable):
    # Git for Windows ships MSYS GnuPG, which accepts /c/... rather than C:/...
    converter = executable.parent / 'cygpath.exe'
    if converter.exists():
        return subprocess.check_output([str(converter), '-u', str(path.resolve())], text=True).strip()
    return str(path.resolve())


def verify_signature(signed, keyring, fingerprint, executable):
    with tempfile.TemporaryDirectory(prefix='pikonest-public-signature-') as tmp:
        root = Path(tmp)
        source = root / 'InRelease'; source.write_bytes(signed)
        key = root / 'public.gpg'; key.write_bytes(keyring)
        body = root / 'Release'
        convert = lambda p: gpg_path(p, executable)
        result = subprocess.run([
            str(executable), '--no-options', '--homedir', convert(root), '--batch',
            '--no-default-keyring', '--keyring', convert(key), '--no-auto-key-retrieve',
            '--status-fd', '1', '--output', convert(body), '--decrypt', convert(source)
        ], capture_output=True, text=True, encoding='utf-8', errors='replace')
        valid = [line.split()[2] for line in result.stdout.splitlines()
                 if line.startswith('[GNUPG:] VALIDSIG ')]
        if result.returncode != 0 or valid != [fingerprint]:
            raise ValueError('Catalog signature is invalid or signed by an unexpected key')
        return body.read_text(encoding='utf-8')
