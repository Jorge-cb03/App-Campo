package com.example.proyecto.ui.diary

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.BancalEntity
import com.example.proyecto.data.database.entity.JardineraEntity
import com.example.proyecto.ui.HuertaInput
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.util.MediaManager
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDiaryEntryScreen(
    navController: NavController,
    initialDateMillis: Long,
    taskId: String? = null,
    initialTitle: String? = null,
    initialDesc: String? = null,
    initialGarden: String? = null,
    initialType: String? = null,
    initialWater: Float = 0f,
    initialIsUrgent: Boolean = false,
    viewModel: GardenViewModel = koinViewModel()
) {
    val isEditMode = taskId != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val irrigationTypeStr = stringResource(Res.string.chip_irrigation)
    val successMsg = stringResource(Res.string.dialog_success_diary_saved)

    var title by remember { mutableStateOf(initialTitle ?: "") }
    var description by remember { mutableStateOf(initialDesc ?: "") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialDateMillis > 0) Instant.fromEpochMilliseconds(initialDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
            else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
    }

    val jardineras by viewModel.jardineras.collectAsState()
    var selectedJardinera by remember { mutableStateOf<JardineraEntity?>(null) }
    var expandedGarden by remember { mutableStateOf(false) }
    val selectedBancalIds = remember { mutableStateListOf<Long>() }

    val bancalesDisponibles by if (selectedJardinera != null) {
        viewModel.getBancales(selectedJardinera!!.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<BancalEntity>()) }
    }

    val taskTypes = listOf(
        irrigationTypeStr, stringResource(Res.string.chip_pruning), stringResource(Res.string.chip_harvest),
        stringResource(Res.string.chip_fertilizer), stringResource(Res.string.chip_other)
    )
    var selectedType by remember { mutableStateOf(initialType ?: taskTypes[0]) }
    var waterAmount by remember { mutableStateOf(initialWater) }

    var diaryPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val launcher = MediaManager.rememberLauncher { bytes -> if (bytes != null) diaryPhotoBytes = bytes }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launcher.launchCamera() }

    LaunchedEffect(taskId, jardineras) {
        if (taskId != null && jardineras.isNotEmpty()) {
            val idLong = taskId.toLongOrNull() ?: 0L
            val entrada = viewModel.getEntradaDiarioById(idLong)
            entrada?.let { ent ->
                title = ent.tipoAccion
                description = ent.descripcion
                selectedType = if(taskTypes.contains(ent.tipoAccion)) ent.tipoAccion else taskTypes.last()
                diaryPhotoBytes = ent.foto
                val bancal = viewModel.getBancalById(ent.bancalId)
                bancal?.let { b ->
                    selectedJardinera = jardineras.find { it.id == b.jardineraId }
                    selectedBancalIds.clear()
                    selectedBancalIds.add(b.id)
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { keyboardController?.hide() }) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(if (isEditMode) Res.string.diary_edit_entry_title else Res.string.diary_new_entry_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                if (!isEditMode) {
                    TabRow(
                        selectedTabIndex = 0,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions -> SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[0]), color = MaterialTheme.colorScheme.primary) }
                    ) {
                        Tab(
                            selected = true, onClick = {},
                            text = { Text(stringResource(Res.string.tab_gardening), fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Filled.Grass, null) }
                        )
                        Tab(
                            selected = false, onClick = { navController.popBackStack(); navController.navigate(AppScreens.createAddAnimalDiaryRoute(initialDateMillis)) },
                            text = { Text(stringResource(Res.string.tab_animals), fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Filled.Pets, null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.diary_section_location), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Box {
                        SelectorRow(label = stringResource(Res.string.diary_select_garden), value = selectedJardinera?.nombre ?: stringResource(Res.string.diary_tap_select), icon = Icons.Filled.Grass) { expandedGarden = true; keyboardController?.hide() }
                        DropdownMenu(expanded = expandedGarden, onDismissRequest = { expandedGarden = false }) {
                            jardineras.forEach { jardinera ->
                                DropdownMenuItem(text = { Text(jardinera.nombre) }, onClick = { selectedJardinera = jardinera; selectedBancalIds.clear(); expandedGarden = false })
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SelectorRow(label = stringResource(Res.string.diary_date_activity), value = "${selectedDate.dayOfMonth}/${selectedDate.monthNumber}/${selectedDate.year}", icon = Icons.Filled.CalendarToday) { showDatePicker = true; keyboardController?.hide() }
                }
            }

            if (selectedJardinera != null) {
                OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(stringResource(Res.string.diary_select_slots), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            TextButton(onClick = { if (selectedBancalIds.size == bancalesDisponibles.size) selectedBancalIds.clear() else { selectedBancalIds.clear(); selectedBancalIds.addAll(bancalesDisponibles.map { it.id }) } }) { Text(if (selectedBancalIds.size == bancalesDisponibles.size) stringResource(Res.string.diary_deselect) else stringResource(Res.string.diary_all), color = MaterialTheme.colorScheme.primary) }
                        }
                        LazyVerticalGrid(columns = GridCells.Fixed(selectedJardinera!!.columnas), modifier = Modifier.heightIn(max = 250.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bancalesDisponibles) { bancal ->
                                val isSelected = selectedBancalIds.contains(bancal.id)
                                Box(
                                    modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).clickable { if (isSelected) selectedBancalIds.remove(bancal.id) else selectedBancalIds.add(bancal.id); keyboardController?.hide() },
                                    contentAlignment = Alignment.Center
                                ) { Text(text = bancal.nombreCultivo?.take(2) ?: "${bancal.fila + 1}-${bancal.columna + 1}", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(Res.string.diary_section_details), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    HuertaInput(value = title, onValueChange = { title = it }, label = stringResource(Res.string.diary_task_title_hint), icon = Icons.Filled.Title, imeAction = ImeAction.Next)
                    OutlinedTextField(
                        value = description, onValueChange = { description = it }, label = { Text(stringResource(Res.string.diary_notes_hint)) },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Description, null) }, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(Res.string.diary_type_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            taskTypes.forEach { type ->
                                FilterChip(
                                    selected = (type == selectedType), onClick = { selectedType = type; keyboardController?.hide() }, label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = type == selectedType, borderColor = MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }

                    if (selectedType == irrigationTypeStr) {
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text(stringResource(Res.string.diary_water_amount), color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("${waterAmount.toInt()} L", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Slider(value = waterAmount, onValueChange = { waterAmount = it }, valueRange = 0f..20f, steps = 19, colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary))
                        }
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).clickable { showPhotoOptions = true; keyboardController?.hide() }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = BorderStroke(1.dp, if (diaryPhotoBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (diaryPhotoBytes != null) Icons.Outlined.Image else Icons.Outlined.CameraAlt, null, tint = if (diaryPhotoBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if (diaryPhotoBytes != null) stringResource(Res.string.diary_photo_change) else stringResource(Res.string.diary_add_photo), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && selectedBancalIds.isNotEmpty()) {
                        val finalDesc = if (selectedType == irrigationTypeStr) "$title - ${waterAmount.toInt()}L" else description
                        val dateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                        val idToUpdate = taskId?.toLongOrNull() ?: 0L
                        if (isEditMode) viewModel.guardarEntradaDiario(id = idToUpdate, bancalId = selectedBancalIds.first(), tipo = selectedType, desc = finalDesc, fecha = dateMillis, foto = diaryPhotoBytes)
                        else selectedBancalIds.forEach { id -> viewModel.guardarEntradaDiario(bancalId = id, tipo = selectedType, desc = finalDesc, fecha = dateMillis, id = 0L, foto = diaryPhotoBytes) }
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp), enabled = selectedBancalIds.isNotEmpty() && title.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(16.dp)
            ) { Text(if (isEditMode) stringResource(Res.string.btn_update) else stringResource(Res.string.diary_btn_register, selectedBancalIds.size), fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false }, icon = { Icon(Icons.Outlined.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text(stringResource(Res.string.diary_dialog_photo_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { val permission = Manifest.permission.CAMERA; if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) launcher.launchCamera() else cameraPermissionLauncher.launch(permission); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.diary_btn_take_photo)) }
                    OutlinedButton(onClick = { launcher.launchGallery(); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.diary_btn_gallery), color = MaterialTheme.colorScheme.onSurface) }
                }
            },
            confirmButton = { TextButton(onClick = { showPhotoOptions = false }) { Text(stringResource(Res.string.btn_cancel)) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { }, title = { Text(stringResource(Res.string.dialog_success_title), fontWeight = FontWeight.Bold) }, text = { Text(successMsg) },
            confirmButton = { Button(onClick = { showSuccessDialog = false; navController.popBackStack() }, shape = RoundedCornerShape(12.dp)) { Text(stringResource(Res.string.dialog_btn_ok)) } },
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { millis -> selectedDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date }; showDatePicker = false }) { Text(stringResource(Res.string.dialog_btn_ok)) } },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp)
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
fun SelectorRow(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}