package cc.stadapp.stad.core

/** The broadcasts after which Stad checks that blocking is still on. Anything else is ignored. */
object BootCheck {
    const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    const val MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"

    fun handles(action: String?): Boolean = action == BOOT_COMPLETED || action == MY_PACKAGE_REPLACED
}
