package cc.tomko.outify.ui.model.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val uri: String,
    val resultType: SearchResultType
)

enum class SearchResultType {
    TRACK, ARTIST, EPISODE, PLAYLIST, RADIO
}
