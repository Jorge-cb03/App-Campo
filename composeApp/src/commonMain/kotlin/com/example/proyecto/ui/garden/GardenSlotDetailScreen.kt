package com.example.proyecto.ui.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.BancalEntity
import com.example.proyecto.data.database.entity.ProductoEntity
import com.example.proyecto.ui.theme.RedDanger
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.datetime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenSlotDetailScreen(
    navController: NavController,
    bancalId: String,
    viewModel: GardenViewModel = koinViewModel()
) {
    val id = bancalId.toLongOrNull() ?: 0L
    var activeActionType by remember { mutableStateOf<String?>(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showHarvestDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    val bancalState = remember { mutableStateOf<BancalEntity?>(null) }
    val bancal = bancalState.value

    val historialState = viewModel.getHistorial(id).collectAsState(initial = emptyList())
    val historial = historialState.value

    val fichaTecnica by remember(bancal) {
        derivedStateOf { viewModel.getInfoExtendida(bancal?.perenualId) }
    }

    LaunchedEffect(id) {
        bancalState.value = viewModel.getBancalById(id)
    }

    val displayImageUrl = bancal?.imagenUrl
    val msgTaskRegistered = stringResource(Res.string.dialog_success_task_registered)
    val msgHarvest = stringResource(Res.string.dialog_success_harvest)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (bancal?.nombreCultivo != null) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.garden_harvest)) },
                                    leadingIcon = { Icon(Icons.Default.ShoppingBasket, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { showMenu = false; showHarvestDialog = true }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.garden_delete_plant), color = RedDanger) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) },
                                    onClick = { showMenu = false; showDeleteDialog = true }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header con imagen minimalista
            Box(modifier = Modifier.fillMaxWidth().height(320.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (!displayImageUrl.isNullOrBlank()) {
                    AsyncImage(model = displayImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Eco, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    }
                }

                // Overlay de información
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = 400f)))

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Text(
                        text = bancal?.nombreCultivo ?: stringResource(Res.string.garden_slot_empty_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (bancal?.nombreCultivo != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            bancal.frecuenciaRiegoDias?.let { BadgeInfo(Icons.Default.WaterDrop, "$it d") }
                            bancal.necesidadSol?.let { BadgeInfo(Icons.Default.WbSunny, it) }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Guía de Cultivo (Ficha Técnica)
                if (fichaTecnica != null) {
                    SectionTitle(stringResource(Res.string.garden_guide_title))
                    Spacer(Modifier.height(12.dp))
                    OutlinedCard(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            GuideItem(Icons.Rounded.ThumbUp, stringResource(Res.string.garden_friends), fichaTecnica!!.amigos, MaterialTheme.colorScheme.primary)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            GuideItem(Icons.Rounded.ThumbDown, stringResource(Res.string.garden_enemies), fichaTecnica!!.enemigos, MaterialTheme.colorScheme.error)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            GuideItem(Icons.Rounded.Lightbulb, stringResource(Res.string.garden_pro_tip), fichaTecnica!!.consejo, Color(0xFFFBC02D), isItalic = true)
                        }
                    }
                }

                // Acciones Rápidas
                if (bancal?.nombreCultivo != null) {
                    Spacer(Modifier.height(32.dp))
                    SectionTitle(stringResource(Res.string.garden_quick_actions))
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        QuickActionItem(stringResource(Res.string.action_water), Icons.Default.WaterDrop, Modifier.weight(1f)) { activeActionType = "RIEGO" }
                        QuickActionItem(stringResource(Res.string.action_prune), Icons.Default.ContentCut, Modifier.weight(1f)) {
                            viewModel.guardarEntradaDiario(id, "PODA", "Poda realizada", System.currentTimeMillis())
                            successMessage = msgTaskRegistered
                            showSuccessDialog = true
                        }
                        QuickActionItem(stringResource(Res.string.action_treat), Icons.Default.BugReport, Modifier.weight(1f)) { activeActionType = "ANTIPLAGA" }
                        QuickActionItem(stringResource(Res.string.action_fertilize), Icons.Default.Science, Modifier.weight(1f)) { activeActionType = "ABONADO" }
                    }
                }

                Spacer(Modifier.height(32.dp))
                SectionTitle(stringResource(Res.string.garden_history))
                Spacer(Modifier.height(16.dp))

                // Historial Lineal Minimalista
                if (historial.isEmpty()) {
                    Text(stringResource(Res.string.garden_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = FontStyle.Italic)
                } else {
                    val entriesSorted = historial.sortedByDescending { it.fecha }
                    var totalSiembras = historial.count { it.tipoAccion == "SIEMBRA" }

                    entriesSorted.forEachIndexed { index, entrada ->
                        val date = Instant.fromEpochMilliseconds(entrada.fecha).toLocalDateTime(TimeZone.currentSystemDefault())

                        if (entrada.tipoAccion == "SIEMBRA") {
                            CycleBadge(ciclo = totalSiembras)
                            totalSiembras--
                        }

                        TimelineDetailItem(
                            t = entrada.tipoAccion,
                            d = entrada.descripcion,
                            tm = "${date.dayOfMonth}/${date.monthNumber}",
                            i = when(entrada.tipoAccion) {
                                "SIEMBRA" -> Icons.Default.Eco
                                "RIEGO" -> Icons.Default.WaterDrop
                                "PODA" -> Icons.Default.ContentCut
                                "COSECHA" -> Icons.Default.ShoppingBasket
                                else -> Icons.Default.History
                            },
                            c = when(entrada.tipoAccion) {
                                "SIEMBRA" -> MaterialTheme.colorScheme.primary
                                "COSECHA" -> Color(0xFFFBC02D)
                                "RIEGO" -> Color(0xFF2196F3)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            s = index != entriesSorted.size - 1
                        )
                    }
                }
                Spacer(Modifier.height(100.dp))
            }
        }
    }

    // --- DIÁLOGOS REDISEÑADOS ---
    if (activeActionType != null) ActionDialog(activeActionType!!, viewModel, id) {
        activeActionType = null
        successMessage = msgTaskRegistered
        showSuccessDialog = true
    }

    if (showHarvestDialog && bancal != null) {
        var cantidadCosecha by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showHarvestDialog = false },
            icon = { Icon(Icons.Default.ShoppingBasket, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(Res.string.garden_harvest_dialog_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(Res.string.garden_harvest_dialog_msg))
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cantidadCosecha,
                        onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) cantidadCosecha = it },
                        label = { Text(stringResource(Res.string.garden_harvest_amount_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(Res.string.garden_harvest_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = {
                viewModel.cosecharConCantidad(bancal, cantidadCosecha.toDoubleOrNull() ?: 0.0)
                showHarvestDialog = false
                successMessage = msgHarvest
                showSuccessDialog = true
            }, enabled = cantidadCosecha.isNotEmpty()) { Text(stringResource(Res.string.btn_save)) } },
            dismissButton = { TextButton(onClick = { showHarvestDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.garden_delete_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.garden_delete_dialog_msg)) },
            confirmButton = { Button(onClick = {
                viewModel.eliminarPlanta(id)
                showDeleteDialog = false
                navController.popBackStack()
            }, colors = ButtonDefaults.buttonColors(containerColor = RedDanger)) { Text(stringResource(Res.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title), fontWeight = FontWeight.Bold) },
            text = { Text(successMessage) },
            confirmButton = { Button(onClick = {
                showSuccessDialog = false
                if(successMessage == msgHarvest) navController.popBackStack()
            }) { Text(stringResource(Res.string.dialog_btn_ok)) } },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

// --- COMPONENTES AUXILIARES REDISEÑADOS ---

@Composable
fun BadgeInfo(icon: ImageVector, text: String) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun GuideItem(icon: ImageVector, label: String, content: String, color: Color, isItalic: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal
            )
        }
    }
}

@Composable
fun QuickActionItem(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun CycleBadge(ciclo: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
            Text(
                text = "CICLO #$ciclo",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        HorizontalDivider(modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun TimelineDetailItem(t: String, d: String, tm: String, i: ImageVector, c: Color, s: Boolean) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
            Box(Modifier.size(32.dp).background(c.copy(alpha = 0.1f), CircleShape).border(1.dp, c, CircleShape), Alignment.Center) {
                Icon(i, null, tint = c, modifier = Modifier.size(16.dp))
            }
            if (s) Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }
        OutlinedCard(
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(t, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(tm, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(d, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
@Composable
fun ActionDialog(type: String, viewModel: GardenViewModel, bancalId: Long, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductoEntity?>(null) }

    // Obtenemos los productos según el tipo de acción
    val productosState = if(type == "ABONADO")
        viewModel.productosFertilizante.collectAsState(initial = emptyList())
    else
        viewModel.productosQuimicos.collectAsState(initial = emptyList())

    val productos = productosState.value

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if(type == "RIEGO") Icons.Default.WaterDrop else Icons.Default.Science,
                null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if(type == "RIEGO") stringResource(Res.string.garden_water_dialog_title)
                else stringResource(Res.string.garden_treat_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (type != "RIEGO") {
                    Text(
                        stringResource(Res.string.garden_product_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (productos.isEmpty()) {
                        Text(
                            stringResource(Res.string.garden_no_products),
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Selector de productos minimalista
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            LazyColumn(modifier = Modifier.heightIn(max = 150.dp).fillMaxWidth()) {
                                items(productos) { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedProduct = p }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (selectedProduct == p),
                                            onClick = { selectedProduct = p }
                                        )
                                        Text(
                                            p.nombre,
                                            modifier = Modifier.padding(start = 12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (p != productos.last()) HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }

                // Campo de cantidad rediseñado
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) amount = it },
                    label = {
                        Text(if(type == "RIEGO") stringResource(Res.string.garden_liters_label)
                        else stringResource(Res.string.garden_amount_label))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (type == "RIEGO") {
                        viewModel.registrarRiego(bancalId, value)
                    } else {
                        selectedProduct?.let { viewModel.aplicarTratamiento(bancalId, it, value, type) }
                    }
                    onDismiss()
                },
                enabled = amount.isNotEmpty() && (type == "RIEGO" || selectedProduct != null),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(Res.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.btn_cancel))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}