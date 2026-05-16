package com.example.proyecto.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.ProductoEntity
import com.example.proyecto.domain.model.ProductType
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.RedDanger
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navController: NavController, productId: String, viewModel: GardenViewModel = koinViewModel()) {
    val idLong = productId.toLongOrNull() ?: return
    var producto by remember { mutableStateOf<ProductoEntity?>(null) }
    var expandedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(idLong) { producto = viewModel.getProductoById(idLong) }
    if (producto == null) return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalle", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { expandedMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        DropdownMenuItem(text = { Text("Editar") }, onClick = { expandedMenu = false; navController.navigate(AppScreens.createEditProductRoute(producto!!.id)) }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        DropdownMenuItem(text = { Text("Eliminar", color = RedDanger) }, onClick = { viewModel.eliminarProducto(producto!!.id); navController.popBackStack() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary)
            }

            Text(producto!!.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Categoría", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(getLocalizedTypeName(ProductType.valueOf(producto!!.categoria))), fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Stock", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${producto!!.stock} unidades", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!producto!!.notasCultivo.isNullOrBlank()) {
                Text("Notas", fontWeight = FontWeight.Bold)
                Text(producto!!.notasCultivo!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}