"""Offline fixtures for the release checker; never executes Transit TR."""
from pathlib import Path
import tempfile
import unittest
from legal_release_check import check, fingerprint, REQUIRED_PROVIDERS


class ReleaseCheckTests(unittest.TestCase):
    def fixture(self, root):
        src = root / 'android-legacy/src'
        src.mkdir(parents=True)
        (src / 'Example.java').write_text('class Example {}', encoding='utf-8')
        return {
            'publisher': {'legal_name': 'Fixture publisher', 'contact': 'fixture@invalid.test', 'privacy_notice_url': 'https://invalid.test/privacy'},
            'providers': [{'id': p, 'status': 'reviewed', 'basis': 'written-permission',
                           'evidence': 'fixture only', 'reviewer': 'Fixture reviewer', 'reviewed_on': '2026-11-10'}
                          for p in REQUIRED_PROVIDERS],
            'release_review': dict.fromkeys(('privacy_notice_in_ui', 'data_minimization', 'retention_and_deletion',
                'cross_border_transfer_basis', 'database_license_obligations', 'third_party_code_and_marks',
                'credential_distribution_rights', 'custom_tls_validated'), True),
        }

    def test_review_is_bound_to_current_source_and_data(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); policy = self.fixture(root)
            policy['release_review']['source_sha256'] = fingerprint(root)
            self.assertEqual(check(root, policy), [])
            (root / 'android-legacy/src/Example.java').write_text('class Example {int changed;}', encoding='utf-8')
            self.assertIn('REVIEW_BINDING', [x['code'] for x in check(root, policy)])

    def test_pending_rights_are_not_approved_by_license_url(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); policy = self.fixture(root)
            policy['providers'][0].update(status='pending', license_url='https://invalid.test/license')
            self.assertIn('PROVIDER_RIGHTS', [x['code'] for x in check(root, policy)])

    def test_tls_bypass_blocks_without_printing_contents(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); policy = self.fixture(root)
            src = root / 'web-service'; src.mkdir()
            (src / 'transport.mjs').write_text("const secret='fixture-secret'; new Agent({rejectUnauthorized:false});", encoding='utf-8')
            policy['release_review']['source_sha256'] = fingerprint(root)
            issues = check(root, policy)
            self.assertIn('TLS_BYPASS', [x['code'] for x in issues])
            self.assertNotIn('fixture-secret', str(issues))

    def test_missing_provider_and_publisher_block(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp); policy = self.fixture(root)
            policy['providers'] = []; policy['publisher'] = {}
            issues = check(root, policy)
            self.assertEqual(sum(x['code'] == 'PROVIDER_RIGHTS' for x in issues), len(REQUIRED_PROVIDERS))
            self.assertEqual(sum(x['code'] == 'PUBLISHER_IDENTITY' for x in issues), 3)


if __name__ == '__main__':
    unittest.main()
