package com.example.proyecto.ui.theme

import androidx.compose.ui.graphics.Color

// ==================================================================
// PALETA MODERNA MINIMALISTA
// ==================================================================

// Color de Acento (Un verde esmeralda vibrante y tecnológico)
val MinimalAccent = Color(0xFF10B981)

// Escala de Grises (Modo Claro)
val LightBackground = Color(0xFFF8FAFC) // Blanco roto muy limpio
val LightSurface = Color(0xFFFFFFFF)    // Blanco puro
val LightText = Color(0xFF0F172A)       // Casi negro
val LightTextSecondary = Color(0xFF64748B)
val LightOutline = Color(0xFFE2E8F0)    // Bordes sutiles

// Escala de Grises (Modo Oscuro)
val DarkBackground = Color(0xFF0F172A)  // Azul noche muy oscuro
val DarkSurface = Color(0xFF1E293B)     // Gris oscuro
val DarkText = Color(0xFFF8FAFC)        // Blanco
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkOutline = Color(0xFF334155)
val TierraSlot = Color(0xFFEFEBE9)

// NUEVO: Marrón oscuro para modo noche (resalta sobre el fondo azul noche)
val TierraSlotDark = Color(0xFF3E2723)
val TierraSlotBorder = Color(0xFFD7CCC8)

// ==================================================================
// COMPATIBILIDAD (Para que la app no crashee)
// ==================================================================
val GreenPrimary = MinimalAccent
val GreenSecondary = Color(0xFFD1FAE5) // Esmeralda muy claro
val RedDanger = Color(0xFFEF4444)      // Rojo moderno y limpio