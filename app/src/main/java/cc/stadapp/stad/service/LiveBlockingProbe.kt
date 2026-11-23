package cc.stadapp.stad.service

import android.content.Context
import cc.stadapp.stad.data.BlockingProbe

/** The real [BlockingProbe]: the accessibility setting and Android's exit records. Both are synchronous. */
class LiveBlockingProbe(context: Context) : BlockingProbe {
    private val app = context.applicationContext
    override fun serviceOn() = ServiceStatus.isOn(app)
    override fun forceStoppedAt() = ForceStops.lastAt(app)
}
