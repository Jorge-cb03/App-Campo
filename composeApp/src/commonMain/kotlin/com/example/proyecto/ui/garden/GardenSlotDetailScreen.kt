package com.example.proyecto.ui.garden

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
import com.example.proyecto.data.database.entity.EntradaDiarioEntity
import com.example.proyecto.ui.HuertaCard
import com.example.proyecto.ui.theme.GreenPrimary
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
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    if (bancal?.nombreCultivo != null) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.White)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.garden_harvest)) }, leadingIcon = { Icon(Icons.Default.ShoppingBasket, null, tint = GreenPrimary) }, onClick = { showMenu = false; showHarvestDialog = true })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text(stringResource(Res.string.garden_delete_plant), color = RedDanger) }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) }, onClick = { showMenu = false; showDeleteDialog = true })
                            }
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header con imagen
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (!displayImageUrl.isNullOrBlank()) {
                    AsyncImage(model = displayImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Eco, null, modifier = Modifier.size(100.dp), tint = GreenPrimary.copy(alpha=0.3f)) }
                }
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 300f)))
                Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = bancal?.nombreCultivo ?: stringResource(Res.string.garden_slot_empty_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    if (bancal?.nombreCultivo != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            bancal.frecuenciaRiegoDias?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WaterDrop, null, tint = Color.White, modifier = Modifier.size(16.dp)); Text(" $it d", color = Color.White, fontWeight = FontWeight.Bold) } }
                            bancal.necesidadSol?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.WbSunny, null, tint = Color.White, modifier = Modifier.size(16.dp)); Text(" $it", color = Color.White, fontWeight = FontWeight.Bold) } }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                // Ficha Técnica
                if (fichaTecnica != null) {
                    Text(stringResource(Res.string.garden_guide_title), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    HuertaCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.ThumbUp, null, tint = GreenPrimary, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Column { Text(stringResource(Res.string.garden_friends), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(fichaTecnica!!.amigos, fontSize = 13.sp, color = Color.Gray) } }
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.ThumbDown, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Column { Text(stringResource(Res.string.garden_enemies), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(fichaTecnica!!.enemigos, fontSize = 13.sp, color = Color.Gray) } }
                            HorizontalDivider(color = Color.LightGray.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.Top) { Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Column { Text(stringResource(Res.string.garden_pro_tip), fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(fichaTecnica!!.consejo, fontSize = 13.sp, fontStyle = FontStyle.Italic) } }
                        }
                    }
                }

                // Acciones Rápidas
                if (bancal?.nombreCultivo != null) {
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(Res.string.garden_quick_actions), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
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

                Spacer(Modifier.height(30.dp))
                Text(stringResource(Res.string.garden_history), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))

                // --- LÓGICA DE DIARIO LINEAL CON CICLOS ---
                if (historial.isEmpty()) {
                    Text(stringResource(Res.string.garden_no_history), color = Color.Gray, fontStyle = FontStyle.Italic)
                } else {
                    // Calculamos los ciclos: Cada "SIEMBRA" inicia uno nuevo
                    val entriesSorted = historial.sortedByDescending { it.fecha }
                    var totalSiembras = historial.count { it.tipoAccion == "SIEMBRA" }

                    entriesSorted.forEachIndexed { index, entrada ->
                        val date = Instant.fromEpochMilliseconds(entrada.fecha).toLocalDateTime(TimeZone.currentSystemDefault())

                        // Si la entrada es una SIEMBRA, mostramos el indicador de ciclo
                        if (entrada.tipoAccion == "SIEMBRA") {
                            CycleIndicator(ciclo = totalSiembras)
                            totalSiembras--
                        }

                        TimelineItem(
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
                                "SIEMBRA" -> GreenPrimary
                                "COSECHA" -> Color(0xFFFBC02D)
                                "RIEGO" -> Color(0xFF2196F3)
                                else -> GreenPrimary
                            },
                            s = index != entriesSorted.size - 1
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // Dialogs... (ActionDialog, HarvestDialog, DeleteDialog, SuccessDialog se mantienen igual)
    if (activeActionType != null) ActionDialog(activeActionType!!, viewModel, id) {
        activeActionType = null
        successMessage = msgTaskRegistered
        showSuccessDialog = true
    }

    if (showHarvestDialog && bancal != null) {
        var cantidadCosecha by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showHarvestDialog = false },
            icon = { Icon(Icons.Default.ShoppingBasket, null, tint = GreenPrimary) },
            title = { Text(stringResource(Res.string.garden_harvest_dialog_title)) },
            text = { Column { Text(stringResource(Res.string.garden_harvest_dialog_msg)); Spacer(Modifier.height(8.dp)); OutlinedTextField(value = cantidadCosecha, onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) cantidadCosecha = it }, label = { Text(stringResource(Res.string.garden_harvest_amount_hint)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(4.dp)); Text(stringResource(Res.string.garden_harvest_note), style = MaterialTheme.typography.bodySmall, color = Color.Gray) } },
            confirmButton = { Button(onClick = {
                viewModel.cosecharConCantidad(bancal, cantidadCosecha.toDoubleOrNull() ?: 0.0)
                showHarvestDialog = false
                successMessage = msgHarvest
                showSuccessDialog = true
            }, enabled = cantidadCosecha.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) { Text(stringResource(Res.string.btn_save)) } },
            dismissButton = { TextButton(onClick = { showHarvestDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.garden_delete_dialog_title)) },
            text = { Text(stringResource(Res.string.garden_delete_dialog_msg)) },
            confirmButton = { Button(onClick = {
                viewModel.eliminarPlanta(id)
                showDeleteDialog = false
                navController.popBackStack()
            }, colors = ButtonDefaults.buttonColors(containerColor = RedDanger)) { Text(stringResource(Res.string.btn_delete)) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title)) },
            text = { Text(successMessage) },
            confirmButton = { Button(onClick = {
                showSuccessDialog = false
                if(successMessage == msgHarvest) navController.popBackStack()
            }) { Text(stringResource(Res.string.dialog_btn_ok)) } }
        )
    }
}

// NUEVO COMPONENTE: Indicador visual de ciclo de cultivo
@Composable
fun CycleIndicator(ciclo: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CICLO #$ciclo",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun QuickActionItem(l: String, i: ImageVector, m: Modifier, c: () -> Unit) {
    Card(modifier = m.clickable { c() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(i, null, tint = GreenPrimary)
            Text(l, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun TimelineItem(t: String, d: String, tm: String, i: ImageVector, c: Color, s: Boolean) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
            Box(Modifier.size(32.dp).border(2.dp, c, CircleShape), Alignment.Center) { Icon(i, null, tint = c, modifier = Modifier.size(16.dp)) }
            if (s) Box(Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
        }
        HuertaCard(Modifier.padding(bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(t, fontWeight = FontWeight.Bold); Text(tm, fontSize = 12.sp, color = Color.Gray) }
            Text(d, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ActionDialog(type: String, viewModel: GardenViewModel, bancalId: Long, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductoEntity?>(null) }
    val productosState = if(type == "ABONADO") viewModel.productosFertilizante.collectAsState(initial = emptyList()) else viewModel.productosQuimicos.collectAsState(initial = emptyList())
    val productos = productosState.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(type == "RIEGO") stringResource(Res.string.garden_water_dialog_title) else stringResource(Res.string.garden_treat_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type != "RIEGO") {
                    Text(stringResource(Res.string.garden_product_label), style = MaterialTheme.typography.bodySmall)
                    if (productos.isEmpty()) {
                        Text(stringResource(Res.string.garden_no_products), fontStyle = FontStyle.Italic, color = Color.Gray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 120.dp).fillMaxWidth()) {
                            items(productos) { p ->
                                Row(modifier = Modifier.fillMaxWidth().clickable { selectedProduct = p }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = (selectedProduct == p), onClick = { selectedProduct = p })
                                    Text(p.nombre, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { if(it.all { c -> c.isDigit() || c == '.' }) amount = it }, label = { Text(if(type == "RIEGO") stringResource(Res.string.garden_liters_label) else stringResource(Res.string.garden_amount_label)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = { Button(onClick = { val value = amount.toDoubleOrNull() ?: 0.0; if (type == "RIEGO") viewModel.registrarRiego(bancalId, value) else selectedProduct?.let { viewModel.aplicarTratamiento(bancalId, it, value, type) }; onDismiss() }, enabled = amount.isNotEmpty() && (type == "RIEGO" || selectedProduct != null)) { Text(stringResource(Res.string.btn_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel)) } }
    )
}