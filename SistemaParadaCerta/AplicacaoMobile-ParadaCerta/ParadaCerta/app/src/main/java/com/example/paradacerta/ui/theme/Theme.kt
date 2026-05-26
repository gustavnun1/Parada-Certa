package com.example.paradacerta.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AzulPrincipal,
    onPrimary = BrancoFundo,
    primaryContainer = AzulClaro,
    onPrimaryContainer = AzulEscuro,

    secondary = VerdePrincipal,
    onSecondary = BrancoFundo,
    secondaryContainer = VerdeClaro,
    onSecondaryContainer = VerdeEscuro,

    background = BrancoFundo,
    onBackground = CinzaEscuro,

    surface = BrancoFundo,
    onSurface = CinzaEscuro,

    surfaceVariant = CinzaClaro,
    onSurfaceVariant = CinzaMedio
)

@Composable
fun ParadaCertaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}