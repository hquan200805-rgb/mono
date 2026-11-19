package com.chitieu.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Theme theo quy tắc 60-30-10.
 * Tắt dynamicColor mặc định để giữ màu thương hiệu ổn định.
 */
private val LightColorScheme = lightColorScheme(
    // 30% Brand
    primary = Brand,
    onPrimary = OnBrand,
    primaryContainer = BrandContainer,
    onPrimaryContainer = OnBrandContainer,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    // 10% Accent → dùng tertiary làm CTA
    tertiary = Accent,
    onTertiary = OnAccent,
    tertiaryContainer = BrandContainer,
    onTertiaryContainer = OnBrandContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    // 60% Dominant
    background = DominantLight,
    onBackground = TextPrimary,
    surface = DominantSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandDark,
    onPrimary = OnBrandDark,
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = OnBrandContainerDark,

    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextSecondaryDark,

    tertiary = BrandDark,
    onTertiary = OnBrandDark,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    background = DominantDark,
    onBackground = TextPrimaryDark,
    surface = DominantSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark
)

@Composable
fun ChiTieuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Giữ false để màu brand ổn định, không bị hệ thống ghi đè
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChiTieuTypography,
        content = content
    )
}
