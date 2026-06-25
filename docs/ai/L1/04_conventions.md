# 04 · Conventions

> Coding patterns for both the Android app and the Python server. Follow these to keep the app and backend aligned.

## Android (Kotlin / Compose)

- **ViewModel owns state** — `CallViewModel` holds all `StateFlow`s (`phase`, `turns`, `micMuted`, `agentState`, `errorMessage`). Compose screens are stateless; they only read flows and invoke ViewModel functions.
- **IO on `Dispatchers.IO`** — all `BackendApi` calls (`getConfig`, `startAgent`, `stopAgent`) are wrapped in `withContext(Dispatchers.IO)` inside `viewModelScope.launch`.
- **Callback → coroutine bridging** — `AgoraSession.start()` accepts a `onReady(error?)` callback; `CallViewModel` resumes its coroutine from that callback.
- **Upsert by `(turnId, type)`** — `CallViewModel.upsertTranscript` deduplicates by `(turnId, type)`. A turn has separate `USER` and `AGENT` rows; the composite key `"$turnId-${type}"` is used as the `LazyColumn` item key.
- **`adjustRecordingSignalVolume`** — mic mute is implemented by setting recording volume to 0 (mute) or 100 (restore), not by toggling `publishMicrophoneTrack`.
- **Vendored toolkit** — `convoaiApi/` is copied verbatim (MIT). Do not modify it. A `CovLogger` shim in `io.agora.scene.convoai` routes its internal logging to `android.util.Log`.

## Android build

- Kotlin 2.0.20 + AGP 8.5.2; Compose BOM 2024.09.02; `kotlinOptions.jvmTarget = "17"`.
- Dependencies declared in `gradle/libs.versions.toml` version catalog; reference via `libs.*` aliases.
- `AGENT_BACKEND_URL` is a `buildConfigField`; change it in `android/app/build.gradle.kts`, not at runtime.
- `usesCleartextTraffic="true"` is set in the manifest for local HTTP dev. Remove or scope with a `network_security_config` before production.

## Backend (Python / FastAPI)

- Async throughout: route handlers are `async def`; agent uses `AsyncAgora` and `create_async_session`.
- Pydantic models for request bodies (`StartAgentRequest`, `StopAgentRequest`). Field names are **camelCase** (`channelName`, `rtcUid`, `userUid`) to match the Android client.
- Error mapping is centralized: `_to_http_error()` maps `ValueError → 400`, `RuntimeError → 500`, else 500. Raise plain `ValueError`/`RuntimeError` in business logic; let the route convert.
- Env loaded from `server/.env.local` then `server/.env` with `override=True`. Use `os.getenv`; don't hard-code credentials.

## Response envelope

All backend JSON responses use:

```json
{ "code": 0, "msg": "success", "data": { } }
```

`data` is present only when the route returns a payload. The Android `BackendApi` treats a missing or non-zero `code` as a decoding error.

## Testing approach

- **Android**: JVM unit tests in `app/src/test/` use `MockWebServer` (OkHttp); no emulator required. Run with `./gradlew testDebugUnitTest`.
- **Server**: `pytest` in `server/`; `conftest.py` provides `fake_env` fixture with stub credentials; no cloud, no real creds needed. `test_agent_construction.py` fakes the SDK session to exercise the real `AgoraAgent` build path.

## Doc upkeep

When you change request/response contracts, env vars, or workflow, update the Android client, server, README, **and** the matching `docs/ai/L1/` file together, then bump `Last Reviewed` in [L0](../L0_repo_card.md).

## Related Deep Dives

- None.
