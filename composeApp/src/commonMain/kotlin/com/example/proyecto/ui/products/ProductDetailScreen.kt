package com.example.proyecto.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.ProductoEntity
import com.example.proyecto.data.repository.JardineraRepository
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.GreenPrimary
import com.example.proyecto.ui.theme.RedDanger
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    viewModel: GardenViewModel = koinViewModel()
) {
    val idLong = productId.toLongOrNull() ?: return
    var producto by remember { mutableStateOf<ProductoEntity?>(null) }
    var fichaTecnica by remember { mutableStateOf<JardineraRepository.FichaCultivo?>(null) }

    // Estado para recargar datos al volver de editar
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(idLong, refreshTrigger) {
        val p = viewModel.getProductoById(idLong)
        producto = p
        if (p?.perenualId != null) {
            fichaTecnica = viewModel.getInfoExtendida(p.perenualId)
        }
    }

    // Si el producto fue eliminado o no existe, volvemos atrás
    if (producto == null) {
        // Podrías mostrar un loading aquí si la carga es lenta
        return
    }

    val product = producto!! // Smart cast seguro
    var expandedMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.product_tech_sheet)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { expandedMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                    DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                        // ACCIÓN EDITAR: Navega a AddProductScreen con el ID
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_edit)) },
                            onClick = {
                                expandedMenu = false
                                navController.navigate(AppScreens.createEditProductRoute(product.id))
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.menu_delete), color = RedDanger) },
                            onClick = {
                                expandedMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // IMAGEN DE CABECERA
            Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                if (product.imagenUrl != null) {
                    AsyncImage(
                        model = product.imagenUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(GreenPrimary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Spa, null, modifier = Modifier.size(80.dp), tint = GreenPrimary)
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Text(product.nombre, style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
                    if (product.nombreCientifico != null) {
                        Text(product.nombreCientifico!!, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f), fontStyle = FontStyle.Italic)
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // TARJETAS DE INFORMACIÓN (FICHA TÉCNICA)
                if (fichaTecnica != null) {
                    Text(stringResource(Res.string.section_basic_info), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoChip(
                            icon = Icons.Outlined.WbSunny,
                            label = stringResource(Res.string.weather_uv),
                            value = fichaTecnica!!.sol,
                            modifier = Modifier.weight(1f)
                        )
                        InfoChip(
                            icon = Icons.Outlined.WaterDrop,
                            label = stringResource(Res.string.chip_irrigation),
                            value = "${fichaTecnica!!.riegoDias} ${stringResource(Res.string.garden_days_planted)}", // Reutilizando string días
                            modifier = Modifier.weight(1f)
                        )
                        InfoChip(
                            icon = Icons.Outlined.Timer,
                            label = "Germinación", // Podrías añadir string resource
                            value = fichaTecnica!!.germinacion,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    DetailSection(stringResource(Res.string.garden_friends), fichaTecnica!!.amigos)
                    DetailSection(stringResource(Res.string.garden_enemies), fichaTecnica!!.enemigos)
                    DetailSection(stringResource(Res.string.garden_pro_tip), fichaTecnica!!.consejo, isTip = true)
                }

                // INFORMACIÓN DEL INVENTARIO
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.products_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreenPrimary)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(Res.string.product_category))
                            Text(stringResource(getLocalizedTypeName(com.example.proyecto.domain.model.ProductType.valueOf(product.categoria))), fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(Res.string.product_stock_label))
                            Text("${product.stock} ${stringResource(Res.string.product_units)}", fontWeight = FontWeight.Bold, color = if(product.stock < 5) RedDanger else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                if (!product.notasCultivo.isNullOrBlank()) {
                    Text(stringResource(Res.string.diary_detail_notes_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    Text(product.notasCultivo!!, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.product_delete_confirm)) },
            text = { Text(stringResource(Res.string.product_delete_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarProducto(product.id)
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun InfoChip(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun DetailSection(title: String, content: String, isTip: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isTip) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface),
        border = if (isTip) BorderStroke(1.dp, Color(0xFFFFB74D)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isTip) Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                if (isTip) Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isTip) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(content, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DetailInfoCard(title: String, value: String, suffix: String, icon: ImageVector, statusColor: Color, statusText: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = GreenPrimary.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = GreenPrimary) } }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray); Row(verticalAlignment = Alignment.Bottom) { Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Spacer(Modifier.width(4.dp)); Text(suffix, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp)) } }
            Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) { Text(statusText, Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold) }
        }
    }
}