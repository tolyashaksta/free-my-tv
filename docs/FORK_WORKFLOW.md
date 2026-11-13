# Working with this fork

This is `tolyashaksta/free-my-tv`. Use `main`, tracking `origin/main`.
Read `CLAUDE.md` first and the ignored `LOCAL_SETUP.md` for computer-specific details.

## Windows tools and checks

The restored portable tools live inside the ignored `.local/toolchain/` directory:

- `jdk/<JDK directory>/bin/javac.exe`: Temurin JDK 17.
- `android-sdk/build-tools/34.0.0/`: Android build tools.
- `android-sdk/platforms/android-34/android.jar`: Android API 34.
- `android-sdk/platform-tools/adb.exe`: Android platform tools.

Git for Windows supplies Bash; Python and Node must also be installed. If this checkout is deleted, these tools can
be downloaded again from Adoptium and Google's Android SDK repository. Verify the archive checksums published by
those providers. Keep the directory layout above; no global environment changes are required.

From PowerShell in the project directory:

```powershell
./tools/local.ps1                 # Android build, Java tests, translations, web tests and LG/Samsung packages
./tools/local.ps1 -Action Build   # Own settings and ORIGINAL signing key, both restored first
./tools/local.ps1 -Action Generic # Settings chosen on the TV, still needs the ORIGINAL signing key
```

Check mode uses a separate verification key and leaves its APK at `.local/verification.apk`; it is for validation.
It preserves any existing `FreeMyTV.apk`. Build mode produces `FreeMyTV.apk` for installation. No command here
installs anything or changes the TV. Installation requires a separate request.

## Files to back up privately

Git restores source changes, but deliberately excludes these files:

- `freemytv.keystore`: original APK signing identity, essential for updating an installed personal build.
- `config.properties`: city, preferences and private API credentials.
- `LOCAL_SETUP.md`: TV address/model, local paths and setup notes (never put passwords or tokens here).
- Any TV pairing keys or Samsung signing certificates used for this installation.

Keep a separate private backup outside the checkout. Never commit these files. A newly generated signing key cannot
replace a lost original key for an in-place Android update. Do not uninstall or reset the TV just to resolve that.

## Import a published upstream release

Only after the owner asks to update, verify the latest published release and its exact tag. Fetch that tag, not
`upstream/main`. Ensure the working tree is clean and `origin/main` has been fetched before merging.

```powershell
git fetch origin main
git fetch --no-tags upstream tag v1.4.0
git merge --no-commit --no-ff v1.4.0
# Resolve conflicts, run ./tools/local.ps1, then commit and push main.
```

Replace the example tag with the requested release. Preserve the fork's TV Settings top button and configurable
All Apps shelf shortcut. These coexist with 1.4's Recent Apps, App Store and draggable top buttons.

The old round Wi-Fi/VPN indicators were deliberately removed at the owner's request. On Android, use the upstream
Status icons setting: Wi-Fi/Ethernet, VPN, Bluetooth audio and USB. VPN appears only while connected and visible to
the app, in the same style as the other icons. Android can hide a per-app VPN from a launcher excluded from its
allowed apps: include Free My TV in that VPN's app list if its status should be visible through these normal APIs.
Bluetooth means an audio connection (headphones/speaker), not merely an enabled adapter or a connected remote.
A single network subscription within the upstream status lifecycle tracks the underlying
connection and VPN together; turning the setting off or leaving Home stops it. Never restore the old separate
NetworkStatus watcher, its glass circles or its Top Buttons entries. Saved legacy indicator keys are ignored while
the real top buttons keep their order. Existing explicitly selected legacy indicators enable Status icons once;
subsequent changes to the switch are preserved. LG/Samsung no longer draw the old custom indicators; the upstream
status feature and VPN detection are Android-only.

Use HTTPS for `origin` if SSH authentication is unavailable. Never force-push an update.

## Recovery on 2026-11-10

Recovered source at `690f7ed` from the fork and merged published upstream `v1.4.0` (`d400001`). The former imported
upstream baseline was `7235308`, so some items in the full 1.4 release notes already existed in this fork.
The original private configuration, signing key and TV notes were initially absent from this checkout. Later on
2026-11-10 they were recovered from the Windows Recycle Bin, along with old local helpers and verification records
(archived under `.local/recovered/`). These files remain excluded from Git and still need a private backup outside
the checkout.

The customized 1.4 APK was then built using the recovered original signing key and installed over 1.3 without
uninstalling. The installed APK's SHA-256 matched the build, and the home activity and screensaver remained selected.
The private installation record is `.local/installed-build.json`; the previous APK is `.local/tv-before-update.apk`.


## Release update on 2026-11-13

Merged published upstream `v1.5.0` (`0e33f16`) into `main`, starting from `1bced2b`.
Preserved TV Settings and configurable All Apps, the unified status icons with VPN and their one-time migration.
The web editor combines the new input ordering with the fork's top-button editor and Arabic direction support.
Windows checks now include NetPicture tests; translation catalogues stay LF through `.gitattributes`, as required
by upstream's translation check. The full `tools/local.ps1` check passed: Android build, remote/order/status/network
picture tests, all translations, web preferences/fork controls, LG/Samsung packages and old-TV syntax/CSS checks.
This source update does not install anything on the TV; its last verified installation remains the customized 1.4.
