# Sunset.11: stability rollback

The Xperia ZR user reports that sunset.10 makes Recents visible, but after
some time both SystemUI and the launcher freeze. Log (3) confirms the .10
workaround ran with the correct system_server caller UID (1000). It also
shows invalid Parcel exception codes during ActivityManager.checkPermission,
nested Keyguard permission checks, broken UI input channels and SystemUI ANRs.

That evidence does not establish which engine reply-routing or buffer-lifetime
fault is responsible, but it rules out treating .10 as a successful fix.
The immediate action is to remove the new scheduling behavior completely.

The guest shim and its test runner are restored byte-for-byte to sunset.9
(commit 86d6baf). The .10 synthetic Keyguard queue test is removed because
it validated packet filtering, not live engine scheduling, reply association,
buffer ownership or long-running UI stability. All of it remains recoverable
from the .10 Git tag. Existing shim and audio tests remain.

The app version is increased to code 13 / 0.0.0.3-sunset.11 so this rollback
can be installed over .10 without uninstalling the app or erasing guest data.
The working .9 setup policy, synthetic SIM identity, vibration, controls and
translations are preserved. Camera WIP is not included.

This release is not a confirmed Recents fix and may restore the original
invisible/black Recents behavior. Long-running on-phone stability still needs
the user's test. A future fix must cover the real Binder engine's scheduling
and transaction ownership; a packet-only test is insufficient.

## Validation

The remote Linux build passed all 16 JVM tests, 5 ARM shim smoke tests and
2 audio ABI smoke tests. Both APKs passed signature verification and report
version code 13. The rebuilt and APK-embedded shim SHA-256 is
`20ae6f59701d15ae9261eb49d56d7d7bed16a8479fede6f0bbeaa50d21298908`,
identical to sunset.9. The direct-track audio library is unchanged. These
checks verify the rollback artifacts, not the user's long-running UI workload.
