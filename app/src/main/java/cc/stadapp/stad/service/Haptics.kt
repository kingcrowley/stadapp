package cc.stadapp.stad.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import cc.stadapp.stad.core.TagFeedback

/** Short buzzes after a tap: one to lock, two to unlock, three for "nothing changed". */
object Haptics {

    fun play(context: Context, haptic: TagFeedback.Haptic) {
        val timings = when (haptic) {
            TagFeedback.Haptic.One -> longArrayOf(0, 80)
            TagFeedback.Haptic.Two -> longArrayOf(0, 60, 100, 60)
            TagFeedback.Haptic.Three -> longArrayOf(0, 40, 80, 40, 80, 40)
            TagFeedback.Haptic.None -> return
        }
        val vibrator = vibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
}
