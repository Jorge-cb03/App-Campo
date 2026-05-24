package com.example.proyecto.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.ui.garden.GardenViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import androidx.navigation.NavGraph.Companion.findStartDestination


object ShortcutManager {
    val pinnedGardenIds = mutableStateListOf<Long>()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: GardenViewModel = koinViewModel()
) {
    val jardineras by viewModel.jardineras.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current

    // Filtrar jardineras favoritas
    val jardinerasFavoritas = jardineras.filter { it.esFavorita }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name), fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── WIDGET DEL CLIMA ──
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Clima Actual", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (weatherState.isDay == 1) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                                    contentDescription = null,
                                    tint = if (weatherState.isDay == 1) Color(0xFFFFA000) else Color(0xFF5E35B1),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text("${weatherState.temperature} °C", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WeatherStatItem(Icons.Rounded.Air, "Viento", "Normal", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onSurface)
                        WeatherStatItem(Icons.Rounded.WaterDrop, "Humedad", "Media", Color(0xFF0288D1), MaterialTheme.colorScheme.onSurface)
                        WeatherStatItem(Icons.Rounded.BrightnessHigh, "Índice UV", uvLabel(5.0), Color(0xFFE64A19), MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Pie de más info nativo con clic a Google
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            try { uriHandler.openUri("https://weather.com") } catch (e: Exception) {}
                        },
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ver más en Google  →",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── JARDINERAS FAVORITAS ──
            Text("Jardineras Favoritas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            if (jardinerasFavoritas.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tienes jardineras marcadas como favoritas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 16.dp)
                ) {
                    items(jardinerasFavoritas) { jardinera ->
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    navController.navigate("garden/${jardinera.id}") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalFlorist, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(Modifier.height(12.dp))
                                Text(jardinera.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                                Text("${jardinera.filas * jardinera.columnas} Bancales", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── ACCESOS DIRECTOS ──
            Text("Acciones Rápidas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickActionCard(
                    title = "Añadir Tarea",
                    icon = Icons.Rounded.EditCalendar,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                ) {
                    val epoch = Clock.System.now().toEpochMilliseconds()
                    navController.navigate("add_diary_entry/$epoch")
                }
                QuickActionCard(
                    title = "Chat IA",
                    icon = Icons.Rounded.SmartToy,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                ) {
                    navController.navigate("chat")
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun QuickActionCard(title: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    }
}

@Composable
private fun WeatherStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    text: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = text)
        Text(text = label, fontSize = 10.sp, color = tint)
    }
}

private fun uvLabel(uv: Double): String = when {
    uv < 3.0  -> "Bajo"
    uv < 6.0  -> "Moderado"
    uv < 8.0  -> "Alto"
    else -> "Extremo"
}