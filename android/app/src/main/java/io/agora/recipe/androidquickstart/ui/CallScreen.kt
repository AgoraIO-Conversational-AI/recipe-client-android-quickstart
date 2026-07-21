package io.agora.recipe.androidquickstart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.agora.conversational.api.AgentState
import io.agora.conversational.api.Transcript
import io.agora.conversational.api.TranscriptType

@Composable
fun CallScreen(
    connecting: Boolean,
    agentState: AgentState,
    micMuted: Boolean,
    turns: List<Transcript>,
    onToggleMic: () -> Unit,
    onEnd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = if (connecting) "Connecting…" else "Agent: ${agentState.value}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(turns, key = { "${it.turnId}-${it.type}" }) { t ->
                val who = if (t.type == TranscriptType.USER) "You" else "Agent"
                Column {
                    Text(who, style = MaterialTheme.typography.labelMedium)
                    Text(t.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onToggleMic) {
                Text(if (micMuted) "Unmute" else "Mute")
            }
            Button(onClick = onEnd) {
                Text("End")
            }
        }
    }
}
