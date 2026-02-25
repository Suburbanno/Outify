package cc.tomko.outify.ui.components.rows

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cc.tomko.outify.data.Playlist
import coil3.compose.AsyncImage

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    artworkUrl: String?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.attributes.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2
        )
    }
}