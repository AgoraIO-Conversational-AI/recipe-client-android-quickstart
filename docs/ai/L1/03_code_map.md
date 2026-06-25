# 03 · Code Map

> Where things live. Two top-level modules: `android/` (Kotlin/Compose app) and `server/` (FastAPI token service). No shared orchestration script — each module runs independently.

## Root

| Path                  | Responsibility                                                             |
| --------------------- | -------------------------------------------------------------------------- |
| `README.md`           | Layout, run steps, lifecycle walkthrough, SDK versions.                    |
| `AGENTS.md`           | Coding-agent handbook + How to Load / Git Conventions / Doc Commands.      |
| `Dockerfile`          | Backend-only image (`python:3.12-slim`, port 8000).                        |
| `.github/workflows/`  | `ci.yml` (server pytest + android assembleDebug + unit tests), `docker.yml`. |
| `NOTICE`              | Third-party MIT attribution for the vendored toolkit.                      |

## `server/` — FastAPI token service (:8000)

| Path                              | Responsibility                                                           |
| --------------------------------- | ------------------------------------------------------------------------ |
| `src/server.py`                   | FastAPI app, CORS, route handlers (`/get_config`, `/startAgent`, `/stopAgent`), error mapping, uvicorn entrypoint. |
| `src/agent.py`                    | `Agent` class: `AsyncAgora` client, cascading vendors, `start()`/`stop()`, `_sessions` map. |
| `scripts/run_fake_server.py`      | Boots `server.app` with `FakeAgent` for integration smoke tests.         |
| `tests/test_agent_construction.py`| Builds the real `AgoraAgent`, fakes the SDK session, asserts response shape. |
| `tests/test_config.py`            | `Agent` construction smoke with stub creds.                              |
| `tests/conftest.py`               | `fake_env` fixture; no cloud, no real creds.                             |
| `requirements.txt`                | Runtime deps (`fastapi`, `uvicorn`, `agora-agents>=2.3.0`).              |
| `requirements-dev.txt`            | Dev deps (`pytest`, `httpx`).                                            |

## `android/` — Kotlin / Compose app

| Path                              | Responsibility                                                           |
| --------------------------------- | ------------------------------------------------------------------------ |
| `app/build.gradle.kts`            | SDK dependencies, `AGENT_BACKEND_URL` `buildConfigField`, compileSdk 35. |
| `gradle/libs.versions.toml`       | Version catalog: AGP 8.5.2, Kotlin 2.0.20, RTC 4.5.1, RTM 2.2.3.       |
| `settings.gradle.kts`             | Maven repos (Google, MavenCentral, jitpack), single `:app` module.       |
| `app/src/main/AndroidManifest.xml`| `INTERNET` + `RECORD_AUDIO` permissions; `usesCleartextTraffic="true"`.  |

### `app/src/main/java/io/agora/recipe/androidquickstart/`

| File                 | Responsibility                                                              |
| -------------------- | --------------------------------------------------------------------------- |
| `MainActivity.kt`    | Single-activity entry point; phase-driven screen routing (Landing / Call).  |
| `CallViewModel.kt`   | `AndroidViewModel`; `StateFlow` for phase, turns, mic, agent state, error; coroutine orchestration. |
| `AgoraSession.kt`    | Creates `RtcEngineEx` + `RtmClient`; wires toolkit; implements `IConversationalAIAPIEventHandler`. |
| `BackendApi.kt`      | OkHttp synchronous client; `@Serializable` envelopes for `/get_config`, `/startAgent`, `/stopAgent`. |

### `app/src/main/java/.../ui/`

| File              | Responsibility                       |
| ----------------- | ------------------------------------ |
| `LandingScreen.kt`| Permission request; Connect button.  |
| `CallScreen.kt`   | Transcript `LazyColumn`, Mute/End controls. |

### `app/src/main/java/io/agora/scene/convoai/convoaiApi/` (vendored, do not modify)

| Path               | Responsibility                                                              |
| ------------------ | --------------------------------------------------------------------------- |
| `IConversationalAIAPI.kt`       | Public interface + callback types (`Transcript`, `StateChangeEvent`, etc.). |
| `ConversationalAIAPIImpl.kt`    | RTM subscription, message dispatch, handler management.                     |
| `subRender/v1/`, `subRender/v3/`| RTM message parsers (v1 word-level, v3 full-text transcript modes).         |
| `ConversationalAIUtils.kt`      | Shared utility helpers.                                                     |

### `app/src/test/`

| File               | Responsibility                                                 |
| ------------------ | -------------------------------------------------------------- |
| `BackendApiTest.kt`| JVM unit tests for `BackendApi` using `MockWebServer`.         |

## Related Deep Dives

- None. For runtime flow see [02_architecture](02_architecture.md); for contracts see [06_interfaces](06_interfaces.md).
