package com.mindurset.app.ui.safety

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk

/**
 * S-91: the person the crisis card offers to reach. Two states, both local — a saved contact with call
 * and message actions, or the form to add one. It never asks for network access, and the sheet is only
 * reachable from the crisis card, which is where the proposal puts it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactSheet(
    state: TrustedContactState,
    onDismiss: () -> Unit,
    onStartEditing: () -> Unit,
    onCancelEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val messageBody = stringResource(R.string.crisis_contact_message_body)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.crisis_contact_title),
                style = MaterialTheme.typography.titleMedium,
                color = NavyInk,
            )
            val contact = state.contact
            if (contact == null || state.editing) {
                if (contact == null) {
                    Text(
                        text = stringResource(R.string.crisis_contact_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.crisis_contact_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = onPhoneChange,
                    label = { Text(stringResource(R.string.crisis_contact_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.invalid) {
                    Text(
                        text = stringResource(R.string.crisis_contact_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text(stringResource(R.string.crisis_contact_save))
                }
                if (contact != null) {
                    TextButton(onClick = onCancelEditing) {
                        Text(stringResource(R.string.crisis_contact_cancel))
                    }
                }
            } else {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyInk,
                )
                Text(
                    text = contact.phone,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedGray,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")),
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(stringResource(R.string.crisis_contact_call))
                    }
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_SENDTO,
                                    Uri.parse("smsto:${contact.phone}"),
                                ).putExtra("sms_body", messageBody),
                            )
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(stringResource(R.string.crisis_contact_message))
                    }
                }
                TextButton(onClick = onStartEditing) {
                    Text(stringResource(R.string.crisis_contact_change))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
