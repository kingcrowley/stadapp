package cc.stadapp.stad.data

import cc.stadapp.stad.core.BlockList
import kotlinx.coroutines.flow.Flow

/** Stores the user's block list on the device. */
interface BlockListRepository {
    val blockList: Flow<BlockList>

    /**
     * Atomically replaces the stored list with `transform(current)`. Use this for every change,
     * so a toggle and a clean-up can't overwrite each other.
     */
    suspend fun update(transform: (BlockList) -> BlockList)
}
