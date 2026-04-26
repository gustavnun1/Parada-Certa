package com.example.paradacerta.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SeloAmbar = Color(0xFFFF8F00)
val SeloFundo = Color(0xFFFFF8E1)

@Composable
fun SeloQualidade(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = SeloFundo,
        border = BorderStroke(1.dp, SeloAmbar)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Selo de qualidade",
                tint = SeloAmbar,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Qualidade Verificada",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SeloAmbar
            )
        }
    }
}
