---
recipe_version: 1.0.0
recipe_status: experimental
extension_points:
  - id: android.ui
    name: Compose screens, ViewModel state, and navigation
  - id: android.session
    name: AgoraSession RTC/RTM options, audio settings, mute behavior
  - id: server.agent-pipeline
    name: STT/LLM/TTS vendor configuration, greeting, and VAD turn detection
  - id: server.routes
    name: Token service REST routes and response envelope
invariants:
  - id: secrets.server-only
    summary: AGORA_APP_CERTIFICATE stays in the server; the app receives only a short-lived Token007.
  - id: subscribe-before-start
    summary: subscribeMessage(channelName) must complete before POST /startAgent to avoid lost transcript events.
  - id: load-audio-before-join
    summary: loadAudioSettings() must be called before joinChannel.
  - id: broadcaster-role
    summary: The app joins as CLIENT_ROLE_BROADCASTER with publishMicrophoneTrack=true so the agent can hear the user.
  - id: token.uid-concrete
    summary: Backend resolves missing, zero, or negative UIDs before issuing a token.
stable_contracts:
  - id: env.required
    summary: AGORA_APP_ID and AGORA_APP_CERTIFICATE are required server-side.
  - id: api.core-routes
    summary: GET /get_config, POST /startAgent, and POST /stopAgent remain the client-facing contract.
  - id: response.envelope
    summary: Successful backend responses use { code, data } or { code } without data.
  - id: android.build-config
    summary: AGENT_BACKEND_URL is injected as a buildConfigField; changing it requires a Gradle rebuild.
---

# Recipe Contract

This base recipe defines the reusable surface for an Android Kotlin/Compose client with a Python FastAPI token service for Agora Conversational AI.

## Recipe Role

- Role: `base` recipe (self-contained, clone-and-run; no `Extends` pin).
- Target audience: developers building an Android voice-agent app with Agora ConvoAI and a managed STT→LLM→TTS backend pipeline.
- Reuse model: clone, set `AGORA_APP_ID` + `AGORA_APP_CERTIFICATE` in `server/.env.local`, run backend, build and install APK.

## Recipe Scope

- Python FastAPI token generation and managed agent lifecycle (DeepgramSTT → OpenAI → MiniMaxTTS, keyless).
- Android Kotlin/Compose UI: `LandingScreen` (permission + connect), `CallScreen` (transcript + mute/end), `CallViewModel` (state machine).
- `AgoraSession`: RTC engine + RTM client + vendored ConversationalAIAPI toolkit lifecycle.
- `BackendApi`: OkHttp REST client with `{ code, data }` envelope decoding.

## Baseline Implementation Guidance

Use this repo's source and progressive disclosure docs as the starting point. Do not recreate the Agora SDK lifecycle from memory — the ordering constraints (subscribe before startAgent, loadAudioSettings before join, BROADCASTER role) are non-obvious and verified in the iOS quickstart before being applied here.

## Extension Points

| ID | Surface | How to extend | Required follow-up |
| -- | ------- | ------------- | ------------------ |
| `android.ui` | `ui/LandingScreen.kt`, `ui/CallScreen.kt`, `CallViewModel.kt` | Add Compose screens, new `StateFlow`s, or ViewModel functions. | Preserve session lifecycle ownership in `AgoraSession`; do not call SDK methods from Compose directly. |
| `android.session` | `AgoraSession.kt` | Change `ChannelMediaOptions`, audio scenario, mute strategy, or add callbacks. | Keep ordering constraints: loadAudioSettings → joinChannel → subscribeMessage → startAgent. |
| `server.agent-pipeline` | `server/src/agent.py` | Change STT, LLM, TTS vendors, greeting, VAD config, or session parameters. | Run `cd server && pytest -q` after changes. |
| `server.routes` | `server/src/server.py` | Add FastAPI routes. | Add corresponding method in `BackendApi.kt` + `MockWebServer` unit test. |

## Invariants

- `AGORA_APP_CERTIFICATE` never reaches the Android app; only Token007 is returned.
- `subscribeMessage(channelName)` completes before `POST /startAgent` — first transcript events would be lost otherwise.
- `loadAudioSettings()` is called before `rtc.joinChannel()`.
- The app joins as `CLIENT_ROLE_BROADCASTER` with `publishMicrophoneTrack = true`.
- Backend resolves UID ≤ 0 to a random concrete UID before token issuance.

## Stable Contracts

| Contract | Stable shape |
| -------- | ------------ |
| Required server env | `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE` |
| Optional server env | `OPENAI_MODEL`, `OPENAI_API_KEY`, `AGENT_GREETING` |
| Android build config | `AGENT_BACKEND_URL` (default `http://10.0.2.2:8000`) |
| `GET /get_config` | Query `channel?`, `uid?`; returns `data.app_id`, `data.token`, `data.uid`, `data.channel_name`, `data.agent_uid`. |
| `POST /startAgent` | Body `{ channelName, rtcUid, userUid, parameters? }`; returns `data.agent_id`, `data.channel_name`, `data.status`. |
| `POST /stopAgent` | Body `{ agentId }`; returns `{ code: 0, msg: "success" }`. |
| Success envelope | `{ "code": 0, "msg": "success", "data": ... }` where the route has data. |

## Internal / Subject to Change

- Compose layout, colors, typography, and screen composition.
- Exact STT/TTS vendor model names and voice IDs.
- In-memory `Agent._sessions`; the stable behavior is start by channel/user and stop by returned `agent_id`.
- Vendored toolkit internals in `convoaiApi/`; the stable surface is `IConversationalAIAPI` + `IConversationalAIAPIEventHandler`.

## Related Progressive Disclosure Docs

- `L1/01_setup.md` — setup, env, and build commands.
- `L1/02_architecture.md` — topology and session lifecycle.
- `L1/05_workflows.md` — common modification workflows.
- `L1/06_interfaces.md` — route, env, and SDK contracts.
- `L1/L2/sdk_session_integration.md` — full RTC/RTM/toolkit wiring detail.
