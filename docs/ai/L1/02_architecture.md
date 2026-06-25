# 02 · Architecture

> Three-layer topology: the Android app drives RTC + RTM, fetches config and manages the agent session via the Python token service, and receives live transcript events from the ConversationalAIAPI toolkit over RTM.

## Topology

```
Android App (Compose UI + CallViewModel)
  │  GET /get_config  ──────────────────────────────────────────┐
  │  POST /startAgent ──────────────────────────────────────────┤
  │  POST /stopAgent  ──────────────────────────────────────────┤
  │                                                             ▼
  │                                               Token Service (server/, :8000)
  │                                                 │  generate_convo_ai_token()
  │                                                 │  DeepgramSTT → OpenAI → MiniMaxTTS
  │                                                 ▼
  │                                               Agora ConvoAI Cloud
  │  RTC audio ◀──────────────────────────────────────────────▶ (agent audio channel)
  │  RTM messages ◀───────────────────────────────────────────── transcript / state / metrics
  ▼
ConversationalAIAPIImpl (vendored toolkit)
  → onTranscriptUpdated / onAgentStateChanged → CallViewModel → Compose UI
```

- **`android/`** — Kotlin / Jetpack Compose. `CallViewModel` owns business logic; `AgoraSession` owns the Agora SDK lifecycle (RTC engine + RTM client + toolkit).
- **`server/`** — Python FastAPI (:8000). Owns token generation (`generate_convo_ai_token`) and agent session lifecycle (`Agent.start()` / `Agent.stop()`).
- **Vendored toolkit** — `convoaiApi/` (copied MIT source). Attaches to the live `RtcEngine` + `RtmClient` and parses RTM messages into structured transcript/state callbacks.

## Request lifecycle

1. App `GET /get_config` → server mints a Token007 RTC+RTM token, returns `{app_id, token, uid, channel_name, agent_uid}`.
2. App creates `RtcEngine` + `RtmClient`, logs in to RTM, calls `loadAudioSettings()`, joins the RTC channel, then calls `subscribeMessage(channelName)`.
3. App `POST /startAgent {channelName, rtcUid=agent_uid, userUid=uid}` → server starts managed cascade (DeepgramSTT → OpenAI → MiniMaxTTS) and returns `agent_id`.
4. Agent audio streams into the channel; RTM delivers transcript/state/metrics to the toolkit.
5. App `POST /stopAgent {agentId}` → `session.stop()` → RTC `leaveChannel` → RTM `logout`.

## Why subscribe before startAgent

The toolkit's `subscribeMessage(channelName)` must complete **before** `POST /startAgent` is called. If the agent sends its greeting before RTM is subscribed, the first transcript events are lost. `AgoraSession.start()` enforces this ordering; see [07_gotchas](07_gotchas.md).

## Pipeline (server-side)

`DeepgramSTT(nova-3, en)` → `OpenAI` (Agora-managed, keyless by default) → `MiniMaxTTS`. The pipeline is a cascading STT→LLM→TTS set of vendors; no separate `llm/` service. VAD is configured on `AgoraAgent` turn detection.

## Key abstractions

| Class / File | Where | Responsibility |
| ------------ | ----- | -------------- |
| `CallViewModel` | `android/` | `StateFlow` state machine, coroutine orchestration, `BackendApi` calls |
| `AgoraSession` | `android/` | RTC engine + RTM client lifecycle, `IConversationalAIAPIEventHandler` |
| `BackendApi` | `android/` | OkHttp REST client; decodes `{code, data}` envelope |
| `ConversationalAIAPIImpl` | `android/convoaiApi/` | Vendored toolkit; parses RTM messages into callbacks |
| `Agent` | `server/src/` | `AgoraAgent` construction, session map, start/stop |
| `server.py` | `server/src/` | FastAPI routes, token issuance, error mapping |

## Related Deep Dives

- [sdk_session_integration.md](L2/sdk_session_integration.md) — RTC/RTM/toolkit wiring, ordering constraints, and teardown sequence.
