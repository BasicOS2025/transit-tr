"""Offline public-distribution preflight. It does not certify legal compliance.

Never executes application code, contacts a provider, prints credentials, or
changes source/release files. Development builds remain separate from approval.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REQUIRED_PROVIDERS = {
    'ego-mobile', 'ego-website', 'iett-soap', 'otobusum-proxy', 'ibb-metro',
    'eshot-app', 'izmir-open-data', 'konya-atus', 'konya-gtfs', 'kentkart',
    'asis', 'bursa', 'osm',
}
SCOPES = ('android-legacy/src', 'android-modern/src', 'android-legacy/assets',
          'web', 'web-service')
EXCLUDED = {'node_modules', '.git', 'build', 'dist', '__pycache__', 'test'}


def source_files(root):
    for scope in SCOPES:
        base = root / scope
        if not base.exists():
            continue
        for path in sorted(base.rglob('*')):
            if path.is_file() and not EXCLUDED.intersection(path.relative_to(root).parts):
                if path.is_symlink():
                    raise ValueError('Public review does not accept symbolic links')
                yield path


def fingerprint(root):
    """Bind the manual review to the app, data and web service actually packaged."""
    digest = hashlib.sha256()
    for path in source_files(root):
        rel = path.relative_to(root).as_posix()
        digest.update(rel.encode('utf-8') + b'\0')
        digest.update(hashlib.sha256(path.read_bytes()).digest())
    return digest.hexdigest()


def check(root, policy):
    issues = []
    def add(code, detail):
        issues.append({'code': code, 'detail': detail})
    owner = policy.get('publisher', {})
    for field in ('legal_name', 'contact', 'privacy_notice_url'):
        value = owner.get(field, '')
        if not isinstance(value, str) or not value.strip() or re.search(r'TODO|DOLDUR|example\.', value, re.I):
            add('PUBLISHER_IDENTITY', field + ' is missing or a placeholder')
    providers = {p.get('id'): p for p in policy.get('providers', []) if isinstance(p, dict)}
    for name in sorted(REQUIRED_PROVIDERS):
        p = providers.get(name, {})
        # A declaration that a source is "disabled" is insufficient: a reviewed
        # build must actually omit it and the review must cover the resulting hash.
        if p.get('status') != 'reviewed' or p.get('basis') not in ('open-license', 'written-permission', 'omitted-from-reviewed-build'):
            add('PROVIDER_RIGHTS', name + ': access and redistribution basis not reviewed')
        elif not p.get('evidence') or not p.get('reviewer') or not p.get('reviewed_on'):
            add('PROVIDER_EVIDENCE', name + ': evidence/reviewer/date missing')
    review = policy.get('release_review', {})
    if review.get('source_sha256') != fingerprint(root):
        add('REVIEW_BINDING', 'The current source/data fingerprint has not been reviewed')
    for field in ('privacy_notice_in_ui', 'data_minimization', 'retention_and_deletion',
                  'cross_border_transfer_basis', 'database_license_obligations',
                  'third_party_code_and_marks', 'credential_distribution_rights'):
        if review.get(field) is not True:
            add('REVIEW_REQUIRED', field)
    # Static indicators are engineering release blockers, not a finding that an
    # offence has occurred. Match code, never output the source line or key value.
    patterns = [
        ('TLS_BYPASS', re.compile(r'rejectUnauthorized\s*:\s*false')),
        ('TLS_BYPASS', re.compile(r'\.disableCertificateChecks\s*\(')),
        ('TLS_BYPASS', re.compile(r'setHostnameVerifier\s*\(')),
    ]
    for path in source_files(root):
        if path.suffix not in ('.java', '.mjs', '.js'):
            continue
        text = path.read_text(encoding='utf-8', errors='replace')
        for code, pattern in patterns:
            match = pattern.search(text)
            if match:
                line = text.count('\n', 0, match.start()) + 1
                add(code, path.relative_to(root).as_posix() + ':' + str(line))
        if path.suffix == '.java' and 'notifyServerCertificate' in text:
            # Custom TLS authentication requires a separately reviewed validation
            # implementation; accepting a chain alone is not hostname validation.
            if policy.get('release_review', {}).get('custom_tls_validated') is not True:
                add('CUSTOM_TLS_REVIEW', path.relative_to(root).as_posix())
    return issues


def enforce(root=ROOT):
    path = root / 'legal/public-release.json'
    try:
        policy = json.loads(path.read_text(encoding='utf-8'))
        issues = check(root, policy)
    except (OSError, ValueError, TypeError) as error:
        raise SystemExit('PUBLIC RELEASE BLOCKED: missing/invalid legal review configuration') from error
    if issues:
        print(json.dumps({'public_release': 'blocked', 'issues': issues}, ensure_ascii=False, indent=2))
        raise SystemExit(2)
    print('Public release preflight passed; this is not a legal opinion or provider permission.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fingerprint', action='store_true', help='Print the source/data digest without approving it')
    args = parser.parse_args()
    if args.fingerprint:
        print(fingerprint(ROOT))
    else:
        enforce()
