package cc.stadapp.stad.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cc.stadapp.stad.R
import cc.stadapp.stad.core.PickerRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    state: BlockListUiState,
    loadIcon: (String) -> Bitmap?,
    onQueryChange: (String) -> Unit,
    onToggle: (packageName: String, blocked: Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.choose_apps)) }) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (!state.editable) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.list_locked_banner),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.rows.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_apps_match))
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.app.packageName }) { row ->
                        AppRow(row, loadIcon, onToggle)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    row: PickerRow,
    loadIcon: (String) -> Bitmap?,
    onToggle: (String, Boolean) -> Unit,
) {
    val icon by produceState<Bitmap?>(initialValue = null, row.app.packageName) {
        value = withContext(Dispatchers.IO) { loadIcon(row.app.packageName) }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = row.checked,
                enabled = row.enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle(row.app.packageName, it) },
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            Spacer(Modifier.size(40.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(row.app.displayName, style = MaterialTheme.typography.bodyLarge)
            if (row.protected) {
                Text(
                    stringResource(R.string.cannot_be_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Checkbox(checked = row.checked, onCheckedChange = null, enabled = row.enabled)
    }
}
