package com.example.proyecto.ui.garden

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proyecto.domain.model.EstadoJardinera
import com.example.proyecto.domain.model.Jardinera

@Composable
fun JardineraCard(
    jardinera: Jardinera,
    onClick: () -> Unit
) {
    val cardColor = when (jardinera.estado) {
        EstadoJardinera.VACIO -> MaterialTheme.colorScheme.surfaceVariant
        EstadoJardinera.OCUPADO -> MaterialTheme.colorScheme.primaryContainer
        EstadoJardinera.ENFERMO -> MaterialTheme.colorScheme.errorContainer
    }

    Card(
        modifier = Modifier
            .padding(4.dp) // MENOS MARGEN EXTERNO (antes era 8.dp)
            .aspectRatio(1f) // Se mantiene cuadrada
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp), // MENOS PADDING INTERNO
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Emoji más pequeño
            Text(
                text = jardinera.icon,
                fontSize = 28.sp // Antes era 40.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Título (Cultivo) más pequeño
            Text(
                text = jardinera.cultivo ?: "Vacío",
                style = MaterialTheme.typography.labelLarge, // Letra más compacta
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1, // Si es muy largo, corta con "..."
                overflow = TextOverflow.Ellipsis
            )

            // 3. Subtítulo (Nombre) más pequeño
            Text(
                text = jardinera.nombre,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}