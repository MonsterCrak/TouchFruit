package com.jlls.touchfruit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jlls.touchfruit.ui.theme.*

// ===============================
// TARJETA PASTEL (Action Card)
// ===============================

@Composable
fun TarjetaPastel(
    titulo: String,
    icono: String,
    colorFondo: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(100.dp),
        shape = ShapeCard,
        color = colorFondo
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingMd),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icono,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(SpacingSm))
            Text(
                text = titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ===============================
// BOTON PRINCIPAL (Primary Button)
// ===============================

@Composable
fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = ActionBg,
            contentColor = ActionFg,
            disabledContainerColor = ActionBg.copy(alpha = 0.5f),
            disabledContentColor = ActionFg.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ===============================
// BOTON SECUNDARIO (Secondary Button)
// ===============================

@Composable
fun BotonSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = ActionBg,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = ShapePill,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = color
        ),
        border = BorderStroke(1.5.dp, color)
    ) {
        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

// ===============================
// BOTON RELLENO (Filled Button custom color)
// ===============================

@Composable
fun BotonRelleno(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = ActionBg,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor,
            disabledContainerColor = color.copy(alpha = 0.5f),
            disabledContentColor = textColor.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = texto,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// ===============================
// PILLS DE CATEGORIA
// ===============================

@Composable
fun CategoriaPill(
    categoria: String,
    emoji: String,
    seleccionado: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ShapePill,
        color = if (seleccionado) ActionBg else BgSurface,
        border = if (!seleccionado) {
            androidx.compose.foundation.BorderStroke(1.dp, BgSearch)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = categoria,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (seleccionado) ActionFg else TextPrimary
            )
        }
    }
}

// ===============================
// CELDA MESA (pequeña)
// ===============================

@Composable
fun CeldaMesa(
    mesaId: Int,
    estaAbierta: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = 64.dp, minHeight = 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (estaAbierta) CardGreen else BgSurface,
        border = if (estaAbierta) null else androidx.compose.foundation.BorderStroke(1.dp, BgSearch)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = mesaId.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (estaAbierta) "🟢" else "🔴",
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ===============================
// CELDA MESA GRANDE (para dashboard)
// ===============================

@Composable
fun CeldaMesaGrande(
    mesaId: Int,
    estaAbierta: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val borderColor = if (estaAbierta) CardGreen else CardRed
    val borderWidth = if (estaAbierta) 4.dp else 3.dp

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .defaultMinSize(minWidth = 100.dp, minHeight = 100.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (estaAbierta) Color(0xFFDCFCE7) else Color(0xFFFFFFFF),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = if (estaAbierta) 6.dp else 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Número de mesa
            Text(
                text = mesaId.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Estado texto
            Text(
                text = if (estaAbierta) "ABIERTA" else "CERRADA",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (estaAbierta) Color(0xFF16A34A) else Color(0xFFDC2626)
            )

            // Emoji indicador
            Text(
                text = if (estaAbierta) "🟢" else "🔴",
                fontSize = 20.sp
            )
        }
    }
}

// ===============================
// TARJETA DE PRODUCTO (horizontal row)
// ===============================

@Composable
fun TarjetaProductoRow(
    nombre: String,
    precio: Double,
    cantidad: Int,
    onMas: () -> Unit,
    onMenos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCard,
        color = BgSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingMd),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nombre y precio
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "$${precio.toInt()}",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(SpacingMd))

            // Stepper controls
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cantidad == 0) {
                    BotonCantidad(texto = "+", onClick = onMas)
                } else {
                    BotonCantidad(texto = "-", onClick = onMenos)
                    Spacer(modifier = Modifier.width(SpacingSm))
                    Text(
                        text = cantidad.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(SpacingSm))
                    BotonCantidad(texto = "+", onClick = onMas)
                }
            }
        }
    }
}

// ===============================
// STEPER DE CANTIDAD (reutilizable)
// ===============================

@Composable
fun StepperCantidad(
    cantidad: Int,
    onMas: () -> Unit,
    onMenos: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        BotonCantidad(texto = "-", onClick = onMenos)
        Spacer(modifier = Modifier.width(SpacingSm))
        Text(
            text = cantidad.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(SpacingSm))
        BotonCantidad(texto = "+", onClick = onMas)
    }
}

// ===============================
// BOTON CANTIDAD (reutilizable)
// ===============================

@Composable
fun BotonCantidad(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = ShapePill,
        color = ActionBg
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = texto,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ActionFg
            )
        }
    }
}

// ===============================
// ITEM EN RESUMEN DE COMANDA
// ===============================

@Composable
fun ItemResumenComanda(
    nombre: String,
    cantidad: Int,
    subtotal: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface, ShapeCard)
            .padding(SpacingMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "x$cantidad",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "$${subtotal.toInt()}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ===============================
// BARRA DE NAVEGACION INFERIOR
// ===============================

@Composable
fun NavegacionInferior(
    itemActivo: Int, // 0=Home, 1=second, 2=Cerrar
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    segundoItemLabel: String = "Historial" // customizable label for index 1
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BgSurface, ShapePill)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val items = listOf("🏠" to "Home", "📋" to segundoItemLabel, "🚪" to "Cerrar")
        items.forEachIndexed { index, (emoji, label) ->
            val seleccionado = itemActivo == index
            Surface(
                onClick = { onItemClick(index) },
                shape = ShapePill,
                color = if (seleccionado) ActionBg else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (seleccionado) ActionFg else TextSecondary
                    )
                }
            }
        }
    }
}

// ===============================
// INPUT FIELD
// ===============================

@Composable
fun CampoTexto(
    valor: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        value = valor,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                color = TextSecondary
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = BgSearch,
            unfocusedContainerColor = BgSearch,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = ShapeCard,
        singleLine = true
    )
}