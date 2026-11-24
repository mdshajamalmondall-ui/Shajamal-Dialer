package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.BlockedNumberEntry
import com.example.data.model.CommunitySpamReport
import com.example.data.model.SpamCategory
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpamDatabaseScreen(
    language: AppLanguage,
    communityReports: List<CommunitySpamReport>,
    blockedNumbers: List<BlockedNumberEntry>,
    onBack: () -> Unit,
    onSubmitReport: (number: String, category: SpamCategory, note: String, alsoBlock: Boolean) -> Unit,
    onRemoveReport: (String) -> Unit,
    onUnblockNumber: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    val isBn = language == AppLanguage.BENGALI
    var showReportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showReportDialog = true },
                containerColor = CallMissedRed,
                modifier = Modifier.testTag("add_spam_report_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.reportNumberTitle, tint = Color.White)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("spam_db_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strings.spamDatabaseTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.spamDatabaseSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "${strings.reportNumberTitle} (${communityReports.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (communityReports.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = Color(0xFFD97706))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = strings.spamDatabaseSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(communityReports, key = { it.canonicalKey }) { report ->
                    val catColor = when (report.category) {
                        SpamCategory.SCAM_FRAUD -> CallMissedRed
                        SpamCategory.VERIFIED_BUSINESS -> CallIncomingGreen
                        else -> Color(0xFFD97706)
                    }
                    val catLabel = if (isBn) report.category.labelBn else report.category.labelEn
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.38f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = PhoneNumberInfoHelper.formatDisplayNumber(report.number),
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = catColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "$catLabel • ${report.reportCount}x",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = catColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                if (report.note.isNotBlank()) {
                                    Text(
                                        text = report.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { onRemoveReport(report.canonicalKey) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = CallMissedRed)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${strings.blockListTitle} (${blockedNumbers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(blockedNumbers, key = { it.number }) { entry ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, CallMissedRed.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = CallMissedRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = PhoneNumberInfoHelper.formatDisplayNumber(entry.number),
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (entry.label.isNotBlank()) {
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { onUnblockNumber(entry.number) }) {
                            Text(strings.unblock, color = CallMissedRed)
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        var number by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf(SpamCategory.TELEMARKETING) }
        val selectable = remember {
            listOf(
                SpamCategory.SCAM_FRAUD,
                SpamCategory.TELEMARKETING,
                SpamCategory.LOAN_DEBT,
                SpamCategory.ROBOCALL,
                SpamCategory.HARASSMENT,
                SpamCategory.VERIFIED_BUSINESS
            )
        }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text(strings.reportNumberTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text(strings.enterNumberToBlock) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectable.forEach { cat ->
                            val selected = cat == selectedCategory
                            val label = if (isBn) cat.labelBn else cat.labelEn
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(strings.notePlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (number.isNotBlank()) {
                            onSubmitReport(
                                number,
                                selectedCategory,
                                note,
                                selectedCategory == SpamCategory.SCAM_FRAUD || selectedCategory == SpamCategory.HARASSMENT
                            )
                            showReportDialog = false
                        }
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
