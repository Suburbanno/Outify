package cc.tomko.outify.core

/**
 * Handles the librespot session
 */
class Session {
    val spClient: SpClient = SpClient()

    external fun initializeSession()
}