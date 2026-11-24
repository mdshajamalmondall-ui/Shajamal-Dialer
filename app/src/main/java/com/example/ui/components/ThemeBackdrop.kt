package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.DoodleDesign
import com.example.ui.theme.AppPictureTheme
import com.example.ui.theme.ThemeCatalog
import java.io.File

@Composable
fun ThemeBackdrop(
    theme: AppPictureTheme,
    doodleDesign: DoodleDesign = DoodleDesign.COSMIC_STAR,
    customImagePath: String?,
    dimAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val customFile = remember(theme.id, customImagePath) {
        if (theme.id == ThemeCatalog.ID_CUSTOM_PHOTO && !customImagePath.isNullOrBlank()) {
            File(customImagePath).takeIf { it.exists() }
        } else {
            null
        }
    }
    val hasPicture = customFile != null || theme.backgroundRes != null
    val dimBrush = remember(theme.colorScheme.background, dimAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                theme.colorScheme.background.copy(alpha = (dimAlpha * 0.9f).coerceIn(0.15f, 0.92f)),
                theme.colorScheme.background.copy(alpha = dimAlpha.coerceIn(0.20f, 0.92f)),
                theme.colorScheme.background.copy(alpha = (dimAlpha + 0.14f).coerceIn(0.25f, 0.95f))
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colorScheme.background)
    ) {
        when {
            customFile != null -> {
                val request = remember(customFile) {
                    ImageRequest.Builder(context)
                        .data(customFile)
                        .size(1080)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = "Custom theme background picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            theme.backgroundRes != null -> {
                val request = remember(theme.backgroundRes) {
                    ImageRequest.Builder(context)
                        .data(theme.backgroundRes)
                        .size(1080)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = "${theme.title} theme background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (hasPicture) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dimBrush)
            )
        }

        DoodleCanvas(
            design = doodleDesign,
            tintColor = theme.accentSecondary,
            isLightTheme = theme.isLight
        )

        content()
    }
}
