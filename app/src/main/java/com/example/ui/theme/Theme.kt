package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.example.data.model.AppLanguage

val LocalPictureTheme = staticCompositionLocalOf { ThemeCatalog.themes.first() }

@Composable
fun ShajamalTheme(
    pictureTheme: AppPictureTheme = ThemeCatalog.themes.first(),
    language: AppLanguage = AppLanguage.ENGLISH,
    displayScale: Float = 1.0f,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = pictureTheme.isLight
            controller.isAppearanceLightNavigationBars = pictureTheme.isLight
        }
    }

    val localizedStrings = stringsFor(language)
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, displayScale, fontScale) {
        Density(
            density = baseDensity.density * displayScale.coerceIn(0.85f, 1.20f),
            fontScale = baseDensity.fontScale * fontScale.coerceIn(0.85f, 1.35f)
        )
    }

    CompositionLocalProvider(
        LocalPictureTheme provides pictureTheme,
        LocalAppStrings provides localizedStrings,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = pictureTheme.colorScheme,
            typography = ShajamalTypography,
            content = content
        )
    }
}
