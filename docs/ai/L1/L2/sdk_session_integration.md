# SDK Session Integration

**When to Read This:** You are modifying `AgoraSession.kt`, changing the RTC join options, debugging lost transcripts or the agent not hearing audio, or implementing a teardown variation.

## Overview

`AgoraSession` owns the full Agora SDK lifecycle: RTC engine + RTM client + ConversationalAIAPI toolkit. The ordering of SDK calls is strict and dictated by both Agora SDK requirements and the transcript delivery guarantee.

## Initialization order (inside `AgoraSession.start()`)

```
1. RtcEngine.create(RtcEngineConfig)
   - mAudioScenario = AUDIO_SCENARIO_AI_CLIENT
   - mChannelProfile = CHANNEL_PROFILE_LIVE_BROADCASTING

2. RtmClient.create(RtmConfig)
   - uid = rtcUid.toString()

3. ConversationalAIAPIImpl(ConversationalAIAPIConfig)
   - rtcEngine = rtc, rtmClient = rtm
   - renderMode = TranscriptRenderMode.Text

4. api.addHandler(this)  ← before RTM login

5. rtm.login(token) {
     // Inside success callback:
     6. api.loadAudioSettings(AUDIO_SCENARIO_AI_CLIENT)
     7. rtc.joinChannel(token, channelName, rtcUid, ChannelMediaOptions)
        - publishMicrophoneTrack = true
        - autoSubscribeAudio = true
        - clientRoleType = CLIENT_ROLE_BROADCASTER
     8. api.subscribeMessage(channelName) { onReady(...) }
        // onReady(null) only after subscribeMessage succeeds
   }

9. Caller: POST /startAgent  ← only after onReady(null)
```

Skipping or reordering steps 6–8 causes audio or transcript issues. See [07_gotchas](../07_gotchas.md).

## ChannelMediaOptions — why BROADCASTER

The app must publish its microphone track for the agent to hear the user. `CLIENT_ROLE_BROADCASTER` + `publishMicrophoneTrack = true` + `autoSubscribeAudio = true` is the correct combination for a voice agent quickstart. An audience role would suppress microphone publishing.

## RTM UID alignment

The `RtmConfig` is built with the same `rtcUid.toString()` used for the RTC join. The ConversationalAIAPI toolkit uses this alignment to correlate RTM messages to RTC speakers. If the UIDs diverge, transcript speaker attribution breaks.

## Mute implementation

Mic mute uses `rtcEngine.adjustRecordingSignalVolume(0)` (silence the recording signal) rather than `updateChannelMediaOptions(publishMicrophoneTrack = false)`. This avoids mid-call channel option re-negotiation and is the pattern verified in the iOS quickstart.

## Teardown order (inside `AgoraSession.stop()`)

```
1. api.unsubscribeMessage(channelName)
2. api.removeHandler(this)
3. api.destroy()
4. rtcEngine?.leaveChannel()
5. rtcEngine = null; RtcEngine.destroy()    ← static destroy
6. rtmClient?.logout(...)
7. rtmClient = null; RtmClient.release()    ← static release
```

`RtcEngine.destroy()` and `RtmClient.release()` are static calls that release native resources. They must come after the instance references are nulled to avoid double-free. The toolkit must be destroyed before the engines.

## Transcript upsert keying

The toolkit delivers `Transcript` events for the same `(turnId, type)` multiple times as the text streams in (the agent narrates word by word, then finalizes). `CallViewModel.upsertTranscript` deduplicates by `(turnId, type)`:

- Key in the list: `indexOfFirst { it.turnId == t.turnId && it.type == t.type }`.
- `LazyColumn` item key: `"$turnId-${type}"` — stable across recompositions.
- Sort: ascending by `turnId`, then `USER` before `AGENT` within a turn.

`TranscriptRenderMode.Text` (v3) delivers the final full text per update. Do not switch to `Word` mode without updating the upsert and display logic.

## ConversationalAIAPIConfig fields

| Field | Value used | Notes |
| ----- | ---------- | ----- |
| `rtcEngine` | the live `RtcEngineEx` instance | Must be the same instance used for `joinChannel` |
| `rtmClient` | the live `RtmClient` instance | Must be the same instance used for `login` |
| `renderMode` | `TranscriptRenderMode.Text` | Activates v3 full-text transcript parser |

## Related L1

- [02_architecture](../02_architecture.md) — topology overview.
- [06_interfaces](../06_interfaces.md) — Agora SDK surface table.
- [07_gotchas](../07_gotchas.md) — ordering pitfalls.
