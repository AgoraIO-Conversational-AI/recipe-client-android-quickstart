# 07 · Gotchas

> Non-obvious pitfalls specific to this Android + ConversationalAI quickstart. Read before changing the session setup, Gradle config, or server wiring.

## `subscribeMessage` must complete before `POST /startAgent`

If `subscribeMessage(channelName)` is not called (and its callback received) before the agent is started, the agent's greeting and early transcript events arrive on RTM before the client is subscribed — they are silently dropped. `AgoraSession.start()` calls `subscribeMessage` inside the RTM `login` success callback and only calls `onReady(null)` after subscribe succeeds. Do not reorder this.

## `loadAudioSettings` must precede `joinChannel`

`ConversationalAIAPIImpl.loadAudioSettings(audioScenario)` configures internal audio processing. Calling it after `joinChannel` may cause degraded audio or no-op behavior. In `AgoraSession.start()`, it is called immediately before `joinChannel` inside the RTM `login` success callback.

## Emulator uses `10.0.2.2`, not `localhost`

The Android emulator cannot reach `localhost` or `127.0.0.1` on the host machine. The host loopback is `10.0.2.2`. The default `AGENT_BACKEND_URL` is set to `http://10.0.2.2:8000` for this reason. Physical devices must use the host's actual LAN IP — `10.0.2.2` does not work on real hardware.

## `AGENT_BACKEND_URL` is a build-time constant

`AGENT_BACKEND_URL` is injected via `buildConfigField` in `android/app/build.gradle.kts`. Changing it requires a Gradle rebuild — there is no runtime override. For release builds pointing at a deployed backend, edit this field before building the release APK.

## `usesCleartextTraffic="true"` is only for local dev

The manifest sets `android:usesCleartextTraffic="true"` to allow HTTP to the local backend. This must be removed or replaced with a `network_security_config` scoped to debug builds before any production deployment.

## Vendored toolkit must not be modified

`convoaiApi/` is a vendored copy from `AgoraIO-Community/Conversational-AI-Demo` (MIT). Changes to it will be overwritten if the toolkit is re-vendored. If you need to customize behavior, either wrap the public interface or update the vendored copy and update `NOTICE`.

## Toolkit `renderMode` is `Text` only

`ConversationalAIAPIConfig` is constructed with `renderMode = TranscriptRenderMode.Text`. This activates the v3 transcript parser. Word-level (v1) mode is not wired in this recipe. Do not mix `renderMode.Word` with the existing `onTranscriptUpdated` handling.

## `adjustRecordingSignalVolume` for mute, not `publishMicrophoneTrack`

Mute is implemented via `rtcEngine?.adjustRecordingSignalVolume(0)` rather than toggling `publishMicrophoneTrack`. The app remains a BROADCASTER throughout the call; the signal is simply silenced. Toggling `publishMicrophoneTrack` mid-call causes unnecessary channel option re-negotiation.

## Server is shared with the SwiftUI recipe

`server/src/server.py` and `server/src/agent.py` were originally written for the iOS (SwiftUI) quickstart and re-used here. The FastAPI title and server README still mention "SwiftUI". This is cosmetic — the pipeline and routes are identical. Do not refactor the server title unless you intend to decouple the recipes.

## Related Deep Dives

- [sdk_session_integration.md](L2/sdk_session_integration.md) — full ordering and teardown sequence.
