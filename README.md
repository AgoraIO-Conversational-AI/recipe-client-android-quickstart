# recipe-client-android-quickstart

A native **Android (Kotlin + Jetpack Compose)** voice-agent quickstart. It talks to a bundled,
**key-less** Python backend and renders the live transcript using Agora's official
`ConversationalAIAPI` Kotlin toolkit. The agent greets you on join — no human speech is
required to see it working.

The app owns the Agora **RTC + RTM** lifecycle; the toolkit parses the agent's messages
into transcript and agent-state callbacks.

## Layout

- `server/` — the reused key-less Python/FastAPI backend (`GET /get_config`, `POST /startAgent`,
  `POST /stopAgent`). A managed cascade means you only need `AGORA_APP_ID` / `AGORA_APP_CERTIFICATE`.
- `android/` — the Gradle / Kotlin / Compose app, with Agora RTC, RTM, and Agent Client Toolkit
  dependencies from Maven Central.

## Zero-key

No Agora secret ships in the app. The backend mints the RTC/RTM token and starts the managed agent;
the only secrets it needs are `AGORA_APP_ID` and `AGORA_APP_CERTIFICATE` in `server/.env.local`.

## Run it

### 1. Backend

```bash
cd server
uv venv venv && . venv/bin/activate
uv pip install -r requirements.txt -r requirements-dev.txt
python src/server.py        # serves on 0.0.0.0:8000
```

(or `pytest -q` to run the server tests).

### 2. Android app

```bash
cd android
./gradlew installDebug      # to a running emulator or a connected device
```

…or open `android/` in Android Studio and Run.

The app reads the backend URL from the `AGENT_BACKEND_URL` build-config field, default
`http://10.0.2.2:8000` — **`10.0.2.2` is the Android emulator's alias for the host machine**, so the
emulator reaches the server running on your laptop. A **physical device** must instead point at the
host's LAN IP (edit `AGENT_BACKEND_URL` in `android/app/build.gradle.kts`).

`RECORD_AUDIO` + `INTERNET` permissions are requested; cleartext traffic is allowed for the dev
localhost.

## How it works (lifecycle)

1. `GET /get_config` → `{app_id, token, uid, channel_name, agent_uid}`.
2. Create the RTC engine; `loadAudioSettings()`; **join with `ChannelMediaOptions`** —
   `publishMicrophoneTrack = true`, `autoSubscribeAudio = true`, `clientRoleType = BROADCASTER`.
3. Create + **log in** the RTM client.
4. `ConversationalAIAPIConfig(rtcEngine, rtmClient, renderMode = Text)` → `ConversationalAIAPIImpl`;
   `addHandler(...)`; **`subscribeMessage(channelName)` BEFORE `POST /startAgent`**.
5. `POST /startAgent` with `rtcUid = agent_uid`, `userUid = uid`.
6. `onTranscriptUpdated` rows are **upserted by `(turnId, type)`** (a turn has separate user + agent
   rows); the Compose `LazyColumn` is keyed by the composite `"$turnId-$type"`.
7. End: `unsubscribeMessage` → `POST /stopAgent` → `leaveChannel` → RTM `logout` → `destroy`.

The toolkit is configured with **`TranscriptRenderMode.Text`** (full-text rendering) for this pilot.

## SDKs

Resolved from Maven Central (+ jitpack):

- `io.agora.rtc:full-sdk:4.5.1`
- `io.agora:agora-rtm:2.2.3`
- `io.agora.agents:agora-agent-client-toolkit:2.9.0`

The Android app requires API 26 or newer, matching the Agent Client Toolkit 2.9.0 AAR metadata.

## Agent Client Toolkit

The transcript and agent-event implementation comes from the published
`io.agora.agents:agora-agent-client-toolkit:2.9.0` Maven artifact. Application code imports its
public API from `io.agora.conversational.api`.

## License

MIT — see [`LICENSE`](LICENSE).
