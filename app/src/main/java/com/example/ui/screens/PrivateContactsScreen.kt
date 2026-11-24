package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PrivateContactEntry
import com.example.data.telephony.PhoneNumberInfoHelper
import com.example.ui.theme.CallIncomingGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalAppStrings

@Composable
fun PrivateContactsScreen(
    privateContacts: List<PrivateContactEntry>,
    onBack: () -> Unit,
    onAddPrivateContact: (name: String, number: String, note: String, sensitiveMask: Boolean) -> Unit,
    onToggleSensitiveMask: (String, Boolean) -> Unit,
    onDeletePrivateContact: (String) -> Unit,
    onCallChooseSim: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_private_contact_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = strings.addPrivateContact, tint = MaterialTheme.colorScheme.onPrimary)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("private_contacts_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = strings.privateContactsTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.privateContactsSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (privateContacts.isEmpty()) {
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
                                imageVector = Icons.Rounded.PersonOff,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = strings.privateContactsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.privateContactsSub,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(privateContacts, key = { it.id }) { contact ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF6366F1).copy(alpha = 0.14f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFF6366F1))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = PhoneNumberInfoHelper.formatDisplayNumber(contact.number),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CallIncomingGreen
                                    )
                                    if (contact.note.isNotBlank()) {
                                        Text(
                                            text = contact.note,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onCallChooseSim(contact.number) }) {
                                    Icon(Icons.Rounded.Call, contentDescription = strings.callButton, tint = CallIncomingGreen)
                                }
                                IconButton(onClick = { onDeletePrivateContact(contact.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = CallMissedRed)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = strings.sensitiveProtectionTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = contact.sensitiveMask,
                                    onCheckedChange = { onToggleSensitiveMask(contact.id, it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addPrivateContact) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                            onAddPrivateContact(name, number, note, true)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
