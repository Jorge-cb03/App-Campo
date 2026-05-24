package com.example.proyecto.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.proyecto.ui.navigation.AppScreens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*

sealed class BottomNavItem(val resource: StringResource, val icon: ImageVector, val route: String) {
    object Home     : BottomNavItem(Res.string.menu_home,     Icons.Default.Home,          AppScreens.Home)
    object Garden   : BottomNavItem(Res.string.menu_garden,   Icons.Default.Eco,           "garden/0")
    object Diary    : BottomNavItem(Res.string.menu_diary,    Icons.Default.CalendarMonth, AppScreens.Diary)
    object Animals  : BottomNavItem(Res.string.menu_animals,  Icons.Default.Pets,          AppScreens.Animals)
    object Products : BottomNavItem(Res.string.menu_products, Icons.Default.Inventory2,    AppScreens.Products)
    object Profile  : BottomNavItem(Res.string.menu_profile,  Icons.Default.Person,        AppScreens.Profile)
}

@Composable
fun BottomMenu(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Garden,
        BottomNavItem.Animals,
        BottomNavItem.Diary,
        BottomNavItem.Products,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding(),
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: ""

        items.forEach { item ->
            val title = stringResource(item.resource)

            val isSelected = when (item) {
                BottomNavItem.Home     -> currentRoute == AppScreens.Home || currentRoute == AppScreens.Alerts
                BottomNavItem.Garden   -> currentRoute.contains("garden") || currentRoute.contains("bancal")
                BottomNavItem.Animals  -> currentRoute.contains("animal")
                BottomNavItem.Diary    -> currentRoute.contains("diary") || currentRoute.contains("add_diary")
                BottomNavItem.Products -> currentRoute.contains("product") || currentRoute.contains("add_prod")
                BottomNavItem.Profile  -> currentRoute == AppScreens.Profile || currentRoute == AppScreens.About
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = isSelected,
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                            inclusive = false
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}