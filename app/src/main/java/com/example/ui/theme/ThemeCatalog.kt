package com.example.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.R

data class AppPictureTheme(
    val id: String,
    val title: String,
    val titleBn: String,
    val subtitle: String,
    val subtitleBn: String,
    @DrawableRes val backgroundRes: Int?,
    val isLight: Boolean,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val sim1Accent: Color,
    val sim2Accent: Color,
    val colorScheme: ColorScheme
)

object ThemeCatalog {
    const val ID_PURE_BLACK = "pure_black"
    const val ID_PURE_WHITE = "pure_white"
    const val ID_MIDNIGHT_AURORA = "midnight_aurora"
    const val ID_GOLDEN_HORIZON = "golden_horizon"
    const val ID_OCEANIC_MIST = "oceanic_mist"
    const val ID_ROYAL_CRIMSON = "royal_crimson"
    const val ID_CUSTOM_PHOTO = "custom_photo"

    val themes: List<AppPictureTheme> = listOf(
        AppPictureTheme(
            id = ID_MIDNIGHT_AURORA,
            title = "Midnight Aurora",
            titleBn = "মিডনাইট অরোরা",
            subtitle = "Emerald & indigo silk glow",
            subtitleBn = "এমেরাল্ড ও ইন্ডিগো পিকচার থিম",
            backgroundRes = R.drawable.img_theme_midnight_aurora,
            isLight = false,
            accentPrimary = AuroraPrimary,
            accentSecondary = AuroraSecondary,
            sim1Accent = Color(0xFF10B981),
            sim2Accent = Color(0xFF6366F1),
            colorScheme = darkColorScheme(
                primary = AuroraPrimary,
                onPrimary = Color(0xFF032318),
                primaryContainer = Color(0xFF0B3B2D),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = AuroraSecondary,
                onSecondary = Color(0xFF0B1033),
                secondaryContainer = Color(0xFF1E2556),
                onSecondaryContainer = Color(0xFFE0E7FF),
                tertiary = AuroraTertiary,
                background = AuroraBg,
                onBackground = Color(0xFFF1F5F9),
                surface = AuroraSurface,
                onSurface = Color(0xFFF1F5F9),
                surfaceVariant = Color(0xFF18223F),
                onSurfaceVariant = Color(0xFFCBD5E1),
                outline = Color(0xFF334155)
            )
        ),
        AppPictureTheme(
            id = ID_PURE_BLACK,
            title = "Black",
            titleBn = "ব্ল্যাক (কালো)",
            subtitle = "Pure AMOLED black theme",
            subtitleBn = "পিওর অ্যামোলেড ব্ল্যাক থিম",
            backgroundRes = null,
            isLight = false,
            accentPrimary = Color(0xFF10B981),
            accentSecondary = Color(0xFF38BDF8),
            sim1Accent = Color(0xFF10B981),
            sim2Accent = Color(0xFFF59E0B),
            colorScheme = darkColorScheme(
                primary = Color(0xFF10B981),
                onPrimary = Color(0xFF021F15),
                primaryContainer = Color(0xFF0A2E22),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF041D2E),
                secondaryContainer = Color(0xFF0B2940),
                onSecondaryContainer = Color(0xFFBAE6FD),
                tertiary = Color(0xFFF59E0B),
                background = Color(0xFF000000),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF0C0C0E),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF16161A),
                onSurfaceVariant = Color(0xFFCBD5E1),
                outline = Color(0xFF27272A)
            )
        ),
        AppPictureTheme(
            id = ID_PURE_WHITE,
            title = "White",
            titleBn = "হোয়াইট (সাদা)",
            subtitle = "Crisp bright daylight theme",
            subtitleBn = "পরিষ্কার ঝকঝকে সাদা থিম",
            backgroundRes = null,
            isLight = true,
            accentPrimary = Color(0xFF059669),
            accentSecondary = Color(0xFF2563EB),
            sim1Accent = Color(0xFF059669),
            sim2Accent = Color(0xFFD97706),
            colorScheme = lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD1FAE5),
                onPrimaryContainer = Color(0xFF064E3B),
                secondary = Color(0xFF2563EB),
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFFDBEAFE),
                onSecondaryContainer = Color(0xFF1E3A8A),
                tertiary = Color(0xFFD97706),
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = Color(0xFF475569),
                outline = Color(0xFFCBD5E1)
            )
        ),
        AppPictureTheme(
            id = ID_GOLDEN_HORIZON,
            title = "Golden Horizon",
            titleBn = "গোল্ডেন হরাইজন",
            subtitle = "Warm amber & obsidian silk",
            subtitleBn = "সোনালী অ্যাম্বার ও ডার্ক সিল্ক",
            backgroundRes = R.drawable.img_theme_golden_horizon,
            isLight = false,
            accentPrimary = GoldenPrimary,
            accentSecondary = GoldenSecondary,
            sim1Accent = Color(0xFF10B981),
            sim2Accent = Color(0xFFF59E0B),
            colorScheme = darkColorScheme(
                primary = GoldenPrimary,
                onPrimary = Color(0xFF281700),
                primaryContainer = Color(0xFF472C00),
                onPrimaryContainer = Color(0xFFFDE68A),
                secondary = GoldenSecondary,
                onSecondary = Color(0xFF261400),
                secondaryContainer = Color(0xFF3F2305),
                onSecondaryContainer = Color(0xFFFED7AA),
                tertiary = GoldenTertiary,
                background = GoldenBg,
                onBackground = Color(0xFFFAF6F0),
                surface = GoldenSurface,
                onSurface = Color(0xFFFAF6F0),
                surfaceVariant = Color(0xFF2D251B),
                onSurfaceVariant = Color(0xFFD6CEC2),
                outline = Color(0xFF4B3F31)
            )
        ),
        AppPictureTheme(
            id = ID_OCEANIC_MIST,
            title = "Oceanic Mist",
            titleBn = "ওশানিক মিস্ট",
            subtitle = "Deep cyan & seafoam waves",
            subtitleBn = "নীল সাগর ও সায়ান ওয়েভ",
            backgroundRes = R.drawable.img_theme_oceanic_mist,
            isLight = false,
            accentPrimary = OceanPrimary,
            accentSecondary = OceanSecondary,
            sim1Accent = Color(0xFF06B6D4),
            sim2Accent = Color(0xFF38BDF8),
            colorScheme = darkColorScheme(
                primary = OceanPrimary,
                onPrimary = Color(0xFF00252C),
                primaryContainer = Color(0xFF043F4D),
                onPrimaryContainer = Color(0xFFA5F3FC),
                secondary = OceanSecondary,
                onSecondary = Color(0xFF002338),
                secondaryContainer = Color(0xFF093A58),
                onSecondaryContainer = Color(0xFFBAE6FD),
                tertiary = OceanTertiary,
                background = OceanBg,
                onBackground = Color(0xFFF0F9FF),
                surface = OceanSurface,
                onSurface = Color(0xFFF0F9FF),
                surfaceVariant = Color(0xFF123447),
                onSurfaceVariant = Color(0xFFBAE6FD),
                outline = Color(0xFF1E4C63)
            )
        ),
        AppPictureTheme(
            id = ID_ROYAL_CRIMSON,
            title = "Royal Crimson",
            titleBn = "রয়্যাল ক্রিমসন",
            subtitle = "Velvet wine & rose gold",
            subtitleBn = "রোজ গোল্ড ও ডিপ ক্রিমসন",
            backgroundRes = R.drawable.img_theme_royal_crimson,
            isLight = false,
            accentPrimary = CrimsonPrimary,
            accentSecondary = CrimsonSecondary,
            sim1Accent = Color(0xFF10B981),
            sim2Accent = Color(0xFFFB7185),
            colorScheme = darkColorScheme(
                primary = CrimsonPrimary,
                onPrimary = Color(0xFF360511),
                primaryContainer = Color(0xFF541022),
                onPrimaryContainer = Color(0xFFFFD9E2),
                secondary = CrimsonSecondary,
                onSecondary = Color(0xFFFFFFFF),
                secondaryContainer = Color(0xFF470D1D),
                onSecondaryContainer = Color(0xFFFECDD3),
                tertiary = CrimsonTertiary,
                background = CrimsonBg,
                onBackground = Color(0xFFFFF1F2),
                surface = CrimsonSurface,
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF3B1522),
                onSurfaceVariant = Color(0xFFFECDD3),
                outline = Color(0xFF5C2436)
            )
        ),
        AppPictureTheme(
            id = ID_CUSTOM_PHOTO,
            title = "Custom Photo",
            titleBn = "কাস্টম ছবি",
            subtitle = "Your personal gallery picture",
            subtitleBn = "গ্যালারি থেকে আপনার নিজের ছবি",
            backgroundRes = null,
            isLight = false,
            accentPrimary = AuroraPrimary,
            accentSecondary = Color(0xFF38BDF8),
            sim1Accent = Sim1Emerald,
            sim2Accent = Sim2Amber,
            colorScheme = darkColorScheme(
                primary = AuroraPrimary,
                onPrimary = Color(0xFF032318),
                primaryContainer = Color(0xFF0B3B2D),
                onPrimaryContainer = Color(0xFFA7F3D0),
                secondary = Color(0xFF38BDF8),
                onSecondary = Color(0xFF002338),
                secondaryContainer = Color(0xFF093A58),
                onSecondaryContainer = Color(0xFFBAE6FD),
                tertiary = Sim2Amber,
                background = Color(0xFF070B14),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF101726),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1B2436),
                onSurfaceVariant = Color(0xFFCBD5E1),
                outline = Color(0xFF334155)
            )
        )
    )

    fun byId(id: String): AppPictureTheme =
        themes.firstOrNull { it.id == id } ?: themes.first()
}
