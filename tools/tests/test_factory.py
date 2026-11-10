import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parents[1]

def load(name):
    spec = importlib.util.spec_from_file_location(name, HERE / (name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

class TestFactory(unittest.TestCase):
    def test_reject_traversal_slug(self):
        m = load('new_app')
        with self.assertRaises(ValueError):
            m.create('../private', 'Bad')

    def test_reject_existing_app(self):
        m = load('new_app')
        with tempfile.TemporaryDirectory() as directory:
            fake = Path(directory)
            (fake / 'templates/android-app').mkdir(parents=True)
            (fake / 'apps/demo').mkdir(parents=True)
            with patch.object(m, 'ROOT', fake):
                with self.assertRaises(FileExistsError):
                    m.create('demo', 'Demo')

    def test_reject_keys_in_public_repo(self):
        m = load('check_repo')
        with tempfile.TemporaryDirectory() as directory:
            fake = Path(directory)
            (fake / 'key.jks').write_text('SENSITIVE')
            with patch.object(m, 'ROOT', fake):
                self.assertTrue(m.check(['key.jks']))

    def test_validate_slug_rejects_traversal(self):
        m = load('validate_app')
        with self.assertRaises(ValueError):
            m.validate('../../private')

if __name__ == '__main__':
    unittest.main()
