package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
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
    val hasPicture = (theme.id == ThemeCatalog.ID_CUSTOM_PHOTO && !customImagePath.isNullOrBlank()) ||
        theme.backgroundRes != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colorScheme.background)
    ) {
        when {
            theme.id == ThemeCatalog.ID_CUSTOM_PHOTO && !customImagePath.isNullOrBlank() -> {
                val file = File(customImagePath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Custom theme background picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            theme.backgroundRes != null -> {
                Image(
                    painter = painterResource(id = theme.backgroundRes),
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
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                theme.colorScheme.background.copy(alpha = (dimAlpha * 0.9f).coerceIn(0.15f, 0.92f)),
                                theme.colorScheme.background.copy(alpha = dimAlpha.coerceIn(0.20f, 0.92f)),
                                theme.colorScheme.background.copy(alpha = (dimAlpha + 0.14f).coerceIn(0.25f, 0.95f))
                            )
                        )
                    )
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
