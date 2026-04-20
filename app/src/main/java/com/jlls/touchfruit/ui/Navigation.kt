package com.jlls.touchfruit.ui

// ===============================
// APP NAVIGATION
// ===============================

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object EmisorDashboard : Screen("emisor/dashboard")
    object EmisorNuevaComanda : Screen("emisor/nueva_comanda")
    object EmisorHistorial : Screen("emisor/historial")
    object EmisorResumenSesion : Screen("emisor/resumen_sesion")
    object ReceptorDashboard : Screen("receptor/dashboard")
    object ReceptorPedidos : Screen("receptor/pedidos")
    object ReceptorReportes : Screen("receptor/reportes")
}