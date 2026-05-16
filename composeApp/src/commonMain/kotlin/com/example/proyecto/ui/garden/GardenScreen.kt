package com.example.proyecto.ui.garden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.BancalEntity
import com.example.proyecto.ui.theme.GreenPrimary
import com.example.proyecto.ui.theme.RedDanger
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    navController: NavController,
    initialGardenId: Long = 0L,
    viewModel: GardenViewModel = koinViewModel()
) {
    val jardineras by viewModel.jardineras.collectAsState()
    var currentGardenIndex by remember { mutableStateOf(0) }

    LaunchedEffect(jardineras, initialGardenId) {
        if (initialGardenId != 0L) {
            val index = jardineras.indexOfFirst { it.id == initialGardenId }
            if (index != -1) currentGardenIndex = index
        }
    }

    val currentJardinera = jardineras.getOrNull(currentGardenIndex)
    val bancales by produceState<List<BancalEntity>>(emptyList(), currentJardinera?.id) {
        currentJardinera?.let { viewModel.getBancales(it.id).collect { value = it } }
    }

    var showAddGardenDialog by remember { mutableStateOf(false) }
    var showGardenMenu by remember { mutableStateOf(false) }
    var showDeleteGardenDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedSlotIdForPlanting by remember { mutableStateOf<Long?>(null) }
    var showPlantSelector by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var bancalOptions by remember { mutableStateOf<BancalEntity?>(null) }

    val msgCreated = stringResource(Res.string.dialog_success_garden_created)
    val msgDeleted = stringResource(Res.string.dialog_success_garden_deleted)
    val msgErrorDuplicate = "Ya existe una jardinera con este nombre"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            // TopAppBar Minimalista: Fondo transparente, texto limpio
            TopAppBar(
                title = {
                    Text(
                        text = currentJardinera?.nombre ?: stringResource(Res.string.garden_default_title),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    currentJardinera?.let { jardinera ->
                        IconButton(onClick = { viewModel.toggleFavorito(jardinera) }) {
                            Icon(
                                imageVector = if (jardinera.esFavorita) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (jardinera.esFavorita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showGardenMenu = true }) { Icon(Icons.Filled.MoreVert, null) }
                        DropdownMenu(expanded = showGardenMenu, onDismissRequest = { showGardenMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.garden_new_title)) },
                                leadingIcon = { Icon(Icons.Default.Add, null) },
                                onClick = { showGardenMenu = false; showAddGardenDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.btn_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { showGardenMenu = false; showDeleteGardenDialog = true }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (currentJardinera == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { showAddGardenDialog = true },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text(stringResource(Res.string.garden_create_first)) }
                }
            } else {
                // Tarjeta de Controles Minimalista: Sin sombra, solo borde
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(Modifier.padding(8.dp).fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        DimensionControl(stringResource(Res.string.garden_rows), currentJardinera.filas) { delta ->
                            viewModel.actualizarJardinera(currentJardinera, currentJardinera.nombre, (currentJardinera.filas + delta).coerceAtLeast(1), currentJardinera.columnas)
                        }
                        Divider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        DimensionControl(stringResource(Res.string.garden_cols), currentJardinera.columnas) { delta ->
                            viewModel.actualizarJardinera(currentJardinera, currentJardinera.nombre, currentJardinera.filas, (currentJardinera.columnas + delta).coerceAtLeast(1))
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(currentJardinera.columnas),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).padding(top = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(bancales.sortedWith(compareBy({ it.fila }, { it.columna }))) { bancal ->
                        BancalSlotCard(
                            bancal = bancal,
                            onLongClick = { bancalOptions = bancal },
                            onClick = {
                                if (bancal.perenualId == null) {
                                    selectedSlotIdForPlanting = bancal.id
                                    showPlantSelector = true
                                } else {
                                    navController.navigate("garden_slot_detail/${bancal.id}")
                                }
                            }
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentGardenIndex > 0) currentGardenIndex-- },
                        enabled = currentGardenIndex > 0
                    ) { Icon(Icons.Filled.ChevronLeft, null) }

                    Text(
                        "${currentGardenIndex + 1} / ${jardineras.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { if (currentGardenIndex < jardineras.size - 1) currentGardenIndex++ },
                        enabled = currentGardenIndex < jardineras.size - 1
                    ) { Icon(Icons.Filled.ChevronRight, null) }
                }
            }
        }
    }

    if (bancalOptions != null) {
        val bancal = bancalOptions!!
        val isPlanted = bancal.perenualId != null

        AlertDialog(
            onDismissRequest = { bancalOptions = null },
            icon = { Icon(if (isPlanted) Icons.Default.Warning else Icons.Default.Settings, null) },
            title = { Text(stringResource(Res.string.garden_slot_options)) },
            text = { Text(if (isPlanted) "¿Qué deseas hacer con la planta de este espacio?" else "¿Qué deseas hacer con este espacio?") },
            confirmButton = {
                if (isPlanted) {
                    Button(
                        onClick = {
                            viewModel.eliminarPlanta(bancal.id)
                            bancalOptions = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                    ) {
                        Icon(Icons.Default.DeleteForever, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar planta")
                    }
                } else {
                    Button(onClick = {
                        viewModel.toggleBancal(bancal.id, !bancal.esFuncional)
                        bancalOptions = null
                    }) {
                        Icon(if(bancal.esFuncional) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if(bancal.esFuncional) stringResource(Res.string.garden_hide) else stringResource(Res.string.garden_show))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { bancalOptions = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }

    if (showAddGardenDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddGardenDialog = false
                errorMessage = null
                tempName = ""
            },
            title = { Text(stringResource(Res.string.garden_new_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = {
                            tempName = it
                            if (errorMessage != null) errorMessage = null
                        },
                        label = { Text(stringResource(Res.string.garden_name_label)) },
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nameToSave = tempName.trim()
                    if(nameToSave.isNotBlank()) {
                        viewModel.crearNuevaJardinera(nameToSave, 4, 2) { success ->
                            if (success) {
                                showAddGardenDialog = false
                                successMessage = msgCreated
                                showSuccessDialog = true
                                tempName = ""
                                errorMessage = null
                            } else {
                                errorMessage = msgErrorDuplicate
                            }
                        }
                    }
                }) { Text(stringResource(Res.string.btn_create)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddGardenDialog = false
                    errorMessage = null
                    tempName = ""
                }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }

    if (showDeleteGardenDialog && currentJardinera != null) {
        AlertDialog(
            onDismissRequest = { showDeleteGardenDialog = false },
            title = { Text(stringResource(Res.string.dialog_garden_delete_title)) },
            text = { Text("Se borrará la jardinera y todo su historial de forma permanente.") },
            confirmButton = {
                Button(
                    onClick = {
                        // CORRECCIÓN: Llamamos al método que borra TODO el historial
                        viewModel.eliminarJardineraDefinitivamente(currentJardinera.id)
                        showDeleteGardenDialog = false
                        successMessage = msgDeleted
                        showSuccessDialog = true
                        if (currentGardenIndex > 0) currentGardenIndex--
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteGardenDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title)) },
            text = { Text(successMessage) },
            confirmButton = { Button(onClick = { showSuccessDialog = false }) { Text(stringResource(Res.string.dialog_btn_ok)) } }
        )
    }

    if (showPlantSelector) {
        val semillas by viewModel.semillasDisponibles.collectAsState(initial = emptyList())

        AlertDialog(
            onDismissRequest = { showPlantSelector = false },
            title = { Text("¿Qué vas a plantar?") },
            text = {
                LazyColumn {
                    items(semillas) { semilla ->
                        ListItem(
                            headlineContent = { Text(semilla.nombre) },
                            supportingContent = { Text("Disponible: ${semilla.stock}") },
                            modifier = Modifier.clickable {
                                selectedSlotIdForPlanting?.let { bId ->
                                    // IMPORTANTE: Pasamos el ID del producto
                                    viewModel.plantar(bId, semilla.id)
                                }
                                showPlantSelector = false
                            }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlantSelector = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
fun DimensionControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(-1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Remove, null, Modifier.size(16.dp)) }
            Text("$value", fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
            IconButton(onClick = { onValueChange(1) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BancalSlotCard(
    bancal: com.example.proyecto.data.database.entity.BancalEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isEmpty = bancal.perenualId == null

    // LA MISMA MAGIA: Detección infalible basada en el color de fondo actual
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A) ||
            MaterialTheme.colorScheme.background == Color(0xFF121212)

    // Marrón Tierra Oscuro para que destaque en el modo noche
    val slotColor = if (isDark) Color(0xFF4E342E) else Color(0xFFEFEBE9)
    val slotBorderColor = if (isDark) Color(0xFF6D4C41) else Color(0xFFD7CCC8).copy(alpha = 0.5f)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!bancal.esFuncional) Color.Transparent
            else if (isEmpty) slotColor
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, slotBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { if (bancal.esFuncional) onClick() },
                onLongClick = onLongClick
            )
    ) {
        Box(Modifier.fillMaxSize()) {
            if (bancal.esFuncional) {
                if (isEmpty) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        tint = if (isDark) Color(0xFFD7CCC8) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    if (bancal.imagenUrl != null) {
                        coil3.compose.AsyncImage(model = bancal.imagenUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Spa, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Text(
                        text = bancal.nombreCultivo ?: "",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bancal.imagenUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
            } else {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.align(Alignment.Center).size(18.dp))
            }
        }
    }
}