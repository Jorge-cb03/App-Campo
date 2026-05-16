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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // LA MAGIA ESTÁ AQUÍ: Comprobamos el color real de la app, no el del móvil.
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A) ||
            MaterialTheme.colorScheme.background == Color(0xFF121212)

    // Un marrón oscuro que destaca perfecto sobre el fondo negro/azulado
    val favoriteColor = if (isDark) Color(0xFF4E342E) else Color(0xFFEFEBE9)
    val favoriteBorder = if (isDark) Color(0xFF6D4C41) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {

        Row(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("${today.dayOfMonth}/${today.monthNumber}", color = MaterialTheme.colorScheme.secondary)
                Text(stringResource(Res.string.home_greeting), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = { navController.navigate(AppScreens.Alerts) }) { Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary) }
        }

        WeatherCard(weatherState)

        Spacer(Modifier.height(30.dp))
        Text(stringResource(Res.string.quick_access_title), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (favoritedGardens.isEmpty()) {
                Text(stringResource(Res.string.garden_no_history), color = Color.Gray, modifier = Modifier.padding(16.dp))
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
                        Column(Modifier.fillMaxSize().padding(12.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
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

@Composable
fun WeatherCard(state: WeatherState) {
    val temp = if (state.isLoading) "..." else "${state.temperature}°C"
    val isNight = state.isDay == 0
    val bgColor = if (isNight) Color(0xFF1E293B) else Color(0xFFF1C40F).copy(alpha = 0.15f)

    val statusText = when(state.weatherCode) {
        0 -> if(isNight) stringResource(Res.string.weather_night) else stringResource(Res.string.weather_sunny)
        in 1..3 -> stringResource(Res.string.weather_cloudy)
        in 51..65 -> stringResource(Res.string.weather_rainy)
        else -> if(state.isLoading) stringResource(Res.string.weather_loading) else "Normal"
    }

    Card(Modifier.fillMaxWidth().height(130.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = bgColor)) {
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(Res.string.weather_title_now), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if(isNight) Color.White else Color.Black)
                Text(temp, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = if(isNight) Color.White else Color.Black)
                Text(statusText, fontWeight = FontWeight.Medium, color = if(isNight) Color.White else Color.Black)
            }
            Icon(if(isNight) Icons.Default.NightsStay else Icons.Default.WbSunny, null, Modifier.size(60.dp), tint = if(isNight) Color.White else Color(0xFFF1C40F))
        }
    }
}