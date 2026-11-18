package cc.stadapp.stad.core

/** One row in the app picker. [enabled] is false for protected apps and while locked. */
data class PickerRow(
    val app: AppEntry,
    val checked: Boolean,
    val protected: Boolean,
    val enabled: Boolean,
)
