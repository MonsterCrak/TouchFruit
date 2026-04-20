package com.jlls.touchfruit.ui.theme

import androidx.compose.ui.graphics.Color

// ===============================
// TOUCHFRUIT DESIGN SYSTEM
// Modern Light / Pastel Cards UI
// ===============================

// Fondos y Superficies
val BgCanvas = Color(0xFFE5E5EA)          // Fondo del entorno/pantalla exterior
val BgSurface = Color(0xFFFFFFFF)          // Pantalla móvil y tarjetas principales
val BgSearch = Color(0xFFF5F5F5)           // Barra de búsqueda

// Tipografía
val TextPrimary = Color(0xFF000000)       // Títulos principales
val TextSecondary = Color(0xFF6B7280)       // Placeholders, metadatos

// Tarjetas Pastel (Acciones)
val CardBlue = Color(0xFFE0F2FE)          // Scan, acciones frías
val CardGreen = Color(0xFFDCFCE7)           // Éxito,abierta
val CardYellow = Color(0xFFFEF08A)         // Ask AI, atención
val CardPurple = Color(0xFFF3E8FF)          // Split, Merge, etc.
val CardRed = Color(0xFFEF4444)            // Cerrada, rojo intenso
val CardOrange = Color(0xFFFF8C00)          // Actualizar comanda

// Elementos Interactivos
val NotificationDot = Color(0xFFFF6B6B)     // Notificaciones (campanita)
val ActionBg = Color(0xFF111111)            // Botones principales
val ActionFg = Color(0xFFFFFFFF)           // Iconos sobre botones oscuros
val IconStroke = Color(0xFF1C1C1E)         // Iconos de línea en tarjetas

// Estados de Pedido
val EstadoNuevo = CardBlue
val EstadoEnPreparacion = CardYellow
val EstadoListo = CardGreen

// Fondo para la app
val BackgroundLight = BgCanvas

// Color primario para navegación/等重点
val PrimaryLight = Color(0xFF1C1C1E)