# Android build and test workflow

GitHub Actions builds the Android debug APK and runs local unit tests on pushes and pull requests to `main`.

## Workflow

- Uses JDK 17 and Android SDK.
- Installs Gradle 8.11.1 through the Gradle setup action.
- Runs `gradle --no-daemon assembleDebug`.
- Runs `gradle --no-daemon :app:testDebugUnitTest`.
- Uploads the debug APK and unit-test HTML reports when available.

## Downloading the APK

Open the repository's **Actions** tab, select a successful `Android build and unit tests` run, and download the `nova-debug-apk` artifact. The artifact is a debug build for development and testing, not a signed release APK.

## Limitations

A successful CI build is not a substitute for testing on a physical Android device. It does not prove that a production GLB exists, that Filament renders a particular asset correctly, or that real-time speech/lip-sync performance is acceptable. Those checks require a real model and device-level validation.

The project currently does not include a Gradle wrapper, so this workflow installs a pinned Gradle version in CI. For reproducible local builds, adding and checking in the Gradle wrapper should be a separate follow-up.
