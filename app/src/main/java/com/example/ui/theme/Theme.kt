package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.AppLanguage

val LocalPictureTheme = staticCompositionLocalOf { ThemeCatalog.themes.first() }

@Composable
fun ShajamalTheme(
    pictureTheme: AppPictureTheme = ThemeCatalog.themes.first(),
    language: AppLanguage = AppLanguage.ENGLISH,
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

    CompositionLocalProvider(
        LocalPictureTheme provides pictureTheme,
        LocalAppStrings provides localizedStrings
    ) {
        MaterialTheme(
            colorScheme = pictureTheme.colorScheme,
            typography = ShajamalTypography,
            content = content
        )
    }
}
