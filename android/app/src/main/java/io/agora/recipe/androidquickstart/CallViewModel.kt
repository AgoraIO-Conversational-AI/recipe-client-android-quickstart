package io.agora.recipe.androidquickstart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.agora.conversational.api.AgentState
import io.agora.conversational.api.Transcript
import io.agora.conversational.api.TranscriptType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CallPhase { Idle, Connecting, InCall, Error }

class CallViewModel(app: Application) : AndroidViewModel(app) {

    private val api = BackendApi(BuildConfig.AGENT_BACKEND_URL)

    private val session = AgoraSession(
        onTranscript = { upsertTranscript(it) },
        onAgentState = { _agentState.value = it.state },
        onError = { msg ->
            _errorMessage.value = msg
            _phase.value = CallPhase.Error
        },
    )

    private val _phase = MutableStateFlow(CallPhase.Idle)
    val phase: StateFlow<CallPhase> = _phase.asStateFlow()

    private val _turns = MutableStateFlow<List<Transcript>>(emptyList())
    val turns: StateFlow<List<Transcript>> = _turns.asStateFlow()

    private val _micMuted = MutableStateFlow(false)
    val micMuted: StateFlow<Boolean> = _micMuted.asStateFlow()

    private val _agentState = MutableStateFlow(AgentState.IDLE)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var agentId: String? = null

    /** Upsert a transcript row keyed by (turnId, type); keep sorted turn asc, user before agent. */
    private fun upsertTranscript(t: Transcript) {
        val current = _turns.value.toMutableList()
        val idx = current.indexOfFirst { it.turnId == t.turnId && it.type == t.type }
        if (idx >= 0) current[idx] = t else current.add(t)
        current.sortWith(
            compareBy({ it.turnId }, { if (it.type == TranscriptType.USER) 0 else 1 })
        )
        _turns.value = current
    }

    fun connect() {
        if (_phase.value == CallPhase.Connecting || _phase.value == CallPhase.InCall) return
        _errorMessage.value = null
        _phase.value = CallPhase.Connecting
        viewModelScope.launch {
            try {
                val cfg = withContext(Dispatchers.IO) { api.getConfig() }
                val rtcUid = cfg.uid.toInt()
                session.start(
                    context = getApplication(),
                    appId = cfg.appId,
                    token = cfg.token,
                    channelName = cfg.channelName,
                    rtcUid = rtcUid,
                ) { error ->
                    if (error != null) {
                        _errorMessage.value = error
                        _phase.value = CallPhase.Error
                        return@start
                    }
                    // subscribeMessage succeeded -> now start the agent
                    viewModelScope.launch {
                        try {
                            val id = withContext(Dispatchers.IO) {
                                api.startAgent(
                                    channelName = cfg.channelName,
                                    rtcUid = cfg.agentUid.toInt(),
                                    userUid = cfg.uid.toInt(),
                                )
                            }
                            agentId = id
                            _phase.value = CallPhase.InCall
                        } catch (e: Exception) {
                            _errorMessage.value = e.message ?: "startAgent failed"
                            _phase.value = CallPhase.Error
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "connect failed"
                _phase.value = CallPhase.Error
            }
        }
    }

    fun toggleMic() {
        val next = !_micMuted.value
        _micMuted.value = next
        session.setMicMuted(next)
    }

    fun end() {
        val id = agentId
        agentId = null
        viewModelScope.launch {
            if (id != null) {
                runCatching { withContext(Dispatchers.IO) { api.stopAgent(id) } }
            }
            session.stop()
            _turns.value = emptyList()
            _agentState.value = AgentState.IDLE
            _micMuted.value = false
            _phase.value = CallPhase.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        session.stop()
    }
}
