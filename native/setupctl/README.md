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

## Guest rotation helper (sunset.27)

`RotationCtl` also runs as guest UID/GID 1000, but never invokes setup skipping.
It queries the guest WindowManager's current rotation and requests the next
quarter turn using named reflected APIs (`freezeRotation` on ICS–Nougat,
`setRotation` on Gingerbread). No fixed Binder transaction numbers or sensors
are required. Apps with fixed orientation can still follow the guest's normal
orientation policy. Manual rotation locks the guest rotation; automatic rotation
can be enabled again inside guest Android. Recovery/charging mode has no Android
WindowManager and the menu action is disabled there.

The helper JAR now uses min-api 9; SetupCtl's supported versions remain 16–25.
Host API-selection smoke test:

```sh
javac --release 8 -d native/setupctl/build/classes native/setupctl/src/app/aemu/setup/RotationPolicy.java native/setupctl/tests/RotationPolicyTest.java
java -cp native/setupctl/build/classes app.aemu.setup.RotationPolicyTest
```

[Gingerbread API](https://android.googlesource.com/platform/frameworks/base/+/android-2.3.7_r1/core/java/android/view/IWindowManager.aidl),
[ICS API](https://android.googlesource.com/platform/frameworks/base/+/android-4.0.4_r2.1/core/java/android/view/IWindowManager.aidl).

## Google-app option (sunset.27)

`GoogleCtl disable|restore` runs as guest UID/GID 1000 after zygote, API 9–25.
It discovers installed `com.google.*` packages plus Play Store, Chrome and
Google partner bookmarks, rather than assuming a ROM's APK paths. HOME and
input-method packages, WebView, package installer/permissions and critical
Google-branded platform plumbing are exempt. Setup wizard is owned separately
by SetupCtl. Unknown nonstandard OEM package names are not guessed.

Previous enabled states are persisted before mutation in
`/data/system/aemu-google-disabled.properties`, and preserved across boots.
Already-disabled packages are not claimed. Restore only changes recorded apps
still in the disabled state this feature uses; user re-enabling or otherwise
changing the state is preserved. APKs/data are never removed. Per-package disable
failures are logged without blocking other candidates. A full stop/start applies
the option, and guest apps depending on Google services may stop working.

Policy test (no firmware required):

```sh
javac --release 8 -d native/setupctl/build/classes native/setupctl/src/app/aemu/setup/SetupPolicy.java native/setupctl/src/app/aemu/setup/GooglePolicy.java native/setupctl/tests/GooglePolicyTest.java
java -cp native/setupctl/build/classes app.aemu.setup.GooglePolicyTest
```
