package com.example.proyecto.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.ProductoEntity
import com.example.proyecto.domain.model.ProductType
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.RedDanger
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

fun getLocalizedTypeName(type: ProductType): StringResource {
    return when (type) {
        ProductType.SEED -> Res.string.type_seed
        ProductType.TOOL -> Res.string.type_tool
        ProductType.FERTILIZER -> Res.string.type_fertilizer
        ProductType.CHEMICAL -> Res.string.type_chemical
        ProductType.VEGETABLE -> Res.string.type_vegetable
        ProductType.PIENSO -> Res.string.type_animal_feed
        ProductType.OTHER -> Res.string.type_other
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(navController: NavController, viewModel: GardenViewModel = koinViewModel()) {
    val productos by viewModel.getProductos().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    // Estados para los diálogos
    var productOptions by remember { mutableStateOf<ProductoEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }
    var stockToAddProduct by remember { mutableStateOf<ProductoEntity?>(null) } // <-- NUEVO ESTADO

    val filteredProducts = productos.filter { it.nombre.contains(searchQuery, ignoreCase = true) && it.stock > 0 }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(AppScreens.AddProduct) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(20.dp))
            Text("Inventario", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filteredProducts) { producto ->
                    ProductItemCard(
                        name = producto.nombre,
                        quantity = producto.stock,
                        type = producto.categoria,
                        imageUrl = producto.imagenUrl,
                        onClick = { navController.navigate(AppScreens.createProductDetailRoute(producto.id.toString())) },
                        onLongClick = { productOptions = producto },
                        onIncrease = { viewModel.updateStock(producto.id, producto.stock + 1) },
                        onDecrease = { viewModel.updateStock(producto.id, (producto.stock - 1).coerceAtLeast(0.0)) }
                    )
                }
            }
        }
    }

    // --- MENÚ DE OPCIONES DEL PRODUCTO (Al mantener pulsado) ---
    if (productOptions != null) {
        AlertDialog(
            onDismissRequest = { productOptions = null },
            title = { Text(productOptions?.nombre ?: "") },
            text = { Text("¿Qué deseas hacer con este producto?") },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    // Botón para Sumar Stock Rápido
                    Button(
                        onClick = {
                            stockToAddProduct = productOptions
                            productOptions = null
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) { Text("Añadir Stock (Compra)") }

                    // Botón Editar
                    Button(
                        onClick = {
                            val id = productOptions?.id; productOptions = null
                            if (id != null) navController.navigate(AppScreens.createEditProductRoute(id))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Editar Producto") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val id = productOptions?.id; productOptions = null
                        if (id != null) showDeleteConfirm = id
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedDanger)
                ) { Text("Eliminar") }
            }
        )
    }

    // --- NUEVO DIÁLOGO: AÑADIR STOCK RÁPIDO ---
    if (stockToAddProduct != null) {
        var amountString by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { stockToAddProduct = null },
            title = { Text("Añadir Stock: ${stockToAddProduct!!.nombre}") },
            text = {
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { amountString = it },
                    label = { Text("Cantidad comprada (ej: 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountToAdd = amountString.toDoubleOrNull() ?: 0.0
                        viewModel.updateStock(
                            stockToAddProduct!!.id,
                            stockToAddProduct!!.stock + amountToAdd
                        )
                        stockToAddProduct = null
                    },
                    enabled = amountString.isNotBlank()
                ) { Text("Sumar al inventario") }
            },
            dismissButton = {
                TextButton(onClick = { stockToAddProduct = null }) { Text("Cancelar") }
            }
        )
    }

    // --- DIÁLOGO DE ELIMINAR ---
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Confirmar") },
            text = { Text("¿Seguro que deseas eliminar este producto?") },
            confirmButton = { Button(onClick = { viewModel.eliminarProducto(showDeleteConfirm!!); showDeleteConfirm = null }, colors = ButtonDefaults.buttonColors(containerColor = RedDanger)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItemCard(name: String, quantity: Double, type: String, onClick: () -> Unit, onLongClick: () -> Unit, onIncrease: () -> Unit, onDecrease: () -> Unit, imageUrl: String? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Inventory, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(name, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
                Text(if (quantity % 1.0 == 0.0) quantity.toInt().toString() else quantity.toString(), fontWeight = FontWeight.Bold)
                IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}