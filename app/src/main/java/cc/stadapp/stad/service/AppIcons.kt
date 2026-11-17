package cc.stadapp.stad.service

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

/** Loads app icons as small fixed-size bitmaps, cached. Call [load] off the main thread. */
class AppIcons(context: Context) {

    private val pm = context.packageManager
    private val sizePx = (48 * context.resources.displayMetrics.density).toInt()
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun load(packageName: String): Bitmap? {
        cache.get(packageName)?.let { return it }
        return try {
            pm.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).also { cache.put(packageName, it) }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
