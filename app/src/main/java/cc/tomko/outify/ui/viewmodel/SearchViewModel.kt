package cc.tomko.outify.ui.viewmodel

import androidx.lifecycle.ViewModel
import cc.tomko.outify.ui.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow

class SearchViewModel(
    repository: SearchRepository
): ViewModel() {
    private val query = MutableStateFlow("")

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }
}