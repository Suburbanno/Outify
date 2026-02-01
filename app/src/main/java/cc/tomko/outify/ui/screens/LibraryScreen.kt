package cc.tomko.outify.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.tomko.outify.ui.model.LikedTrack
import cc.tomko.outify.ui.viewmodel.LibraryViewModel
import coil3.compose.AsyncImage

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTrackClick: (LikedTrack) -> Unit,
){
    LazyColumn {
        items(viewModel.tracks) { track ->
            ListItem(
                headlineContent = { Text(track.uri) },
                supportingContent = { Text(track.uri) },
                leadingContent = {
                    AsyncImage(
                        model = "https://i.scdn.co/image/ab67616d000048519efda673310de265a2c1cf1f",
                        contentDescription = null
                    )
                },
                modifier = Modifier.clickable {
                    onTrackClick(track)
                }
            )
        }
    }
}