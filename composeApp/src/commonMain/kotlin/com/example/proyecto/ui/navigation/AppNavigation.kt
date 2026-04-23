package com.example.proyecto.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.proyecto.ui.components.BottomMenu
import com.example.proyecto.ui.diary.*
import com.example.proyecto.ui.garden.*
import com.example.proyecto.ui.home.HomeScreen
import com.example.proyecto.ui.login.*
import com.example.proyecto.ui.products.*
import com.example.proyecto.ui.profile.*
import com.example.proyecto.ui.alerts.AlertsScreen

object AppScreens {
    const val Login = "login"
    const val Register = "register"
    const val Home = "home"
    const val Diary = "diary"
    const val Products = "products"
    const val Profile = "profile"
    const val Alerts = "alerts"
    const val About = "about"

    // Rutas con parámetros
    const val AddDiaryEntry = "add_diary_entry/{dateMillis}?taskId={taskId}"
    fun createAddDiaryRoute(dateMillis: Long) = "add_diary_entry/$dateMillis"

    const val DiaryDetail = "diary_detail/{taskId}"
    fun createDiaryDetailRoute(taskId: Long) = "diary_detail/$taskId"

    // RUTAS DE PRODUCTO
    const val AddProduct = "add_product?productId={productId}"
    fun createEditProductRoute(productId: Long) = "add_product?productId=$productId"

    const val ProductDetail = "product_detail/{productId}"
    fun createProductDetailRoute(id: String) = "product_detail/$id"

    const val Garden = "garden/{gardenId}"
    const val GardenSlotDetail = "garden_slot_detail/{bancalId}"
}

@Composable
fun AppNavigation(isDarkTheme: Boolean, onToggleTheme: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Ocultar menú inferior en Login y  Registro
    val showBottomBar = currentRoute != AppScreens.Login &&
            currentRoute != AppScreens.Register


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBottomBar) BottomMenu(navController) }
    ) { innerPadding ->
        NavHost(navController, startDestination = AppScreens.Login, modifier = Modifier.padding(innerPadding)) {

            // Login & Register
            composable(AppScreens.Login) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(AppScreens.Home) { popUpTo(AppScreens.Login) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate(AppScreens.Register) },
                    onGoogleLoginClick = { /* Google Login Logic */ },
                    onGuestLogin = { navController.navigate(AppScreens.Home) { popUpTo(AppScreens.Login) { inclusive = true } } }
                )
            }
            composable(AppScreens.Register) { RegisterScreen({ navController.navigate(AppScreens.Home) { popUpTo(AppScreens.Register) { inclusive = true } } }, { navController.popBackStack() }) }

            // Home & Features
            composable(AppScreens.Home) { HomeScreen(navController) }
            composable(AppScreens.Alerts) { AlertsScreen(navController) }
            composable(AppScreens.Diary) { DiaryScreen(navController) }
            composable(AppScreens.Products) { ProductsScreen(navController) }
            composable(AppScreens.Profile) { ProfileScreen(navController, isDarkTheme, onToggleTheme) }
            composable(AppScreens.About) { AboutScreen(navController) }

            // Garden
            composable(AppScreens.Garden, arguments = listOf(navArgument("gardenId") { type = NavType.LongType })) {
                GardenScreen(navController, it.arguments?.getLong("gardenId") ?: 0L)
            }
            composable(AppScreens.GardenSlotDetail, arguments = listOf(navArgument("bancalId") { type = NavType.StringType })) {
                GardenSlotDetailScreen(navController, it.arguments?.getString("bancalId") ?: "0")
            }

            // Diary Entries
            composable(
                route = AppScreens.AddDiaryEntry,
                arguments = listOf(
                    navArgument("dateMillis") { type = NavType.LongType },
                    navArgument("taskId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val dateMillis = backStackEntry.arguments?.getLong("dateMillis") ?: 0L
                val taskId = backStackEntry.arguments?.getString("taskId")
                AddDiaryEntryScreen(navController, dateMillis, taskId)
            }
            composable(AppScreens.DiaryDetail, arguments = listOf(navArgument("taskId") { type = NavType.LongType })) {
                DiaryDetailScreen(navController, it.arguments?.getLong("taskId") ?: 0L)
            }

            // Products Management
            composable(
                route = AppScreens.AddProduct,
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")?.toLongOrNull()
                AddProductScreen(navController, productId)
            }

            composable(AppScreens.ProductDetail, arguments = listOf(navArgument("productId") { type = NavType.StringType })) {
                ProductDetailScreen(navController, it.arguments?.getString("productId") ?: "0")
            }
        }
    }
}