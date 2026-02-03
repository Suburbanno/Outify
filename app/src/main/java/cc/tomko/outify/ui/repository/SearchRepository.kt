package cc.tomko.outify.ui.repository

import cc.tomko.outify.OutifyApplication
import kotlinx.serialization.json.Json

class SearchRepository
 {
     fun searchTracks(query: String): List<String> {
         val api = OutifyApplication.session.spClient
         val json = api.search(query, 0, 3)
         return Json.Default.decodeFromString(json)
     }
}