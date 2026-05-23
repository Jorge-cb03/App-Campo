package com.example.proyecto.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.garden.WeatherState
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*

object ShortcutManager {
    val pinnedGardenIds = mutableStateListOf<Long>()
}

@Composable
fun HomeScreen(navController: NavController, viewModel: GardenViewModel = koinViewModel()) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val jardineras by viewModel.jardineras.collectAsState()
    val favoritedGardens = jardineras.filter { it.esFavorita }
    val weatherState by viewModel.weatherState.collectAsState()

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A) ||
            MaterialTheme.colorScheme.background == Color(0xFF121212)

    val favoriteColor = if (isDark) Color(0xFF4E342E) else Color(0xFFEFEBE9)
    val favoriteBorder = if (isDark) Color(0xFF6D4C41) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppScreens.Chat) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Asistente IA")
            }
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${today.dayOfMonth}/${today.monthNumber}", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        stringResource(Res.string.home_greeting),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = { navController.navigate(AppScreens.Alerts) }) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                }
            }

            // ── WeatherCard mejorado ─────────────────────────────────────
            WeatherCard(weatherState)

            Spacer(Modifier.height(30.dp))
            Text(
                stringResource(Res.string.quick_access_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (favoritedGardens.isEmpty()) {
                    Text(
                        stringResource(Res.string.garden_no_history),
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    favoritedGardens.forEach { garden ->
                        Card(
                            modifier = Modifier.width(150.dp).height(110.dp).clickable {
                                navController.navigate("garden/${garden.id}") { launchSingleTop = true }
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = favoriteColor),
                            border = BorderStroke(1.dp, favoriteBorder)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(12.dp),
                                Arrangement.Center,
                                Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.PushPin, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = garden.nombre,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WEATHER CARD MEJORADA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun WeatherCard(state: WeatherState) {
    val uriHandler = LocalUriHandler.current
    val isNight = state.isDay == 0

    // Colores según día/noche
    val bgColor   = if (isNight) Color(0xFF1E293B)              else Color(0xFFF1C40F).copy(alpha = 0.15f)
    val textColor = if (isNight) Color.White                     else Color(0xFF1A1A1A)
    val subColor  = if (isNight) Color.White.copy(alpha = 0.75f) else Color(0xFF444444)
    val sunColor  = if (isNight) Color.White                     else Color(0xFFF1C40F)

    // Descripción del estado del cielo
    val statusText = when (state.weatherCode) {
        0            -> if (isNight) stringResource(Res.string.weather_night) else stringResource(Res.string.weather_sunny)
        in 1..3      -> stringResource(Res.string.weather_cloudy)
        in 51..65    -> stringResource(Res.string.weather_rainy)
        in 71..77    -> "Nevando"
        in 80..82    -> "Chubascos"
        in 95..99    -> "Tormenta"
        else         -> if (state.isLoading) stringResource(Res.string.weather_loading) else "Normal"
    }

    // Temperatura y sensación
    val tempText      = if (state.isLoading) "..." else "${state.temperature}°C"
    val feelsLikeText = if (state.isLoading) "..." else "${state.feelsLike}°C"

    // Humedad, precipitación, UV — valores del estado (con fallback a "–" si no están)
    val humidityText     = if (state.isLoading) "..." else "${state.humidity}%"
    val precipText       = if (state.isLoading) "..." else "${state.precipitation} mm"
    val uvText           = if (state.isLoading) "..." else uvLabel(state.uvIndex)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Abre Google con la búsqueda de tiempo; detecta la ubicación automáticamente
                uriHandler.openUri("https://www.google.com/search?q=tiempo+ahora+en+mi+ubicacion")
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Fila superior: temperatura grande + icono ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.weather_title_now),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subColor,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = tempText,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        lineHeight = 54.sp
                    )
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = subColor
                    )
                }

                Icon(
                    imageVector = if (isNight) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = sunColor
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Divider sutil ────────────────────────────────────────────
            HorizontalDivider(
                color = if (isNight) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
            )

            Spacer(Modifier.height(16.dp))

            // ── Fila de estadísticas: Sensación | Humedad | Lluvia | UV ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherStatItem(
                    icon  = Icons.Rounded.Thermostat,
                    label = "Sensación",
                    value = feelsLikeText,
                    tint  = subColor,
                    text  = textColor
                )
                WeatherStatItem(
                    icon  = Icons.Rounded.WaterDrop,
                    label = "Humedad",
                    value = humidityText,
                    tint  = subColor,
                    text  = textColor
                )
                WeatherStatItem(
                    icon  = Icons.Rounded.Umbrella,
                    label = "Lluvia",
                    value = precipText,
                    tint  = subColor,
                    text  = textColor
                )
                WeatherStatItem(
                    icon  = Icons.Rounded.LightMode,
                    label = "Índice UV",
                    value = uvText,
                    tint  = subColor,
                    text  = textColor
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Pie: toque para más info ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ver más en Google  →",
                    fontSize = 11.sp,
                    color = subColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** Pequeño bloque icono + etiqueta + valor para la fila de stats */
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

/** Convierte el índice UV numérico en una etiqueta legible */
private fun uvLabel(uv: Double): String = when {
    uv < 3  -> "Bajo"
    uv < 6  -> "Moderado"
    uv < 8  -> "Alto"
    uv < 11 -> "Muy alto"
    else    -> "Extremo"
}