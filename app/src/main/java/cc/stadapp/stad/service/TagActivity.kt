package cc.stadapp.stad.service

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Started by Android when a Stad tag is tapped anywhere (NDEF_DISCOVERED, see the manifest).
 * Invisible: it toggles the lock, buzzes, shows a toast and closes.
 *
 * It's exported, so another app could send it a made-up tag. That still needs the paired tag's
 * ID, which is the same as cloning the tag, and is accepted (see the slice 1.3 spec).
 */
class TagActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity (e.g. rotation) must not tap again.
        if (savedInstanceState != null) return finish()
        val id = TagTaps.idFromIntent(intent)
        if (id == null || !TagTaps.debounce.accept(id, SystemClock.elapsedRealtime())) return finish()
        lifecycleScope.launch {
            val feedback = TagTaps.handle(this@TagActivity, id)
            TagTaps.show(this@TagActivity, feedback)
            finish()
        }
    }
}
