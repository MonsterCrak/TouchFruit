package com.jlls.touchfruit.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.jlls.touchfruit.data.repository.TouchFruitRepository
import com.jlls.touchfruit.ui.components.BotonPrincipal
import com.jlls.touchfruit.ui.components.CampoTexto
import com.jlls.touchfruit.ui.theme.*

// ===============================
// LOGIN SCREEN
// ===============================

@Composable
fun LoginScreen(
    onLoginExito: (Boolean) -> Unit // true = emisor, false = receptor
) {
    var codigo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgCanvas)
            .padding(SpacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Título
        Text(
            text = "🍊 TouchFruit",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(SpacingXl))

        // Campo código
        CampoTexto(
            valor = codigo,
            onValueChange = {
                codigo = it.uppercase()
                showError = false
            },
            placeholder = "Ingresa tu código",
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            Spacer(modifier = Modifier.height(SpacingSm))
            Text(
                text = "Código no válido. Usa código que empiece con E (emisor) o R (receptor)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(SpacingXl))

        // Botón entrar
        BotonPrincipal(
            texto = if (isLoading) "Verificando..." else "ENTRAR",
            enabled = codigo.length >= 1 && !isLoading,
            onClick = {
                isLoading = true
                val success = TouchFruitRepository.login(codigo)
                isLoading = false
                if (success) {
                    val esEmisor = codigo.startsWith("E")
                    onLoginExito(esEmisor)
                } else {
                    showError = true
                }
            }
        )

        Spacer(modifier = Modifier.height(SpacingXl))

        // Hint
        Text(
            text = "¿Olvidaste tu código?",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(SpacingMd))

        // Demo hint
        Surface(
            shape = ShapeCard,
            color = CardBlue
        ) {
            Column(
                modifier = Modifier.padding(SpacingMd),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Demo:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "E1 = Emisor | R1 = Receptor",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}