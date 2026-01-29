package cc.tomko.outify.ui.model

sealed interface PlayerAction {
    data object PlayPause: PlayerAction
    data object Next: PlayerAction
    data object Previous: PlayerAction
}