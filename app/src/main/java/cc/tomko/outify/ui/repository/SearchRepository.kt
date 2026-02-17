package cc.tomko.outify.ui.repository

import cc.tomko.outify.OutifyApplication
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.ui.model.search.SearchResult
import cc.tomko.outify.ui.model.search.SearchResultType
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val spClient: SpClient
)
 {
     val json = Json { ignoreUnknownKeys = true }

     fun search(query: String): List<SearchResult> {
         val encodedQuery = query.replace(" ", "+")
         val json_raw = spClient.search(encodedQuery, 0, 3)
         val uris = json.decodeFromString<List<String>>(json_raw)

         return uris.map { SearchResult(it, SearchResultType.TRACK) }
     }
}