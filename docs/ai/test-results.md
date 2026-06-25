# Progressive Disclosure — Test Results

> Test run for `recipe-client-android-quickstart` progressive disclosure docs.
> Date: 2026-06-25 · Standard: AgoraIO-Community/ai-devkit progressive-disclosure.

## Step 1 — Structural checks

| Check                                               | Result |
| --------------------------------------------------- | ------ |
| `L0_repo_card.md` ≤ 50 lines                        | Pass (36) |
| All 8 L1 files present                              | Pass |
| Each L1 has purpose blockquote + Related Deep Dives | Pass |
| L2 `_index.md` present                              | Pass |
| Each L2 opens with "When to Read This" callout      | Pass (2/2) |
| Relative links resolve (`docs/ai/` + AGENTS.md)     | Pass (35/35, 0 broken) |
| AGENTS.md has How to Load / Git Conventions / Doc Commands | Pass |

**Note on L1 line counts:** files are table-dense and information-complete but
run 43–75 lines, under the 80–200 soft target. The standard favors tables over
prose and warns against bloat, so they were left concise rather than padded.
Accepted deviation; revisit if a section needs more depth.

## Step 2/3 — Question runs

Questions span the five standard categories. Each answer was checked against the
repo source before being marked Pass. "Level" is the lowest disclosure level
that fully answers the question.

### Setup & Build

| # | Question | Expected answer | Source of truth | Level | Status |
|---|----------|-----------------|-----------------|-------|--------|
| 1 | How do I run the backend locally? | `cd server && uv venv venv && . venv/bin/activate && uv pip install -r requirements.txt -r requirements-dev.txt && python src/server.py` (serves on 0.0.0.0:8000). | `L1/01_setup.md` ↔ `README.md` | L1 | Pass |
| 2 | Which server env vars are required? | `AGORA_APP_ID` and `AGORA_APP_CERTIFICATE` only. No LLM key required (keyless pipeline). | `L1/01_setup.md`, `06_interfaces.md` ↔ `server/src/agent.py` | L1 | Pass |
| 3 | How do I run the Android app on an emulator? | `cd android && ./gradlew installDebug`; backend must be running; emulator uses `http://10.0.2.2:8000` by default. | `L1/01_setup.md` ↔ `android/app/build.gradle.kts` | L1 | Pass |

### Test & Run

| # | Question | Expected answer | Source of truth | Level | Status |
|---|----------|-----------------|-----------------|-------|--------|
| 4 | How do I run server tests without cloud creds? | `cd server && pytest -q`; `conftest.py` fakes env + stubs `load_dotenv`. | `L1/04_conventions.md`, `01_setup.md` ↔ `tests/conftest.py` | L1 | Pass |
| 5 | How do I run Android tests without an emulator? | `cd android && ./gradlew testDebugUnitTest`; uses `MockWebServer` (OkHttp), no emulator needed. | `L1/04_conventions.md` ↔ `BackendApiTest.kt` | L1 | Pass |
| 6 | Are there server tests currently passing? | `test_config.py` passes; `test_agent_construction.py` fails due to pre-existing `AgoraAgent(client=...)` API mismatch from an incomplete 2.3.x migration. Noted below. | `tests/test_agent_construction.py` ↔ `server/src/agent.py` | L1 | Pass (accurately documented) |

### Conventions

| # | Question | Expected answer | Source of truth | Level | Status |
|---|----------|-----------------|-----------------|-------|--------|
| 7 | What response shape do backend routes use? | `{ code, msg, data }`; `data` omitted when no payload. Android `BackendApi` treats non-zero `code` as an error. | `L1/04_conventions.md`, `06_interfaces.md` ↔ `server.py` | L1 | Pass |
| 8 | How are errors mapped to HTTP codes in the backend? | `ValueError→400`, `RuntimeError→500`, else 500 via `_to_http_error`. | `L1/04_conventions.md` ↔ `server/src/server.py` | L1 | Pass |
| 9 | What are the commit/branch conventions? | Conventional commits `type: description`; branches `type/short-description`; no AI tool names; no Co-Authored-By. | `AGENTS.md` Git Conventions | L1 | Pass |

### Development

| # | Question | Expected answer | Source of truth | Level | Status |
|---|----------|-----------------|-----------------|-------|--------|
| 10 | How do I point the Android app at a physical device backend? | Edit `buildConfigField("String", "AGENT_BACKEND_URL", "\"http://<LAN-IP>:8000\"")` in `android/app/build.gradle.kts` and rebuild. | `L1/05_workflows.md`, `07_gotchas.md` ↔ `android/app/build.gradle.kts` | L1 | Pass |
| 11 | How do I change the agent prompt or greeting? | Set `AGENT_GREETING` in `server/.env.local`; or edit the default string in `server/src/agent.py`. | `L1/05_workflows.md` ↔ `agent.py` | L1 | Pass |
| 12 | Where does token generation live and what secret must not leave there? | `generate_convo_ai_token` in `server/src/server.py`; `AGORA_APP_CERTIFICATE` stays on the server; app receives only a Token007. | `L1/02_architecture.md`, `08_security.md` ↔ `server.py`, `agent.py` | L1 | Pass |

### Deep Dive

| # | Question | Expected answer | Source of truth | Level | Status |
|---|----------|-----------------|-----------------|-------|--------|
| 13 | What is the strict initialization order for the Android Agora session? | (1) RtcEngine → (2) RtmClient → (3) ConversationalAIAPIImpl → (4) addHandler → (5) rtm.login → inside callback: loadAudioSettings → joinChannel → subscribeMessage → then POST /startAgent. | `L2/sdk_session_integration.md` ↔ `AgoraSession.kt` | L2 | Pass |
| 14 | Why must `subscribeMessage` complete before `POST /startAgent`? | Agent sends its greeting immediately on start via RTM; events arrive before subscribe completes and are silently dropped. `AgoraSession.start()` serializes this via the `subscribeMessage` success callback. | `L2/sdk_session_integration.md`, `07_gotchas.md` ↔ `AgoraSession.kt` | L2 | Pass |
| 15 | How do I upgrade the Agora RTC or RTM SDK version? | Bump `agoraRtc`/`agoraRtm` in `android/gradle/libs.versions.toml`; re-vendor `convoaiApi/` if toolkit API has drifted; run `./gradlew assembleDebug testDebugUnitTest`. | `L2/gradle_dependencies.md` ↔ `libs.versions.toml` | L2 | Pass |

## Step 4 — Analysis

- All 15 questions answered at the expected disclosure level (12 at L1, 3 at L2).
- No missing-coverage findings; no broken references (35 links checked, 0 broken).
- One soft deviation: L1 line counts below the 80–200 target (accepted; concise/table-dense).
- Android build verification (`./gradlew assembleDebug`) was not run — Android toolchain not available in this environment. `./gradlew testDebugUnitTest` was not run for the same reason; `BackendApiTest.kt` uses `MockWebServer` and does not require an emulator.
- Pre-existing server test failure noted: `test_agent_construction.py` fails with `TypeError: Agent.__init__() got an unexpected keyword argument 'client'` due to an incomplete agora-agents 2.3.x migration in `server/src/agent.py`. This is a pre-existing code issue, not a documentation issue. `test_config.py` passes (1 passed).

## Step 5 — Summary

| Category       | Questions | Pass | Notes |
| -------------- | :-------: | :--: | ----- |
| Setup & Build  | 3 | 3 | — |
| Test & Run     | 3 | 3 | server test pre-existing failure documented; Android tests not run (no toolchain) |
| Conventions    | 3 | 3 | — |
| Development    | 3 | 3 | — |
| Deep Dive      | 3 | 3 | resolved at L2 as designed |
| **Total**      | **15** | **15** | — |

## Step 6 — Fixes / Retest

No failing doc questions; no doc fixes required. Evidence executed during this run:

- `pytest -q` (server venv) → `1 passed, 1 failed`; failure is pre-existing code issue in `agent.py`, not in docs.
- Relative link check → `35 checked, 0 broken`.
- Android build/unit tests not executed (Android toolchain unavailable in this environment).
