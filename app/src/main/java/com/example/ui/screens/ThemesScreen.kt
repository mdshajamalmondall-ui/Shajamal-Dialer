package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppLanguage
import com.example.data.model.DialerSettings
import com.example.data.model.DoodleDesign
import com.example.ui.components.DoodleCanvas
import com.example.ui.theme.AppPictureTheme
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalPictureTheme
import com.example.ui.theme.ThemeCatalog
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ThemesScreen(
    settings: DialerSettings,
    onSelectTheme: (String) -> Unit,
    onSelectDoodle: (DoodleDesign) -> Unit,
    onCustomPhotoPicked: (android.net.Uri) -> Unit,
    onDimAlphaChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val strings = LocalAppStrings.current
    val activeTheme = LocalPictureTheme.current
    val isBn = settings.language == AppLanguage.BENGALI

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onCustomPhotoPicked(uri)
            }
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("themes_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = strings.themesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = strings.themesSubtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            items(ThemeCatalog.themes, key = { it.id }) { theme ->
                val isSelected = settings.selectedThemeId == theme.id
                ThemeGalleryCard(
                    theme = theme,
                    isBn = isBn,
                    isSelected = isSelected,
                    customImagePath = settings.customThemeImagePath,
                    onClick = {
                        if (theme.id == ThemeCatalog.ID_CUSTOM_PHOTO) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            onSelectTheme(theme.id)
                        }
                    }
                )
            }

            item(span = { GridItemSpan(2) }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_custom_photo_theme_button")
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.chooseCustomPhoto)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.backgroundDimming,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(settings.overlayDimAlpha * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = settings.overlayDimAlpha,
                            onValueChange = onDimAlphaChange,
                            valueRange = 0.15f..0.88f
                        )
                    }
                }
            }

            // Doodle & Background Design Section Header
            item(span = { GridItemSpan(2) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = strings.doodleSectionTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = strings.doodleSectionSubtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(DoodleDesign.entries, key = { "doodle_${it.id}" }) { doodle ->
                DoodleGalleryCard(
                    doodle = doodle,
                    theme = activeTheme,
                    isBn = isBn,
                    isSelected = settings.selectedDoodle == doodle,
                    onClick = { onSelectDoodle(doodle) }
                )
            }
        }
    }
}

@Composable
private fun ThemeGalleryCard(
    theme: AppPictureTheme,
    isBn: Boolean,
    isSelected: Boolean,
    customImagePath: String?,
    onClick: () -> Unit
) {
    val title = if (isBn) theme.titleBn else theme.title
    val subtitle = if (isBn) theme.subtitleBn else theme.subtitle

    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) theme.accentPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        color = theme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("theme_card_${theme.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                theme.id == ThemeCatalog.ID_CUSTOM_PHOTO && !customImagePath.isNullOrBlank() -> {
                    val file = File(customImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                theme.backgroundRes != null -> {
                    Image(
                        painter = painterResource(id = theme.backgroundRes),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                theme.id == ThemeCatalog.ID_CUSTOM_PHOTO -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddPhotoAlternate,
                            contentDescription = title,
                            tint = theme.accentPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                theme.id == ThemeCatalog.ID_PURE_BLACK || theme.id == ThemeCatalog.ID_PURE_WHITE -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 28.dp)
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(theme.accentPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(theme.accentPrimary)
                        )
                    }
                }
            }

            if (!theme.isLight) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.78f)
                                )
                            )
                        )
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = theme.accentPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected theme",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (theme.isLight) Color(0xFF0F172A) else Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (theme.isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun DoodleGalleryCard(
    doodle: DoodleDesign,
    theme: AppPictureTheme,
    isBn: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = if (isBn) doodle.titleBn else doodle.titleEn
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 2.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        color = theme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("doodle_card_${doodle.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (doodle == DoodleDesign.NONE) {
                Icon(
                    imageVector = Icons.Rounded.HideImage,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 22.dp)
                        .size(30.dp)
                )
            } else {
                DoodleCanvas(
                    design = doodle,
                    tintColor = theme.accentSecondary,
                    isLightTheme = theme.isLight,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Selected doodle",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        if (theme.isLight) Color.White.copy(alpha = 0.88f) else Color.Black.copy(alpha = 0.62f)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (theme.isLight) Color(0xFF0F172A) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
