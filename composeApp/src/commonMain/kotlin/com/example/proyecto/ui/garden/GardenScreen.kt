package com.example.proyecto.ui.garden

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(currentJardinera?.nombre ?: stringResource(Res.string.garden_default_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    currentJardinera?.let { jardinera ->
                        IconButton(onClick = { viewModel.toggleFavorito(jardinera) }) {
                            Icon(
                                imageVector = if (jardinera.esFavorita) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (jardinera.esFavorita) GreenPrimary else MaterialTheme.colorScheme.onSurface
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
                                text = { Text(stringResource(Res.string.btn_delete), color = RedDanger) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) },
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
                    Button(onClick = { showAddGardenDialog = true }) { Text(stringResource(Res.string.garden_create_first)) }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                        DimensionControl(stringResource(Res.string.garden_rows), currentJardinera.filas) { delta ->
                            viewModel.actualizarJardinera(currentJardinera, currentJardinera.nombre, (currentJardinera.filas + delta).coerceAtLeast(1), currentJardinera.columnas)
                        }
                        DimensionControl(stringResource(Res.string.garden_cols), currentJardinera.columnas) { delta ->
                            viewModel.actualizarJardinera(currentJardinera, currentJardinera.nombre, currentJardinera.filas, (currentJardinera.columnas + delta).coerceAtLeast(1))
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(currentJardinera.columnas),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
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

                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = { if (currentGardenIndex > 0) currentGardenIndex-- }, enabled = currentGardenIndex > 0) { Icon(Icons.Filled.ChevronLeft, null) }
                    Text("${currentGardenIndex + 1} / ${jardineras.size}", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { if (currentGardenIndex < jardineras.size - 1) currentGardenIndex++ }, enabled = currentGardenIndex < jardineras.size - 1) { Icon(Icons.Filled.ChevronRight, null) }
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
            title = { Text(stringResource(Res.string.garden_plant_title)) },
            text = {
                if (semillas.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Spa, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(Res.string.garden_no_seeds), textAlign = TextAlign.Center)
                        Text(stringResource(Res.string.garden_add_seeds_hint), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(semillas) { semilla ->
                            ListItem(
                                headlineContent = { Text(semilla.nombre) },
                                supportingContent = { Text("Stock: ${semilla.stock}") },
                                leadingContent = {
                                    if(semilla.imagenUrl != null) {
                                        AsyncImage(model = semilla.imagenUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.Spa, null, tint = GreenPrimary)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    selectedSlotIdForPlanting?.let {
                                        viewModel.plantar(it, semilla.perenualId ?: semilla.id.toInt())
                                    }
                                    showPlantSelector = false
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPlantSelector = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }
}

@Composable
fun DimensionControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color.Gray)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onValueChange(-1) }) { Icon(Icons.Default.RemoveCircleOutline, null) }
            Text("$value", fontWeight = FontWeight.Bold)
            IconButton(onClick = { onValueChange(1) }) { Icon(Icons.Default.AddCircleOutline, null) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BancalSlotCard(
    bancal: BancalEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isEmpty = bancal.perenualId == null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!bancal.esFuncional) Color.Transparent
            else if (isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { if (bancal.esFuncional) onClick() },
                onLongClick = onLongClick
            )
            .then(if (!bancal.esFuncional) Modifier.border(1.dp, Color.Gray.copy(0.3f), RoundedCornerShape(12.dp)) else Modifier)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (bancal.esFuncional) {
                if (isEmpty) {
                    Icon(Icons.Default.Add, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.align(Alignment.Center))
                } else {
                    if (bancal.imagenUrl != null) {
                        AsyncImage(model = bancal.imagenUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 100f)))
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Eco, null, tint = GreenPrimary, modifier = Modifier.size(40.dp)) }
                    }
                    Text(
                        text = bancal.nombreCultivo ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bancal.imagenUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            } else {
                Icon(Icons.Default.Block, null, tint = Color.Gray.copy(0.2f), modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}