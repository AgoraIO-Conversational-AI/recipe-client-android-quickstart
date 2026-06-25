# Gradle Dependencies

**When to Read This:** You are upgrading Agora SDK versions, adding a new library, diagnosing dependency resolution errors, or understanding the version catalog setup.

## Version catalog (`android/gradle/libs.versions.toml`)

All library versions are declared centrally; modules reference them via `libs.*` aliases:

| Alias | Module | Version |
| ----- | ------ | ------- |
| `libs.agora.rtc` | `io.agora.rtc:full-sdk` | `4.5.1` |
| `libs.agora.rtm` | `io.agora:agora-rtm` | `2.2.3` |
| `libs.okhttp` | `com.squareup.okhttp3:okhttp` | `4.12.0` |
| `libs.okhttp.mockwebserver` | `com.squareup.okhttp3:mockwebserver` | `4.12.0` |
| `libs.kotlinx.serialization.json` | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.7.1` |
| `libs.gson` | `com.google.code.gson:gson` | `2.11.0` |
| `libs.kotlinx.coroutines.android` | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.8.1` |
| `libs.compose.bom` | `androidx.compose:compose-bom` | `2024.09.02` |

Plugins:

| Alias | ID | Version |
| ----- | -- | ------- |
| `libs.plugins.android.application` | `com.android.application` | AGP `8.5.2` |
| `libs.plugins.kotlin.android` | `org.jetbrains.kotlin.android` | `2.0.20` |
| `libs.plugins.kotlin.serialization` | `org.jetbrains.kotlin.plugin.serialization` | `2.0.20` |
| `libs.plugins.kotlin.compose` | `org.jetbrains.kotlin.plugin.compose` | `2.0.20` |

## Repository resolution order (`settings.gradle.kts`)

```
google() → mavenCentral() → jitpack (https://www.jitpack.io)
```

Agora RTC and RTM SDKs resolve from **Maven Central** — no custom Agora Maven repo is needed. The Agora RTM library resolves from the `io.agora` group on Maven Central. JitPack is present for any transitive dependencies that require it.

## Adding a new library

1. Add a `[versions]` entry in `libs.versions.toml` (if the version is new).
2. Add a `[libraries]` entry with `module` and `version.ref`.
3. Reference via `implementation(libs.<alias>)` in `android/app/build.gradle.kts`.
4. Sync in Android Studio or run `./gradlew :app:dependencies` to verify resolution.

## Upgrading Agora SDKs

1. Update `agoraRtc` and/or `agoraRtm` version strings in `[versions]`.
2. Check the [Agora Android SDK release notes](https://docs.agora.io/en/video-calling/overview/release-notes) for breaking API changes.
3. The ConversationalAIAPI toolkit in `convoaiApi/` is a vendored copy and does not automatically track SDK updates — re-vendor it from the upstream demo repo if the toolkit's RTC/RTM API usage diverges.
4. Run `./gradlew assembleDebug testDebugUnitTest` to catch compile errors.

## `compileSdk` and `minSdk`

| Field | Value | Notes |
| ----- | ----- | ----- |
| `compileSdk` | 35 | Android 14; required by latest Agora RTC SDK |
| `targetSdk` | 35 | Matches compileSdk |
| `minSdk` | 24 | Android 7.0; minimum supported by Agora RTC 4.x |
| `jvmTarget` | 17 | Kotlin compiler + `compileOptions` both set to 17 |

## Compose BOM

Compose dependencies (`material3`, `ui`) are resolved through the BOM (`libs.compose.bom`); individual Compose artifact versions are omitted from `build.gradle.kts`. To upgrade Compose, bump `composeBom` in the version catalog.

## Related L1

- [01_setup](../01_setup.md) — build and run commands.
- [03_code_map](../03_code_map.md) — file locations.
