package cc.tomko.outify.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOverlay(
    active: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    if(!active) return;

    AnimatedVisibility(
        visible = active,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column {
                SearchBar(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { _ -> },
                    active = active,
                    onActiveChange = { isActive ->
                        if (!isActive) {
                            onClose()
                        }
                    },
                    placeholder = { Text("Search Spotify") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        item {
                            Text(
                                text = "something",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}