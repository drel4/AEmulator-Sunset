# Guest setup option

`SetupCtl` runs as guest UID/GID 1000 in a separate, bounded `app_process`.
The host starts it after zygote, without waiting for `sys.boot_completed`.
It supports the external SettingsProvider interface in guest API 16–25
(Android 4.1–7.1), including SQLite-backed and XML-backed settings.

The provider is acquired through `IActivityManager.getContentProviderExternal`,
as in the stock settings CLI. A standalone process is not registered as an AMS
application: acquiring through a system Context's ContentResolver would fail.
Insert/query reflection handles the calling-package and cancellation-argument
differences between these guest versions. Each provision flag is read back.

Only exact package names from `SetupPolicy` are disabled. It does not disable
arbitrary launchers, sign-in apps, or packages guessed from a substring.
Before disabling a package, its old enabled state is atomically persisted in
`/data/system/aemu-setup-skip.properties`. Opt-out restores only those recorded
packages still in the disabled state this feature uses. Provision flags remain
completed; opt-out is not a factory reset or a way to replay setup.

Unknown OEM wizards and additional vendor-specific setup state are not covered.
This does not bypass activation, account locks, or encrypted device data.
The existing old-engine and legacy Google sign-in workarounds remain separate.

Build on Linux with `ANDROID_HOME` and JDK configured:

```
sh native/setupctl/build.sh
javac --release 8 -d native/setupctl/build/classes native/setupctl/src/app/aemu/setup/SetupPolicy.java native/setupctl/tests/PolicyTest.java
java -cp native/setupctl/build/classes PolicyTest
```

No stock APK, ODEX, framework JAR or build.prop is patched by this option.
