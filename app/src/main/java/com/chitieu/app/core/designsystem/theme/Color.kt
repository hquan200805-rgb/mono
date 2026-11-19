package com.chitieu.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Hệ màu 60-30-10 cho app tài chính cá nhân.
 *
 * 60% – Dominant (nền): sáng trung tính, dễ nhìn số liệu lâu
 * 30% – Brand / Secondary: Teal – chuyên nghiệp, đáng tin cậy
 * 10% – Accent: dùng cho CTA (nút Thêm giao dịch, xác nhận…)
 */

// ---------- 60% DOMINANT (Background / Surface) ----------
val DominantLight = Color(0xFFF5F7FA)          // Xám nhạt chính
val DominantSurface = Color(0xFFFFFFFF)       // Trắng cho card / surface
val DominantDark = Color(0xFF0F172A)          // Nền tối dịu
val DominantSurfaceDark = Color(0xFF1E293B)

// ---------- 30% BRAND (Primary / Secondary) ----------
val Brand = Color(0xFF0D9488)                 // Teal 600 – màu thương hiệu
val OnBrand = Color(0xFFFFFFFF)
val BrandContainer = Color(0xFFCCFBF1)        // Teal rất nhạt
val OnBrandContainer = Color(0xFF134E4A)

val BrandDark = Color(0xFF2DD4BF)             // Teal sáng hơn cho dark mode
val OnBrandDark = Color(0xFF134E4A)
val BrandContainerDark = Color(0xFF115E59)
val OnBrandContainerDark = Color(0xFFCCFBF1)

// Secondary (hỗ trợ brand – slate)
val Secondary = Color(0xFF64748B)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE2E8F0)
val OnSecondaryContainer = Color(0xFF1E293B)

// ---------- 10% ACCENT (CTA) ----------
val Accent = Color(0xFF0F766E)                // Teal đậm hơn một chút – nổi bật nhưng vẫn cùng họ
val OnAccent = Color(0xFFFFFFFF)
// Có thể dùng Amber nếu muốn CTA nóng hơn:
// val Accent = Color(0xFFF59E0B)

// ---------- Semantic ----------
val Error = Color(0xFFDC2626)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFEE2E2)
val OnErrorContainer = Color(0xFF7F1D1D)

val Success = Color(0xFF16A34A)
val Income = Color(0xFF16A34A)                // Thu
val Expense = Color(0xFFDC2626)               // Chi

// Text
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFFCBD5E1)

// Surface variant
val SurfaceVariantLight = Color(0xFFE2E8F0)
val SurfaceVariantDark = Color(0xFF334155)
