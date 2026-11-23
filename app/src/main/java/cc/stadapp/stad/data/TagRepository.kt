package cc.stadapp.stad.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import cc.stadapp.stad.core.EscapeTracker
import cc.stadapp.stad.core.LockState
import cc.stadapp.stad.core.TagAction
import cc.stadapp.stad.core.TagId
import cc.stadapp.stad.core.TagOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The paired tag, and the tap that toggles the lock. Both read the lock state inside the same
 * DataStore edit, so a tap and a re-pair can't interleave, and the lock never changes without a
 * confirmed write. The tag ID is stored on the device only and never logged.
 */
class TagRepository(private val dataStore: DataStore<Preferences>) {

    /** The paired tag's ID, or null. A read error gives null: nothing can toggle, nothing locks. */
    val pairedTag: Flow<String?> = dataStore.data
        .retryingOnReadError()
        .map { prefs -> TagId.normalize(prefs[StadKeys.PAIRED_TAG]) }
        .distinctUntilChanged()

    /**
     * Pairs [id], replacing any earlier tag. Refused (returns false) while locked.
     * Throws [java.io.IOException] if the write fails.
     */
    suspend fun pair(id: String): Boolean {
        val normalized = requireNotNull(TagId.normalize(id)) { "Not a tag ID" }
        var paired = false
        dataStore.edit { prefs ->
            if (LockState.fromStored(prefs[StadKeys.LOCK_STATE]).isLocked) return@edit
            prefs[StadKeys.PAIRED_TAG] = normalized
            paired = true
        }
        return paired
    }

    /**
     * Handles a tap of the tag [scannedId]: the paired tag toggles the lock, anything else changes
     * nothing. The blocking history is updated in the same edit, reading [blocking] inside it so
     * the service's state is current when the lock changes. Throws
     * [java.io.IOException] if the write fails, in which case nothing changed.
     */
    suspend fun tap(
        scannedId: String,
        blocking: BlockingProbe = BlockingProbe.Fixed(),
        now: Long = System.currentTimeMillis(),
    ): TagOutcome {
        var outcome = TagOutcome.NotPaired
        dataStore.edit { prefs ->
            val lock = LockState.fromStored(prefs[StadKeys.LOCK_STATE])
            outcome = TagAction.decide(scannedId, prefs[StadKeys.PAIRED_TAG], lock)
            when (outcome) {
                TagOutcome.Lock -> {
                    prefs[StadKeys.LOCK_STATE] = LockState.Locked.name
                    prefs.applyEscape(EscapeTracker.onLocked(prefs.escapeState(), blocking.serviceOn(), now))
                }
                TagOutcome.Unlock -> {
                    prefs[StadKeys.LOCK_STATE] = LockState.Unlocked.name
                    val on = blocking.serviceOn()
                    val stoppedAt = if (on) null else blocking.forceStoppedAt()
                    prefs.applyEscape(EscapeTracker.onUnlocked(prefs.escapeState(), on, stoppedAt, now))
                }
                TagOutcome.NotPaired, TagOutcome.NotYourTag -> Unit
            }
        }
        return outcome
    }
}
