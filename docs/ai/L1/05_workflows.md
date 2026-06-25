# 05 · Workflows

> Step-by-step guides for common changes in this recipe. Each ends with the narrowest verify command to run.

## Add a new screen or UI control

1. Create a `@Composable` in `android/app/src/main/java/.../ui/`.
2. Add state to `CallViewModel` as a new `MutableStateFlow`; expose it as `StateFlow`.
3. Update the `when (phase)` dispatch in `MainActivity.kt` if routing changes.
4. Verify: `./gradlew testDebugUnitTest` (for ViewModel logic); open in Android Studio and run on device/emulator for visual check.

## Change the token service backend URL

The backend URL is baked into the APK as a `buildConfigField`:

1. Edit `android/app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "AGENT_BACKEND_URL", "\"http://192.168.1.42:8000\"")
   ```
2. Rebuild: `./gradlew assembleDebug`.

For emulator, use `http://10.0.2.2:8000` (default). For a physical device, use the host LAN IP.

## Change the agent prompt / greeting / model

1. Greeting: set `AGENT_GREETING` in `server/.env.local`, or edit the default in `server/src/agent.py`.
2. Model: set `OPENAI_MODEL` (default `gpt-4o-mini`).
3. STT/TTS vendors: edit `Agent.start()` in `server/src/agent.py` — `DeepgramSTT` and `MiniMaxTTS` config.
4. Verify: `cd server && pytest -q`.

## Add a new server route

1. Add the FastAPI handler in `server/src/server.py` (return `{code, msg, data}` envelope).
2. Add a corresponding method in `android/.../BackendApi.kt`.
3. Call from `CallViewModel` via `withContext(Dispatchers.IO)`.
4. Verify: add a `MockWebServer` test in `android/.../BackendApiTest.kt`; run `./gradlew testDebugUnitTest`.

## Build and run locally (combined)

```bash
# Terminal 1 — backend
cd server && uv venv venv && . venv/bin/activate
uv pip install -r requirements.txt -r requirements-dev.txt
python src/server.py

# Terminal 2 — Android (emulator must be running or device connected)
cd android && ./gradlew installDebug
```

## Run server tests (no creds, no cloud)

```bash
cd server && pytest -q
```

## Verify before finishing

| Change touches…              | Run                                           |
| ---------------------------- | --------------------------------------------- |
| Android UI / ViewModel only  | `./gradlew testDebugUnitTest`                 |
| Backend logic / vendors      | `cd server && pytest -q`                      |
| Backend API contract         | Add `MockWebServer` test + `./gradlew testDebugUnitTest` |
| End-to-end (manual)          | Start server, install APK, tap Connect        |

## Deploy

1. Deploy `server/` (or use the backend Docker image `ghcr.io/AgoraIO-Conversational-AI/recipe-client-android-quickstart` on `v*` tags) to a reachable host.
2. Set `AGORA_APP_ID` and `AGORA_APP_CERTIFICATE` in the backend environment.
3. Update `AGENT_BACKEND_URL` in `android/app/build.gradle.kts` to the deployed URL.
4. Build a release APK / AAB and distribute normally.

## Related Deep Dives

- [sdk_session_integration.md](L2/sdk_session_integration.md) — RTC/RTM/toolkit wiring detail.
- [gradle_dependencies.md](L2/gradle_dependencies.md) — SDK version management.
