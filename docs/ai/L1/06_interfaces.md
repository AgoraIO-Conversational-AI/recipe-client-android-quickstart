# 06 · Interfaces

> Boundary contracts: token-service REST routes, the Android `BackendApi` surface, env vars, the response envelope, and key Agora SDK entry points.

## Token service routes (port 8000)

The Android app calls these directly via `BackendApi`.

### `GET /get_config`

- Query (optional): `channel?: string`, `uid?: int` (≤ 0 or missing → server generates one).
- Returns `data`: `{ app_id, token, uid (string), channel_name, agent_uid (string) }`.
- Token is a Token007 RTC+RTM combined token, expiry 3600s, for a concrete non-zero UID.

### `POST /startAgent`

- Body: `{ channelName: string, rtcUid: int, userUid: int, parameters?: object }`.
  - `parameters.output_audio_codec?: string` — optional codec override.
- Returns `data`: `{ agent_id, channel_name, status: "started" }`.
- 400 if `channelName`, `agent_uid`, or `user_uid` invalid (empty / non-positive).

### `POST /stopAgent`

- Body: `{ agentId: string }`.
- Returns `{ code: 0, msg: "success" }` (no `data`).

## Response envelope

```json
{ "code": 0, "msg": "success", "data": { } }
```

`data` is omitted when the route has no payload. The Android `BackendApi` deserializes via `@Serializable` data classes; decoding fails on non-zero `code` or missing fields.

## Android `BackendApi` interface

| Method | HTTP call | Returns |
| ------ | --------- | ------- |
| `getConfig(): AgentConfig` | `GET /get_config?uid=0` | `AgentConfig(app_id, token, uid, channel_name, agent_uid)` |
| `startAgent(channelName, rtcUid, userUid): String` | `POST /startAgent` | `agent_id` string |
| `stopAgent(agentId: String)` | `POST /stopAgent` | Unit (fires and completes) |

`BackendApi` is synchronous (blocking OkHttp); always call from `Dispatchers.IO`.

## Agora SDK surface (used in `AgoraSession`)

| SDK class / call | Purpose |
| ---------------- | ------- |
| `RtcEngine.create(RtcEngineConfig)` | Create the RTC engine; must set `mAudioScenario = AUDIO_SCENARIO_AI_CLIENT`. |
| `rtc.joinChannel(token, channel, uid, ChannelMediaOptions)` | Join with `publishMicrophoneTrack=true`, `autoSubscribeAudio=true`, `clientRoleType=BROADCASTER`. |
| `RtmClient.create(RtmConfig)` | Create the RTM client; `uid` must be the same integer as RTC. |
| `rtm.login(token, callback)` | Authenticate RTM; RTC join and toolkit subscribe happen inside the success callback. |
| `ConversationalAIAPIImpl(ConversationalAIAPIConfig)` | Create the toolkit; `renderMode=TranscriptRenderMode.Text`. |
| `api.loadAudioSettings(audioScenario)` | Must be called **before** `rtc.joinChannel`. |
| `api.subscribeMessage(channelName, callback)` | Must complete **before** `POST /startAgent`. |
| `api.unsubscribeMessage(channelName, callback)` | Call first in teardown. |
| `rtc.adjustRecordingSignalVolume(0 or 100)` | Mute (0) / unmute (100) the mic without re-publishing. |
| `RtcEngine.destroy()` + `RtmClient.release()` | Final cleanup; called after leaveChannel + RTM logout. |

## Environment variables

| Variable                | Scope      | Required | Default          | Notes |
| ----------------------- | ---------- | :------: | ---------------- | ----- |
| `AGORA_APP_ID`          | server     |    ✅    | —                | Agora Console App ID |
| `AGORA_APP_CERTIFICATE` | server     |    ✅    | —                | Never sent to app |
| `OPENAI_MODEL`          | server     |          | `gpt-4o-mini`    | Agora-managed OpenAI vendor |
| `OPENAI_API_KEY`        | server     |          | —                | BYO only; optional |
| `AGENT_GREETING`        | server     |          | built-in line    | Opening utterance override |
| `AGENT_BACKEND_URL`     | Android build config |  | `http://10.0.2.2:8000` | Set in `android/app/build.gradle.kts` |
| `PORT`                  | server (env only) | | `8000`          | Read from env; set at runtime, not in `.env.local` |

## Related Deep Dives

- [sdk_session_integration.md](L2/sdk_session_integration.md) — detailed ordering and teardown of the RTC/RTM/toolkit lifecycle.
