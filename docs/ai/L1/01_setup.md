# 01 · Setup

> Install the Android toolchain and the Python backend, configure env vars, and build and run the quickstart locally.

## Prerequisites

| Tool | Required version | Notes |
| ---- | ---------------- | ----- |
| Android Studio | Latest stable (Iguana / Jellyfish+) | Bundled JDK 17+ works |
| Java | 17 (temurin or bundled) | CI uses `temurin@21`; local JDK 17 is sufficient |
| Android SDK | compileSdk 35, minSdk 24 | SDK Manager → Android 14 (API 35) + build tools |
| Python | 3.10+ | Backend only; CI uses 3.12 |
| `uv` | any recent | Fastest way to create the venv; plain `python -m venv` also works |

## Configure env

Create `server/.env.local` (copy from `server/.env` if present):

| Variable                | Required | Default          | Notes                                             |
| ----------------------- | :------: | ---------------- | ------------------------------------------------- |
| `AGORA_APP_ID`          |    ✅    | —                | Agora Console → Project → App ID                  |
| `AGORA_APP_CERTIFICATE` |    ✅    | —                | Agora Console → Project → App Certificate         |
| `OPENAI_MODEL`          |          | `gpt-4o-mini`    | Agora-managed OpenAI vendor; keyless by default   |
| `OPENAI_API_KEY`        |          | —                | BYO only — set if your account requires it        |
| `AGENT_GREETING`        |          | built-in line    | Optional opening utterance override               |

The `AGORA_APP_CERTIFICATE` stays on the server and is never sent to the app.

## Run the backend

```bash
cd server
uv venv venv && . venv/bin/activate
uv pip install -r requirements.txt -r requirements-dev.txt
python src/server.py        # serves on 0.0.0.0:8000
```

Backend unit tests (no cloud, no real creds):

```bash
cd server && pytest -q
```

## Build and run the Android app

### Via Android Studio

Open `android/` in Android Studio → Run on a connected device or emulator.

### Via Gradle command line

```bash
cd android
./gradlew assembleDebug           # build only
./gradlew installDebug            # install to running emulator / device
./gradlew testDebugUnitTest       # JVM unit tests (no emulator needed)
```

## Emulator vs physical device

| Scenario | `AGENT_BACKEND_URL` | How to set |
| -------- | ------------------- | ---------- |
| Android emulator | `http://10.0.2.2:8000` (default) | Default in `app/build.gradle.kts` |
| Physical device | `http://<host-LAN-IP>:8000` | Edit `buildConfigField` in `android/app/build.gradle.kts` |

`10.0.2.2` is the emulator's alias for the host machine's loopback. Physical devices cannot use it.

## Related Deep Dives

- [gradle_dependencies.md](L2/gradle_dependencies.md) — Agora SDK Maven coordinates, version catalog, and dependency resolution.
