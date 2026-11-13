package coil.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Box(
        modifier = modifier.background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = contentDescription,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(24.dp)
        )
    }
}
