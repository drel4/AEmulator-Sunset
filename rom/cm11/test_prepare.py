#!/usr/bin/env python3
"""Fixture tests: no modification of the real source checkout."""
import hashlib
import pathlib
import shutil
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile

RECIPE = pathlib.Path(__file__).resolve().parent
APK = pathlib.Path(sys.argv[1]).resolve()
sys.argv = sys.argv[:1]
ORIGINAL = '''PRODUCT_PACKAGES += \\
    CMFileManager \\
    CMUpdater \\
    CMFota \\
    CMAccount
PRODUCT_COPY_FILES += \\
    vendor/cm/proprietary/Term.apk:system/app/Term.apk \\
    vendor/cm/proprietary/lib/armeabi/libjackpal-androidterm4.so:system/lib/libjackpal-androidterm4.so
PRODUCT_PACKAGES += Superuser su
'''

class PrepareTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='aess-cm11-test-')
        self.addCleanup(self.temp.cleanup)
        self.base = pathlib.Path(self.temp.name)
        self.common = self.base / 'src/vendor/cm/config/common.mk'
        self.common.parent.mkdir(parents=True)
        self.common.write_text(ORIGINAL)
        minui = self.base / 'src/bootable/recovery/minui/Android.mk'
        minui.parent.mkdir(parents=True)
        minui.write_text('''# Some devices need kernel headers for graphics
ifeq ($(TARGET_PREBUILT_KERNEL),)
  LOCAL_ADDITIONAL_DEPENDENCIES := $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr
  LOCAL_C_INCLUDES += $(TARGET_OUT_INTERMEDIATES)/KERNEL_OBJ/usr/include
endif
''')
        (self.base / 'tools').mkdir()
        ime = self.base / 'src/frameworks/base/services/java/com/android/server/InputMethodManagerService.java'
        ime.parent.mkdir(parents=True)
        ime.write_text('''            defIm = InputMethodUtils.getMostApplicableDefaultIME(
                    mSettings.getEnabledInputMethodListLocked());
            Slog.i(TAG, "No default found, using " + defIm.getId());''')
        shutil.copyfile(APK, self.base / 'tools/Term.apk')

    def run_prepare(self):
        return subprocess.run([sys.executable, str(RECIPE / 'prepare.py'), str(self.base)],
                              capture_output=True, text=True)

    def test_packages_libraries_and_idempotence(self):
        result = self.run_prepare()
        self.assertEqual(result.returncode, 0, result.stderr)
        first = self.common.read_bytes()
        text = first.decode()
        self.assertIn('CMFileManager', text)
        self.assertIn('Superuser su', text)
        self.assertNotIn('CMUpdater', text)
        self.assertNotIn('CMFota', text)
        self.assertNotIn('    CMAccount', text)
        self.assertNotIn('androidterm4', text)
        with zipfile.ZipFile(APK) as archive:
            for lib in ('libjackpal-androidterm5.so', 'libjackpal-termexec2.so'):
                installed = self.base / 'src/vendor/cm/proprietary/lib/armeabi' / lib
                self.assertEqual(installed.read_bytes(), archive.read('lib/armeabi/' + lib))
        self.assertEqual(self.run_prepare().returncode, 0)
        self.assertEqual(self.common.read_bytes(), first)
        ime = self.base / 'src/frameworks/base/services/java/com/android/server/InputMethodManagerService.java'
        self.assertIn('setInputMethodEnabledLocked(defIm.getId(), true)', ime.read_text())
        self.assertEqual(ime.read_text().count('// AESS: first boot'), 1)
        product = self.base / 'src/device/aemulator/aess/cm.mk'
        self.assertIn('PRODUCT_NAME := cm_aess', product.read_text())
        overlay = product.parent / 'overlay/frameworks/base/core/res/res/values/config.xml'
        resources = ET.parse(overlay).getroot()
        self.assertEqual([(item.tag, item.get('name'), item.text) for item in resources],
                         [('bool', 'config_showNavigationBar', 'true')])

    def test_checksum_failure_precedes_changes(self):
        (self.base / 'tools/Term.apk').write_bytes(b'wrong APK')
        result = self.run_prepare()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.common.read_text(), ORIGINAL)
        self.assertFalse((self.base / 'src/device').exists())

if __name__ == '__main__':
    unittest.main()
