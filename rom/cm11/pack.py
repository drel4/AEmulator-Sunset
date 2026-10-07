#!/usr/bin/env python3
"""Create an untested AESS v2 VM from the built CM11 tree; no user data.
AEmulator Sunset addition, 2026-10-04. GPL-3.0; see LICENSE.
"""
import gzip
import hashlib
import io
import json
import os
import pathlib
import posixpath
import stat
import struct
import subprocess
import sys
import tarfile
import xml.etree.ElementTree as ET

def cpio_entries(blob):
    offset = 0
    while offset + 110 <= len(blob):
        assert blob[offset:offset + 6] == b'070701', 'Expected newc ramdisk'
        fields = [int(blob[offset + 6 + i * 8:offset + 14 + i * 8], 16) for i in range(13)]
        _, mode, uid, gid, _, mtime, size, _, _, _, _, namesize, _ = fields
        offset += 110
        name = blob[offset:offset + namesize - 1].decode('utf-8')
        offset = (offset + namesize + 3) & ~3
        data = blob[offset:offset + size]
        assert len(data) == size
        offset = (offset + size + 3) & ~3
        if name == 'TRAILER!!!':
            return
        name = name.removeprefix('./').rstrip('/')
        if name in ('', '.'):
            continue
        assert not name.startswith('/') and '..' not in name.split('/')
        yield name, mode, uid, gid, mtime, data
    raise ValueError('Missing cpio trailer')

def package(base):
    out = base / 'src/out/target/product/aess'
    dest = base / 'packaged-AESS442-3'
    dest.mkdir(exist_ok=True)
    archive = dest / 'Android442forAESS.aessvm'
    assert not archive.exists(), 'Refusing to replace an existing package'
    boot = dest / 'boot.img'
    assert not boot.exists(), 'Refusing to replace boot image'
    subprocess.run([str(base / 'src/out/host/linux-x86/bin/mkbootimg'),
                    '--kernel', '/dev/null', '--ramdisk', str(out / 'ramdisk.img'),
                    '--output', str(boot)], check=True)
    blob = boot.read_bytes()
    assert blob[:8] == b'ANDROID!'
    kernel_size, _, rd_size = struct.unpack_from('<III', blob, 8)
    page = struct.unpack_from('<I', blob, 36)[0]
    assert kernel_size == 0 and blob[page:page + rd_size] == (out / 'ramdisk.img').read_bytes()
    props = dict(line.split('=', 1) for line in (out / 'system/build.prop').read_text().splitlines()
                 if '=' in line and not line.startswith('#'))
    assert props['ro.build.version.release'] == '4.4.2'
    assert props['ro.build.version.sdk'] == '19'
    paths = sorted((out / 'system').rglob('*'))
    paths.insert(0, out / 'system')
    request = ''.join(p.relative_to(out).as_posix() + ('/' if p.is_dir() and not p.is_symlink() else '') + '\n'
                      for p in paths)
    result = subprocess.run([str(dest / 'guest-fs-config')], input=request,
                            text=True, capture_output=True, check=True)
    config = {}
    for line in result.stdout.splitlines():
        path, uid, gid, mode = line.split()
        config[path] = int(uid), int(gid), int(mode, 8)
    assert len(config) == len(paths)
    codecs = out / 'system/etc/media_codecs.xml'
    assert codecs.read_bytes() == (base / 'src/device/generic/goldfish/camera/media_codecs.xml').read_bytes()
    assert any(c.get('name') == 'OMX.google.vorbis.decoder' and c.get('type') == 'audio/vorbis'
               for c in ET.parse(codecs).findall('./Decoders/MediaCodec'))
    assert (out / 'system/lib/libstagefright_soft_vorbisdec.so').is_file()
    profile = dict(id='aess-cm11-442-3', name='AEmulator Sunset CM11', release='4.4.2', api=19,
                   brand=props['ro.product.brand'].capitalize(), model=props['ro.product.model'],
                   skin='CyanogenMod 11', engine='kk', abi='armeabi-v7a', profileVersion=0,
                   runtime='dalvik', sourceName=archive.name, baseId='',
                   settings=dict(width=540, height=960, density=240),
                   warnings=['Experimental CM11 guest; not firmware for a physical device.'],
                   oneTimeNote='CM11 includes root, CM File Manager and Terminal. No Google Apps. This old Android version is for trusted testing, not a secure daily-use system.',
                   aessvmIncludesData=False, aessvmIncludesConfig=True,
                   aessvmRomFingerprint=props.get('ro.build.fingerprint', ''))
    entries = set()
    with tarfile.open(archive.with_suffix('.partial'), 'w:gz', format=tarfile.PAX_FORMAT) as tar:
        def add(name, mode, uid=0, gid=0, data=b'', kind=tarfile.REGTYPE, link=''):
            assert name not in entries, name
            entries.add(name)
            info = tarfile.TarInfo(name)
            info.mode, info.uid, info.gid = mode & 0xfff, uid, gid
            info.type, info.linkname = kind, link
            info.size = len(data) if kind == tarfile.REGTYPE else 0
            tar.addfile(info, io.BytesIO(data) if info.size else None)
        def link_target(name, target):
            if target.startswith('/'):
                target = posixpath.relpath('root' + target, posixpath.dirname(name))
            resolved = posixpath.normpath(posixpath.join(posixpath.dirname(name), target))
            assert resolved == 'root' or resolved.startswith('root/'), (name, target)
            return target
        add('aessvm.version', 0o644, data=b'2\n')
        add('image.json', 0o644, data=json.dumps(profile, indent=2).encode())
        add('aessvm.parts', 0o644, data=b'110\n')
        add('root', 0o755, kind=tarfile.DIRTYPE)
        for path in paths:
            rel = path.relative_to(out).as_posix()
            name = 'root/' + rel
            uid, gid, mode = config[rel]
            if path.is_symlink():
                add(name, mode, uid, gid, kind=tarfile.SYMTYPE,
                    link=link_target(name, os.readlink(path)))
            elif path.is_dir():
                add(name, mode, uid, gid, kind=tarfile.DIRTYPE)
            elif path.is_file():
                add(name, mode, uid, gid, data=path.read_bytes())
            else:
                raise ValueError('Special system file: ' + rel)
        for rel, mode, uid, gid, _, data in cpio_entries(gzip.decompress((out / 'ramdisk.img').read_bytes())):
            if rel.split('/')[0] in ('system', 'data', 'cache', 'dev', 'proc', 'sys'):
                continue
            name = 'root/' + rel
            if stat.S_ISDIR(mode):
                add(name, mode, uid, gid, kind=tarfile.DIRTYPE)
            elif stat.S_ISLNK(mode):
                add(name, mode, uid, gid, kind=tarfile.SYMTYPE,
                    link=link_target(name, data.decode()))
            elif stat.S_ISREG(mode):
                add(name, mode, uid, gid, data=data)
            else:
                raise ValueError('Special ramdisk file: ' + rel)
        # init.rc creates this at runtime; AEmulator does not execute init.
        if 'root/etc' not in entries:
            add('root/etc', 0o777, kind=tarfile.SYMTYPE, link='system/etc')
        add('boot.img', 0o644, data=blob)
    with tarfile.open(archive.with_suffix('.partial'), 'r:gz') as tar:
        members = tar.getmembers()
        assert [m.name for m in members[:3]] == ['aessvm.version', 'image.json', 'aessvm.parts']
        assert tar.extractfile('aessvm.parts').read() == b'110\n'
        assert tar.extractfile('root/system/build.prop').read() == (out / 'system/build.prop').read_bytes()
        assert tar.extractfile('root/system/etc/media_codecs.xml').read() == codecs.read_bytes()
        etc_alias = tar.getmember('root/etc')
        assert etc_alias.issym() and etc_alias.linkname == 'system/etc'
        for required in ('root/init.rc', 'root/system/app/CMFileManager.apk', 'root/system/app/Term.apk',
                         'root/system/xbin/su', 'root/system/lib/libjackpal-androidterm5.so',
                         'root/system/lib/libjackpal-termexec2.so',
                         'root/system/lib/libstagefright_soft_vorbisdec.so',
                         'root/system/media/audio/ui/Effect_Tick.ogg'):
            tar.getmember(required)
        for m in members:
            assert not m.name.startswith('/') and '..' not in m.name.split('/')
            assert m.name.split('/')[1:2] not in [[x] for x in ('data', 'cache', 'dev', 'proc', 'sys')]
            if m.issym():
                assert not m.linkname.startswith('/')
                link_target(m.name, m.linkname)
            if m.isfile():
                with tar.extractfile(m) as source:
                    while source.read(65536):
                        pass
    archive.with_suffix('.partial').rename(archive)
    digest = hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest()
    print(f'Validated {len(entries)} entries. {archive.stat().st_size} bytes. SHA256={digest}', flush=True)

if __name__ == '__main__':
    package(pathlib.Path(sys.argv[1]).resolve())
