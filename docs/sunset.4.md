# sunset.4: restart supervision and crash evidence

The sunset.3 Sony C5503 run still shows the boot animation, not the launcher.
Its boot-completed property is not proof that the UI appeared. The supplied
guest log contains a native SIGABRT in system_server (pid 22353, tid 23119),
followed by the deaths of its registered services. Nearby Qualcomm location
errors are a clue, not an established cause. Host exit code 137 alone does
not establish out-of-memory killing.

## Confirmed fixes

- The watchdog removed the zygote entry **after** starting its replacement,
  deleting the new process from supervision. It now consumes the exact dead
  instance before restarting. Subsequent crashes remain supervised, and the
  six-restart limit is enforced even after boot-completed properties are set.
- Host logging calls boot completion a guest report, with UI unverified.
- Export reads persisted main/system/radio records and service stdout/stderr,
  rather than the guest panel's last 300 records. The panel stays lightweight.
- Native crash reporting streams the entire maps file: the old 256 KiB
  buffer could omit thread stacks near the end. Stack discovery continues
  after the executable-symbol table reaches its limit. Dumps include thread
  names and explicitly report an unavailable stack scan.

This is a recovery/diagnostics build, **not a claimed fix for the SIGABRT**.
No speculative GPS or other vendor-library patches are included.

## Next run

Install sunset.4 over the same clone package. Cold-start the existing image;
do not wipe or reimport it. Reproduce the stall and export the guest and host
logs from the log panel. The expanded guest export includes the assertion
buffer and zygote stderr that may identify the abort's caller.

Version code 6; version name `0.0.0.3-sunset.4`.

Native verification includes a maps file exceeding 256 KiB, stack discovery
after table exhaustion, an unreadable guard page, and the existing fopen
smoke checks. JVM export tests verify early records, alternate buffers,
service stderr, and incomplete trailing records.
