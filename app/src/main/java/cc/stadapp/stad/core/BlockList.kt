package cc.stadapp.stad.core

/** The set of package names the user has chosen to block. Immutable. */
@JvmInline
value class BlockList(val packages: Set<String> = emptySet()) {
    val size: Int get() = packages.size
    fun isEmpty(): Boolean = packages.isEmpty()
    operator fun contains(packageName: String): Boolean = packageName in packages

    /** Returns a list with [packageName] added if absent, removed if present. */
    fun toggled(packageName: String): BlockList =
        if (packageName in packages) BlockList(packages - packageName)
        else BlockList(packages + packageName)

    fun with(packageName: String, blocked: Boolean): BlockList =
        if (blocked) BlockList(packages + packageName) else BlockList(packages - packageName)
}
