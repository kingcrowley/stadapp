package cc.stadapp.stad.service

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import cc.stadapp.stad.BuildConfig
import cc.stadapp.stad.R
import cc.stadapp.stad.data.LockStateRepository
import cc.stadapp.stad.data.stadSettings
import cc.stadapp.stad.ui.theme.StadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shown over a blocked app. Started only by [BlockerAccessibilityService] (not exported). It
 * closes itself whenever it leaves the screen or Stad is unlocked, so a stale block screen never
 * comes back.
 */
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val label = appLabel(pkg)
        val icons = AppIcons(this)
        // Unlocking with the tag closes the block screen.
        lifecycleScope.launch {
            LockStateRepository(stadSettings).lockState.first { !it.isLocked }
            finish()
        }
        setContent {
            StadTheme {
                BackHandler { goHome() }
                val icon by produceState<android.graphics.Bitmap?>(null, pkg) {
                    value = withContext(Dispatchers.IO) { icons.load(pkg) }
                }
                BlockedScreen(label = label, icon = icon, onGoHome = ::goHome)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.applicationInfoCompat(pkg, 0).loadLabel(packageManager).toString()
    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
        pkg
    }.ifBlank { getString(R.string.this_app) }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE = "cc.stadapp.stad.extra.PACKAGE"
    }
}

@Composable
private fun BlockedScreen(label: String, icon: android.graphics.Bitmap?, onGoHome: () -> Unit) {
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
            if (icon != null) {
                // The app name is in the text below, so the icon is decorative for TalkBack.
                Image(icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp))
            }
            Text(
                text = stringResource(R.string.blocked_title, label),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                // Debug builds also have the Unlock (debug) button on Home.
                text = stringResource(if (BuildConfig.DEBUG) R.string.blocked_message_debug else R.string.blocked_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onGoHome, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.go_home))
            }
        }
    }
}
