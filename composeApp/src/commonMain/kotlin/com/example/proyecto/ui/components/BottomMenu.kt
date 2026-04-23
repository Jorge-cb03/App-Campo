package com.example.proyecto.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.GreenPrimary // IMPORTANTE: Asegúrate de que esta ruta es correcta
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*

sealed class BottomNavItem(val resource: StringResource, val icon: ImageVector, val route: String) {
    object Home : BottomNavItem(Res.string.menu_home, Icons.Default.Home, AppScreens.Home)
    object Garden : BottomNavItem(Res.string.menu_garden, Icons.Default.Eco, "garden/0")
    object Diary : BottomNavItem(Res.string.menu_diary, Icons.Default.Edit, AppScreens.Diary)
    object Products : BottomNavItem(Res.string.menu_products, Icons.Default.ShoppingCart, AppScreens.Products)
    object Profile : BottomNavItem(Res.string.menu_profile, Icons.Default.Person, AppScreens.Profile)
}

@Composable
fun BottomMenu(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Garden,
        BottomNavItem.Diary,
        BottomNavItem.Products,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val title = stringResource(item.resource)

            val isSelected = when (item) {
                BottomNavItem.Home -> currentRoute == AppScreens.Home || currentRoute == AppScreens.Alerts
                BottomNavItem.Garden -> currentRoute?.contains("garden") == true
                BottomNavItem.Diary -> currentRoute?.contains("diary") == true
                BottomNavItem.Products -> currentRoute?.contains("product") == true
                BottomNavItem.Profile -> currentRoute == AppScreens.Profile || currentRoute == AppScreens.About
            }

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = title) },
                label = { Text(title) },
                selected = isSelected,
                // --- CORRECCIÓN DE COLORES ---
                colors = NavigationBarItemDefaults.colors(
                    // Color de la "píldora" de fondo cuando está seleccionado (Verde suave)
                    indicatorColor = GreenPrimary.copy(alpha = 0.2f),
                    // Color del icono cuando está seleccionado (Verde sólido)
                    selectedIconColor = GreenPrimary,
                    // Color del texto cuando está seleccionado (Verde sólido)
                    selectedTextColor = GreenPrimary,
                    // Colores cuando no está seleccionado (Grisáceo del tema)
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(AppScreens.Home) {
                            saveState = false
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
        }
    }
}