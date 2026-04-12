package com.example.paradacerta.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Banner de anúncio exibido apenas para usuários não-premium.
 *
 * Para integrar com o Google AdMob em produção, substitua o conteúdo
 * deste composable por um AndroidView com AdView configurado com o
 * seu Ad Unit ID do AdMob.
 *
 * Exemplo de integração AdMob:
 *   AndroidView(factory = { ctx ->
 *       AdView(ctx).apply {
 *           setAdSize(AdSize.BANNER)
 *           adUnitId = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
 *           loadAd(AdRequest.Builder().build())
 *       }
 *   })
 *
 * @param isPremium Se true, o banner não é exibido.
 * @param modifier  Modificador externo opcional.
 */
@Composable
fun AdBanner(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPremium) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anúncio",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Espaço reservado para publicidade",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "AD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
