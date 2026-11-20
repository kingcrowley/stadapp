package cc.stadapp.stad.core

/**
 * What the blocking service does when it (re)connects, e.g. after a reboot or an update of Stad.
 *
 * The service only learns which app is in front from the next window event, and can't ask
 * (reading other windows needs window-content access, which Stad doesn't request). So a blocked
 * app already on screen would stay usable. While that's possible, each new lock state or block
 * list is checked here until the foreground app is known or Stad has gone Home once.
 */
object ConnectCheck {

    enum class Action {
        /** Go to the home screen: the app in front is unknown and might be blocked. */
        GoHome,
        /** Check the app Stad already saw (a window event arrived before storage was read). */
        EvaluateForeground,
        Nothing,
    }

    fun decide(lockState: LockState, blockListEmpty: Boolean, foregroundKnown: Boolean, homeSent: Boolean): Action =
        when {
            !lockState.isLocked || blockListEmpty -> Action.Nothing
            foregroundKnown -> Action.EvaluateForeground
            homeSent -> Action.Nothing
            else -> Action.GoHome
        }
}
