package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.HiddenCallEntry
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HiddenHistoryScreen(
    hiddenCalls: List<HiddenCallEntry>,
    onBack: () -> Unit,
    onCallChooseSim: (String) -> Unit,
    onDeleteHiddenCall: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("hidden_history_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strings.hiddenHistoryTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.hiddenHistorySub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (hiddenCalls.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = CallMissedRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.delete, color = CallMissedRed)
                    }
                }
            }
        }

        if (hiddenCalls.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = Color(0xFF0EA5E9),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(strings.hiddenHistoryTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            strings.hiddenHistorySub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(hiddenCalls, key = { it.id }) { entry ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.contactName ?: PhoneNumberInfoHelper.formatDisplayNumber(entry.number),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = PhoneNumberInfoHelper.formatDisplayNumber(entry.number),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = CallIncomingGreen
                            )
                            val simLabel = entry.simSlotIndex?.let { " • SIM ${it + 1}" }.orEmpty()
                            Text(
                                text = "${entry.direction.name}$simLabel • ${dateFormat.format(Date(entry.timestampMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onCallChooseSim(entry.number) }) {
                            Icon(Icons.Rounded.Call, contentDescription = strings.callButton, tint = CallIncomingGreen)
                        }
                        IconButton(onClick = { onDeleteHiddenCall(entry.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = CallMissedRed)
                        }
                    }
                }
            }
        }
    }
}
