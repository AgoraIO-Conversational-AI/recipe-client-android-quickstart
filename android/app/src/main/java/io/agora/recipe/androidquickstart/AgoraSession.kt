package io.agora.recipe.androidquickstart

import android.content.Context
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtm.ErrorInfo
import io.agora.rtm.ResultCallback
import io.agora.rtm.RtmClient
import io.agora.rtm.RtmConfig
import io.agora.conversational.api.ConversationalAIAPIConfig
import io.agora.conversational.api.ConversationalAIAPIError
import io.agora.conversational.api.ConversationalAIAPIImpl
import io.agora.conversational.api.IConversationalAIAPI
import io.agora.conversational.api.IConversationalAIAPIEventHandler
import io.agora.conversational.api.InterruptEvent
import io.agora.conversational.api.MessageError
import io.agora.conversational.api.MessageReceipt
import io.agora.conversational.api.Metric
import io.agora.conversational.api.ModuleError
import io.agora.conversational.api.StateChangeEvent
import io.agora.conversational.api.Transcript
import io.agora.conversational.api.TranscriptRenderMode
import io.agora.conversational.api.Turn
import io.agora.conversational.api.VoiceprintStateChangeEvent

/**
 * Owns the Agora RTC engine, the RTM client and the ConversationalAI toolkit,
 * and implements the toolkit's event handler. The app drives the RTC/RTM lifecycle; the
 * toolkit only parses messages into transcript / state callbacks.
 *
 * iOS lessons baked in:
 *  - join with publishMicrophoneTrack = true, autoSubscribeAudio = true, BROADCASTER role
 *  - loadAudioSettings() before joinChannel
 *  - subscribeMessage(channelName) BEFORE the caller invokes /startAgent
 */
class AgoraSession(
    private val onTranscript: (Transcript) -> Unit,
    private val onAgentState: (StateChangeEvent) -> Unit,
    private val onError: (String) -> Unit,
) : IConversationalAIAPIEventHandler {

    companion object { private const val DIAG = "DIAG" }

    private var rtcEngine: RtcEngineEx? = null
    private var rtmClient: RtmClient? = null
    private var convoApi: IConversationalAIAPI? = null
    private var channelName: String = ""

    private val rtcHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.i(DIAG, "rtc onJoinChannelSuccess channel=$channel uid=$uid")
        }
        override fun onError(err: Int) {
            Log.i(DIAG, "rtc onError err=$err")
            onError("RTC error $err")
        }
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.i(DIAG, "rtc onUserJoined uid=$uid")
        }
        override fun onConnectionStateChanged(state: Int, reason: Int) {
            Log.i(DIAG, "rtc onConnectionStateChanged state=$state reason=$reason")
        }
    }

    /**
     * Create the RTC engine + RTM client, log in to RTM, wire the toolkit, join the channel,
     * and subscribe to AI messages — all BEFORE the caller starts the agent.
     */
    fun start(
        context: Context,
        appId: String,
        token: String,
        channelName: String,
        rtcUid: Int,
        onReady: (error: String?) -> Unit,
    ) {
        this.channelName = channelName

        // --- RTC engine ---
        val rtcConfig = RtcEngineConfig().apply {
            mContext = context.applicationContext
            mAppId = appId
            mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            mAudioScenario = Constants.AUDIO_SCENARIO_AI_CLIENT
            mEventHandler = rtcHandler
        }
        val rtc = (RtcEngine.create(rtcConfig) as RtcEngineEx)
        rtcEngine = rtc

        // --- RTM client ---
        val rtmConfig = RtmConfig.Builder(appId, rtcUid.toString()).build()
        val rtm = RtmClient.create(rtmConfig)
        rtmClient = rtm

        // --- Toolkit ---
        val api = ConversationalAIAPIImpl(
            ConversationalAIAPIConfig(
                rtcEngine = rtc,
                rtmClient = rtm,
                renderMode = TranscriptRenderMode.Text,
            )
        )
        convoApi = api
        api.addHandler(this)

        // --- RTM login, then join + subscribe ---
        rtm.login(token, object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                Log.i(DIAG, "rtm login success uid=$rtcUid")

                // loadAudioSettings MUST precede joinChannel
                api.loadAudioSettings(Constants.AUDIO_SCENARIO_AI_CLIENT)

                val options = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    publishMicrophoneTrack = true
                    autoSubscribeAudio = true
                }
                val ret = rtc.joinChannel(token, channelName, rtcUid, options)
                Log.i(DIAG, "rtc joinChannel channel=$channelName uid=$rtcUid ret=$ret")

                // subscribeMessage BEFORE /startAgent
                api.subscribeMessage(channelName) { error ->
                    if (error == null) {
                        Log.i(DIAG, "subscribeMessage success channel=$channelName")
                        onReady(null)
                    } else {
                        Log.i(DIAG, "subscribeMessage failed: ${error.errorMessage}")
                        onError("subscribeMessage failed: ${error.errorMessage}")
                        onReady(error.errorMessage)
                    }
                }
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                val msg = errorInfo?.errorReason ?: "unknown"
                Log.i(DIAG, "rtm login failed: $msg")
                onError("RTM login failed: $msg")
                onReady(msg)
            }
        })
    }

    fun setMicMuted(muted: Boolean) {
        Log.i(DIAG, "setMicMuted $muted")
        // 0 mutes the recording signal, 100 restores it
        rtcEngine?.adjustRecordingSignalVolume(if (muted) 0 else 100)
    }

    fun stop() {
        Log.i(DIAG, "stop session")
        val ch = channelName
        convoApi?.unsubscribeMessage(ch) { }
        convoApi?.removeHandler(this)
        convoApi?.destroy()
        convoApi = null
        rtcEngine?.leaveChannel()
        rtcEngine = null
        RtcEngine.destroy()
        rtmClient?.logout(object : ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {}
            override fun onFailure(errorInfo: ErrorInfo?) {}
        })
        rtmClient = null
        RtmClient.release()
    }

    // --- IConversationalAIAPIEventHandler ---

    override fun onAgentStateChanged(agentUserId: String, event: StateChangeEvent) {
        Log.i(DIAG, "onAgentStateChanged agent=$agentUserId state=${event.state} turnId=${event.turnId}")
        onAgentState(event)
    }

    override fun onAgentListeningChanged(agentUserId: String, isListening: Boolean) {
        Log.i(DIAG, "onAgentListeningChanged agent=$agentUserId listening=$isListening")
    }

    override fun onAgentThinkingChanged(agentUserId: String, isThinking: Boolean) {
        Log.i(DIAG, "onAgentThinkingChanged agent=$agentUserId thinking=$isThinking")
    }

    override fun onAgentSpeakingChanged(agentUserId: String, isSpeaking: Boolean) {
        Log.i(DIAG, "onAgentSpeakingChanged agent=$agentUserId speaking=$isSpeaking")
    }

    override fun onAgentInterrupted(agentUserId: String, event: InterruptEvent) {
        Log.i(DIAG, "onAgentInterrupted agent=$agentUserId turnId=${event.turnId}")
    }

    override fun onAgentMetrics(agentUserId: String, metric: Metric) {
        Log.i(DIAG, "onAgentMetrics agent=$agentUserId ${metric.name}=${metric.value}")
    }

    override fun onTurnFinished(agentUserId: String, turn: Turn) {
        Log.i(DIAG, "onTurnFinished agent=$agentUserId turnId=${turn.turnId} e2e=${turn.e2eLatency}")
    }

    override fun onAgentError(agentUserId: String, error: ModuleError) {
        Log.i(DIAG, "onAgentError agent=$agentUserId code=${error.code} ${error.message}")
        onError("agent error ${error.code}: ${error.message}")
    }

    override fun onMessageError(agentUserId: String, error: MessageError) {
        Log.i(DIAG, "onMessageError agent=$agentUserId code=${error.code} ${error.message}")
        onError("message error ${error.code}: ${error.message}")
    }

    override fun onMessageReceiptUpdated(agentUserId: String, receipt: MessageReceipt) {
        Log.i(DIAG, "onMessageReceiptUpdated agent=$agentUserId turnId=${receipt.turnId}")
    }

    override fun onAgentVoiceprintStateChanged(agentUserId: String, event: VoiceprintStateChangeEvent) {
        Log.i(DIAG, "onAgentVoiceprintStateChanged agent=$agentUserId status=${event.status}")
    }

    override fun onTranscriptUpdated(agentUserId: String, transcript: Transcript) {
        Log.i(
            DIAG,
            "onTranscriptUpdated agent=$agentUserId turnId=${transcript.turnId} " +
                "type=${transcript.type} status=${transcript.status} text=\"${transcript.text}\""
        )
        onTranscript(transcript)
    }

    override fun onDebugLog(log: String) {
        Log.i(DIAG, "onDebugLog $log")
    }
}
