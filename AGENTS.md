# Agent Development Guide

For coding agents working in `recipe-client-android-quickstart`. This repository is the
**Android client** recipe in the Agora Conversational AI recipes family.

## How to Load

This repository uses progressive disclosure documentation. Docs live under
`docs/ai/` in three levels.

1. Read [docs/ai/L0_repo_card.md](docs/ai/L0_repo_card.md) to identify the repo.
2. This repo declares `Recipe Role: base`; read [docs/ai/RECIPE.md](docs/ai/RECIPE.md) before changing reusable recipe contracts.
3. Load ALL 8 files in [docs/ai/L1/](docs/ai/L1/). They are small — load all upfront.
4. Follow L2 deep-dive links only when L1 isn't detailed enough. The index is at [docs/ai/L1/L2/_index.md](docs/ai/L1/L2/_index.md).

The sections below remain the canonical contributor handbook for hands-on work;
the `docs/ai/` tree is the structured summary used by AI agents.

## System shape

- **`server/`** — Python FastAPI token service (:8000). Owns Agora token generation and agent session lifecycle. Pipeline: `DeepgramSTT(nova-3, en)` → `OpenAI` (Agora-managed, keyless) → `MiniMaxTTS`. SDK: `agora-agents>=2.3.0`.
- **`android/`** — Kotlin / Jetpack Compose app. `CallViewModel` owns business state; `AgoraSession` owns the Agora SDK lifecycle (RTC + RTM + vendored ConversationalAIAPI toolkit).
- Auth: Token007 from `AGORA_APP_ID` + `AGORA_APP_CERTIFICATE` (server-side only).
- Zero-key: no LLM API key required; OpenAI vendor is Agora-managed.

## Pipeline

`DeepgramSTT` → `OpenAI` (Agora-managed) → `MiniMaxTTS`. Cascading STT/LLM/TTS vendors, not a single MLLM. VAD is configured on `AgoraAgent` turn detection in `server/src/agent.py`.

## Session ordering (critical)

The RTC/RTM/toolkit initialization order is strict:

1. Create `RtcEngine` + `RtmClient` + `ConversationalAIAPIImpl`.
2. `rtm.login(token)` — inside success callback:
   - `api.loadAudioSettings(AUDIO_SCENARIO_AI_CLIENT)` — BEFORE `joinChannel`.
   - `rtc.joinChannel(...)` as `CLIENT_ROLE_BROADCASTER`, `publishMicrophoneTrack=true`, `autoSubscribeAudio=true`.
   - `api.subscribeMessage(channelName)` — wait for success callback, THEN call `POST /startAgent`.

Violating this order causes lost transcript events or no audio. See [docs/ai/L1/07_gotchas.md](docs/ai/L1/07_gotchas.md) and [docs/ai/L1/L2/sdk_session_integration.md](docs/ai/L1/L2/sdk_session_integration.md).

## Emulator vs device

- **Emulator:** `AGENT_BACKEND_URL = http://10.0.2.2:8000` (default; `10.0.2.2` is the host loopback alias).
- **Physical device:** set `AGENT_BACKEND_URL` to the host's LAN IP in `android/app/build.gradle.kts`.

`AGENT_BACKEND_URL` is a `buildConfigField` — changing it requires a Gradle rebuild.

## Vendored toolkit

`android/app/src/main/java/io/agora/scene/convoai/convoaiApi/` is copied verbatim from
`AgoraIO-Community/Conversational-AI-Demo` (MIT). Do not modify it. A `CovLogger` shim routes
its logging to `android.util.Log`.

## Env vars

| Variable | Default | Notes |
|---|---|---|
| `AGORA_APP_ID` | — | required |
| `AGORA_APP_CERTIFICATE` | — | required; never sent to the app |
| `OPENAI_MODEL` | `gpt-4o-mini` | Agora-managed; no key needed |
| `OPENAI_API_KEY` | — | optional; BYO only |
| `AGENT_GREETING` | built-in | Optional opening line override |

## Patterns

- Keep `AGORA_APP_CERTIFICATE` and token generation in `server/`.
- Call `subscribeMessage` and wait for success before `POST /startAgent`.
- All `BackendApi` calls run on `Dispatchers.IO` inside `viewModelScope.launch`.
- Mic mute via `adjustRecordingSignalVolume(0/100)`; do not toggle `publishMicrophoneTrack` mid-call.

## Anti-patterns

- Do not call Agora SDK methods from Compose composables — route through `CallViewModel` → `AgoraSession`.
- Do not embed `AGORA_APP_CERTIFICATE` or any secret in the APK.
- Do not modify `convoaiApi/` — it is a vendored copy.
- Do not skip the `subscribeMessage` → `onReady` callback before calling `startAgent`.
- Do not call `joinChannel` before `loadAudioSettings`.

## Commands

```bash
# Backend
cd server
uv venv venv && . venv/bin/activate
uv pip install -r requirements.txt -r requirements-dev.txt
python src/server.py

# Backend tests (no cloud, no creds)
cd server && pytest -q

# Android (emulator or device connected)
cd android && ./gradlew installDebug

# Android unit tests (no emulator)
cd android && ./gradlew testDebugUnitTest

# Android build only
cd android && ./gradlew assembleDebug
```

## Done criteria

1. Run the narrowest relevant verification command.
2. Android-affecting changes: `./gradlew testDebugUnitTest` passes.
3. Backend-affecting changes: `cd server && pytest -q` passes.
4. If you change required env vars or setup steps, update the root README and the server README together.
5. If the change touches workflows, interfaces, gotchas, or security details, update the matching file under [docs/ai/L1/](docs/ai/L1/) and bump `Last Reviewed` in [docs/ai/L0_repo_card.md](docs/ai/L0_repo_card.md).

## Git Conventions

### Commit messages — conventional commits

- **Format:** `type: description` or `type(scope): description`
- **Types:** `feat:` (new feature), `fix:` (bug fix), `chore:` (maintenance, version bumps), `test:` (test additions/changes), `docs:` (documentation)
- **Scoped variant:** `feat(scope):`, `fix(scope):` — e.g. `fix(server): validate uid before token`
- **Lowercase after prefix** — `feat: add feature`, not `feat: Add feature`
- **Present tense** — "add feature", not "added feature"

### Branch names

- **Format:** `type/short-description` — lowercase, hyphen-separated
- **Types match commit types:** `feat/`, `fix/`, `chore/`, `test/`, `docs/`
- **Examples:** `feat/add-transcript-screen`, `fix/emulator-url`, `docs/progressive-disclosure`

### General rules

- **Repo-local `AGENTS.md` is the authoritative source for repo conventions.**
- **No AI tool names** — never mention claude, cursor, copilot, cody, aider, gemini, codex, chatgpt, or gpt-3/4 in commit messages or PR descriptions.
- **No Co-Authored-By trailers** — omit AI attribution lines.
- **No `--no-verify`** — let git hooks run normally.
- **No git config changes** — do not modify `user.name` or `user.email`.

## Doc Commands

| Command       | When to use                                                                  |
| ------------- | ---------------------------------------------------------------------------- |
| generate docs | No `docs/ai/` directory exists yet                                           |
| update docs   | Code changed since the `Last Reviewed` date in L0                            |
| test docs     | Verify docs give agents the right context (writes `docs/ai/test-results.md`) |
| fix docs      | Close findings from a docs review or test run                                |

See the [progressive disclosure standard](https://github.com/AgoraIO-Community/ai-devkit/blob/main/docs/standard/progressive-disclosure-standard.md) and [workflows](https://github.com/AgoraIO-Community/ai-devkit/blob/main/docs/workflows/progressive-disclosure-docs.md) for the full specification.
