package cc.stadapp.stad.service

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Parcelable
import android.widget.Toast
import cc.stadapp.stad.R
import cc.stadapp.stad.core.BlockListRules
import cc.stadapp.stad.core.StadTagMime
import cc.stadapp.stad.core.TagFeedback
import cc.stadapp.stad.core.TagId
import cc.stadapp.stad.core.TagOutcome
import cc.stadapp.stad.core.TapDebounce
import cc.stadapp.stad.data.DataStoreBlockListRepository
import cc.stadapp.stad.data.TagRepository
import cc.stadapp.stad.data.stadSettings
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first

/**
 * Handles a tap of a tag outside pairing, wherever it came from (the system's NFC dispatch to
 * [TagActivity], or reader mode while Stad is open).
 */
object TagTaps {

    /** One per process: each tap through [TagActivity] is a new activity. */
    val debounce = TapDebounce()

    /**
     * Writes outlive the screen that started them, so a tap's lock change can't be cancelled by
     * the activity finishing or the block screen appearing.
     */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Toggles the lock if [id] is the paired tag, and works out what to tell the user. */
    suspend fun handle(context: Context, id: String): TagFeedback {
        val app = context.applicationContext
        return appScope.async {
            val outcome = try {
                TagRepository(app.stadSettings).tap(id, ServiceStatus.isOn(app), ForceStops.lastAt(app), System.currentTimeMillis())
            } catch (_: IOException) {
                null
            }
            val count = if (outcome == TagOutcome.Lock) {
                BlockListRules.blockedCount(
                    DataStoreBlockListRepository(app.stadSettings).blockList.first(),
                    BlockListRules.ALWAYS_PROTECTED,
                )
            } else {
                0
            }
            TagFeedback.of(outcome, count, ServiceStatus.isOn(app))
        }.await()
    }

    /** Buzz and toast. Call while Stad is in the foreground: Android drops background vibrations. */
    fun show(context: Context, feedback: TagFeedback) {
        Haptics.play(context, feedback.haptic)
        Toast.makeText(context, message(context, feedback), Toast.LENGTH_SHORT).show()
    }

    /**
     * The tag ID from a system NFC intent, or null unless it's a real Stad tap: the
     * NDEF_DISCOVERED action, Stad's MIME type, and a tag. Uses the tag's own ID,
     * never the separate ID extra.
     */
    fun idFromIntent(intent: Intent?): String? {
        if (intent?.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return null
        // Android sets the intent type from the first record, which is what the filter matched.
        if (!StadTagMime.matches(intent.type)) return null
        val tag = intent.parcelable(NfcAdapter.EXTRA_TAG, Tag::class.java) ?: return null
        return TagId.fromBytes(tag.id)
    }

    private fun message(context: Context, feedback: TagFeedback): String = when (feedback.message) {
        TagFeedback.Message.Locked -> context.getString(R.string.tap_locked)
        TagFeedback.Message.LockedServiceOff -> context.getString(R.string.tap_locked_service_off)
        TagFeedback.Message.LockedNoApps -> context.getString(R.string.tap_locked_no_apps)
        TagFeedback.Message.Unlocked -> context.getString(R.string.tap_unlocked)
        TagFeedback.Message.NotYourTag -> context.getString(R.string.tap_not_your_tag)
        TagFeedback.Message.NotPaired -> context.getString(R.string.tap_not_paired)
        TagFeedback.Message.WriteFailed -> context.getString(R.string.tap_write_failed)
    }

    private fun <T : Parcelable> Intent.parcelable(name: String, cls: Class<T>): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, cls)
        } else {
            // The legacy getter can't check the type, and TagActivity is exported: another app
            // could put anything here, so check it rather than crash on a cast.
            @Suppress("DEPRECATION")
            getParcelableExtra<Parcelable>(name)?.takeIf(cls::isInstance)?.let(cls::cast)
        }
}
