package cc.stadapp.stad.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cc.stadapp.stad.R
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.ui.theme.StadTheme

/** Home: name, lock state, how many apps are blocked, and the way into the picker. */
@Composable
fun HomeScreen(
    state: LockState = LockState.Unlocked,
    blockedCount: Int = 0,
    onChooseApps: () -> Unit = {},
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            Button(onClick = onChooseApps, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.choose_apps))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    StadTheme { HomeScreen(blockedCount = 3) }
}
