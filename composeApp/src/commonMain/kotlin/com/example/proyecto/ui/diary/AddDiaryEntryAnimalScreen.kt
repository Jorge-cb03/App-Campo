package com.example.proyecto.ui.diary

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.ui.HuertaInput
import com.example.proyecto.ui.animals.AnimalsViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.util.MediaManager
import huertomanager.composeapp.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDiaryEntryAnimalScreen(
    navController: NavController,
    initialDateMillis: Long,
    taskId: String? = null,
    viewModel: AnimalsViewModel = koinViewModel()
) {
    val isEditMode = taskId != null
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialDateMillis > 0) Instant.fromEpochMilliseconds(initialDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
            else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
    }

    val cercados by viewModel.cercados.collectAsState(initial = emptyList<CercadoEntity>())
    val diarioAnimales by viewModel.diarioAnimales.collectAsState(initial = emptyList())

    var selectedCercado by remember { mutableStateOf<CercadoEntity?>(null) }
    var expandedCercado by remember { mutableStateOf(false) }

    val taskTypes = listOf(
        stringResource(Res.string.animal_diary_type_feeding),
        stringResource(Res.string.animal_diary_type_collect),
        stringResource(Res.string.animal_diary_type_vet),
        stringResource(Res.string.animal_diary_type_clean),
        stringResource(Res.string.animal_diary_type_vaccine),
        stringResource(Res.string.animal_diary_type_other)
    )
    var selectedType by remember { mutableStateOf(taskTypes[0]) }

    var diaryPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val launcher = MediaManager.rememberLauncher { if (it != null) diaryPhotoBytes = it }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) launcher.launchCamera() }

    LaunchedEffect(taskId, diarioAnimales, cercados) {
        if (taskId != null && diarioAnimales.isNotEmpty() && cercados.isNotEmpty()) {
            val idLong = taskId.toLongOrNull() ?: 0L
            val entrada = diarioAnimales.find { it.id == idLong }
            entrada?.let { ent ->
                val partes = ent.descripcion.split(" | ", limit = 2)
                title = partes[0]
                if (partes.size > 1) description = partes[1]
                selectedType = if (taskTypes.contains(ent.tipoAccion)) ent.tipoAccion else taskTypes.last()
                selectedCercado = cercados.find { it.id == ent.cercadoId }
                selectedDate = Instant.fromEpochMilliseconds(ent.fecha).toLocalDateTime(TimeZone.currentSystemDefault()).date
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
                    title = { Text(stringResource(if (isEditMode) Res.string.diary_edit_entry_title else Res.string.animal_diary_new_entry_title), fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
                if (!isEditMode) {
                    TabRow(
                        selectedTabIndex = 1,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions -> SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[1]), color = MaterialTheme.colorScheme.primary) }
                    ) {
                        Tab(selected = false, onClick = { navController.popBackStack(); navController.navigate(AppScreens.createAddDiaryRoute(initialDateMillis)) }, text = { Text(stringResource(Res.string.tab_gardening), fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Filled.Grass, null) })
                        Tab(selected = true, onClick = {}, text = { Text(stringResource(Res.string.tab_animals), fontWeight = FontWeight.Bold) }, icon = { Icon(Icons.Filled.Pets, null) })
                    }
                }
            }
        }
    ) { padding ->
        // ── CLAVE: imePadding() ANTES de verticalScroll ──────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()                  // ← sube el contenido al aparecer el teclado
                .verticalScroll(scrollState)   // ← scroll dentro del espacio restante
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SELECTOR CERCADO
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.diary_section_location), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    ExposedDropdownMenuBox(expanded = expandedCercado, onExpandedChange = { expandedCercado = it; keyboardController?.hide() }) {
                        OutlinedTextField(
                            value = selectedCercado?.nombre ?: stringResource(Res.string.cercado_select_placeholder),
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCercado) }
                        )
                        ExposedDropdownMenu(expanded = expandedCercado, onDismissRequest = { expandedCercado = false }) {
                            cercados.forEach { cercado ->
                                DropdownMenuItem(text = { Text(cercado.nombre) }, onClick = { selectedCercado = cercado; expandedCercado = false })
                            }
                        }
                    }
                }
            }

            // SECCIÓN DETALLES
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(Res.string.diary_section_details), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    HuertaInput(value = title, onValueChange = { title = it }, label = stringResource(Res.string.diary_task_title_hint), icon = Icons.Filled.Title, imeAction = ImeAction.Next)
                    OutlinedTextField(
                        value = description, onValueChange = { description = it }, label = { Text(stringResource(Res.string.diary_notes_hint)) },
                        modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Description, null) }, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
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
                }
            }

            // FOTO
            Surface(modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(16.dp)).clickable { showPhotoOptions = true; keyboardController?.hide() }, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), border = BorderStroke(1.dp, if (diaryPhotoBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (diaryPhotoBytes != null) Icons.Outlined.Image else Icons.Outlined.CameraAlt, null, tint = if (diaryPhotoBytes != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if (diaryPhotoBytes != null) stringResource(Res.string.diary_photo_change) else stringResource(Res.string.diary_add_photo), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // BOTÓN GUARDAR
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedCercado != null) {
                        val finalDesc = if (description.isNotBlank()) "$title | $description" else title
                        val dateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                        val idToUpdate = taskId?.toLongOrNull() ?: 0L
                        viewModel.guardarEntradaDiarioAnimal(id = idToUpdate, cercadoId = selectedCercado!!.id, tipo = selectedType, desc = finalDesc, fecha = dateMillis, foto = diaryPhotoBytes, cantidad = 0f)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && selectedCercado != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(if (isEditMode) Res.string.btn_update else Res.string.animal_diary_register_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp) }

            Spacer(Modifier.height(20.dp))
        }
    }

    // DIÁLOGOS
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false }, icon = { Icon(Icons.Outlined.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) }, title = { Text(stringResource(Res.string.diary_dialog_photo_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { val p = Manifest.permission.CAMERA; if (ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED) launcher.launchCamera() else cameraPermissionLauncher.launch(p); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Outlined.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.diary_btn_take_photo)) }
                    OutlinedButton(onClick = { launcher.launchGallery(); showPhotoOptions = false }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.diary_btn_gallery), color = MaterialTheme.colorScheme.onSurface) }
                }
            },
            confirmButton = { TextButton(onClick = { showPhotoOptions = false }) { Text(stringResource(Res.string.btn_cancel)) } }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { }, title = { Text(stringResource(Res.string.dialog_success_title), fontWeight = FontWeight.Bold) }, text = { Text(stringResource(Res.string.animal_diary_success_msg)) },
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