package cc.stadapp.stad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cc.stadapp.stad.R
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.PairingFlow
import cc.stadapp.stad.ui.theme.StadTheme

/**
 * Home: name, lock state, how many apps are blocked, whether the blocking service is on, and the
 * way into the picker, plus the paired tag and NFC state. [pairing] is non-null while the pairing
 * dialog is open. [showDebugLock] is true in debug builds only.
 */
@Composable
fun HomeScreen(
    state: LockState = LockState.Unlocked,
    blockedCount: Int = 0,
    serviceEnabled: Boolean = false,
    protectionUnknown: Boolean = false,
    lockFailed: Boolean = false,
    settingsUnavailable: Boolean = false,
    tagPaired: Boolean = false,
    nfcStatus: NfcStatus = NfcStatus.On,
    nfcSettingsUnavailable: Boolean = false,
    pairing: PairingFlow.State? = null,
    showDebugLock: Boolean = false,
    onChooseApps: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onOpenNfcSettings: () -> Unit = {},
    onPairTag: () -> Unit = {},
    onClosePairing: () -> Unit = {},
    onDebugLock: (LockState) -> Unit = {},
) {
    var showDisclosure by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(
                    if (state.isLocked) R.string.state_locked else R.string.state_unlocked
                ),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 32.dp),
            )
            Text(
                text = pluralStringResource(R.plurals.apps_blocked, blockedCount, blockedCount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )

            ServiceCard(
                enabled = serviceEnabled,
                onTurnOn = { showDisclosure = true },
                modifier = Modifier.padding(top = 24.dp),
            )

            if (nfcStatus != NfcStatus.On) {
                NfcCard(
                    status = nfcStatus,
                    onTurnOn = onOpenNfcSettings,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            TagCard(
                paired = tagPaired,
                locked = state.isLocked,
                canPair = nfcStatus == NfcStatus.On && !state.isLocked,
                onPair = onPairTag,
                modifier = Modifier.padding(top = 16.dp),
            )

            listOfNotNull(
                R.string.nfc_settings_unavailable.takeIf { nfcSettingsUnavailable },
                R.string.settings_unavailable.takeIf { settingsUnavailable },
                R.string.protection_unknown_home.takeIf { protectionUnknown },
                R.string.lock_failed.takeIf { lockFailed },
            ).forEach { warning ->
                Text(
                    text = stringResource(warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            Button(onClick = onChooseApps, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.choose_apps))
            }

            if (showDebugLock) {
                OutlinedButton(
                    onClick = { onDebugLock(state.toggled()) },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(if (state.isLocked) R.string.debug_unlock else R.string.debug_lock))
                }
            }
        }
    }

    if (showDisclosure) {
        // Play's prominent-disclosure rules: shown before settings, explicit consent button,
        // and dismissing it is not consent.
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text(stringResource(R.string.disclosure_title)) },
            text = { Text(stringResource(R.string.disclosure_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    onOpenAccessibilitySettings()
                }) { Text(stringResource(R.string.disclosure_agree)) }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosure = false }) {
                    Text(stringResource(R.string.disclosure_decline))
                }
            },
        )
    }

    if (pairing != null) {
        PairingDialog(state = pairing, onClose = onClosePairing)
    }
}

@Composable
private fun TagCard(
    paired: Boolean,
    locked: Boolean,
    canPair: Boolean,
    onPair: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(if (paired) R.string.tag_paired else R.string.tag_not_paired),
                style = MaterialTheme.typography.bodyLarge,
            )
            // Strict: the tag can't be changed while locked, or a new tag would be an unlock.
            if (locked) {
                Text(
                    text = stringResource(R.string.tag_locked_help),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                OutlinedButton(
                    onClick = onPair,
                    enabled = canPair,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(stringResource(if (paired) R.string.repair_tag else R.string.pair_tag))
                }
            }
        }
    }
}

@Composable
private fun NfcCard(status: NfcStatus, onTurnOn: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    if (status == NfcStatus.Missing) R.string.nfc_missing else R.string.nfc_off
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (status == NfcStatus.Off) {
                Button(onClick = onTurnOn, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.turn_on))
                }
            }
        }
    }
}

@Composable
private fun PairingDialog(state: PairingFlow.State, onClose: () -> Unit) {
    val done = state.step is PairingFlow.Step.Done
    val message = when (state.error) {
        PairingFlow.Error.RandomId -> R.string.pairing_error_random
        PairingFlow.Error.CantWrite -> R.string.pairing_error_cant_write
        PairingFlow.Error.WriteFailed -> R.string.pairing_error_write_failed
        PairingFlow.Error.DifferentTag -> R.string.pairing_error_different
        PairingFlow.Error.Locked -> R.string.pairing_error_locked
        PairingFlow.Error.SaveFailed -> R.string.pairing_error_save_failed
        null -> when (state.step) {
            PairingFlow.Step.Waiting -> R.string.pairing_first
            is PairingFlow.Step.Confirming -> R.string.pairing_confirm
            is PairingFlow.Step.Saving -> R.string.pairing_saving
            is PairingFlow.Step.Done -> R.string.pairing_done
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.pairing_title)) },
        text = {
            Text(
                text = stringResource(message),
                color = if (state.error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(if (done) R.string.done else R.string.cancel))
            }
        },
    )
}

@Composable
private fun ServiceCard(enabled: Boolean, onTurnOn: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(if (enabled) R.string.service_on else R.string.service_off),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!enabled) {
                Button(onClick = onTurnOn, modifier = Modifier.padding(top = 12.dp)) {
                    Text(stringResource(R.string.turn_on))
                }
                Text(
                    text = stringResource(R.string.restricted_settings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    StadTheme { HomeScreen(blockedCount = 3, showDebugLock = true) }
}
