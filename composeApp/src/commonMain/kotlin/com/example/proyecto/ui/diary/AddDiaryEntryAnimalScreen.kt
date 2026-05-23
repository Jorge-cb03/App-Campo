package com.example.proyecto.ui.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.ui.HuertaInput
import com.example.proyecto.ui.animals.AnimalsViewModel
import com.example.proyecto.ui.navigation.AppScreens
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

    // --- ESTADOS BÁSICOS ---
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(
            if (initialDateMillis > 0) Instant.fromEpochMilliseconds(initialDateMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
            else Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        )
    }

    // --- DATOS DEL VIEWMODEL ---
    val cercados by viewModel.cercados.collectAsState(initial = emptyList<CercadoEntity>())
    val diarioAnimales by viewModel.diarioAnimales.collectAsState(initial = emptyList())

    var selectedCercado by remember { mutableStateOf<CercadoEntity?>(null) }
    var expandedCercado by remember { mutableStateOf(false) }

    // Definición de tipos de tareas usando los recursos de cadena
    val taskTypes = listOf(
        stringResource(Res.string.animal_diary_type_feeding),
        stringResource(Res.string.animal_diary_type_collect),
        stringResource(Res.string.animal_diary_type_vet),
        stringResource(Res.string.animal_diary_type_clean),
        stringResource(Res.string.animal_diary_type_vaccine),
        stringResource(Res.string.animal_diary_type_other)
    )
    var selectedType by remember { mutableStateOf(taskTypes[0]) }

    // --- LÓGICA DE CARGA PARA MODO EDICIÓN ---
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(if (isEditMode) Res.string.diary_edit_entry_title else Res.string.animal_diary_new_entry_title),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, null) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )

                if (!isEditMode) {
                    TabRow(
                        selectedTabIndex = 1,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[1]), color = MaterialTheme.colorScheme.primary)
                        }
                    ) {
                        Tab(
                            selected = false, onClick = {
                                navController.popBackStack()
                                navController.navigate(AppScreens.createAddDiaryRoute(initialDateMillis))
                            },
                            text = { Text(stringResource(Res.string.tab_gardening), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Filled.Grass, null) }
                        )
                        Tab(
                            selected = true, onClick = {},
                            text = { Text(stringResource(Res.string.tab_animals), fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Filled.Pets, null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {

            // Ubicación
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(Res.string.diary_section_location), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = expandedCercado, onExpandedChange = { expandedCercado = it }) {
                        OutlinedTextField(
                            value = selectedCercado?.nombre ?: stringResource(Res.string.cercado_select_placeholder),
                            onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCercado) }
                        )
                        ExposedDropdownMenu(expanded = expandedCercado, onDismissRequest = { expandedCercado = false }) {
                            cercados.forEach { cercado ->
                                DropdownMenuItem(
                                    text = { Text(cercado.nombre) },
                                    onClick = { selectedCercado = cercado; expandedCercado = false }
                                )
                            }
                        }
                    }
                }
            }

            // Detalles
            OutlinedCard(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(stringResource(Res.string.diary_section_details), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    HuertaInput(value = title, onValueChange = { title = it }, label = stringResource(Res.string.diary_task_title_hint), icon = Icons.Filled.Title, imeAction = ImeAction.Next)
                    HuertaInput(value = description, onValueChange = { description = it }, label = stringResource(Res.string.diary_notes_hint), icon = Icons.Filled.Description, imeAction = ImeAction.Done)

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(Res.string.diary_type_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            taskTypes.forEach { type ->
                                FilterChip(
                                    selected = (type == selectedType),
                                    onClick = { selectedType = type },
                                    label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = type == selectedType, borderColor = MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }
                    }
                }
            }

            // Botón Guardar / Actualizar
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedCercado != null) {
                        val finalDesc = if (description.isNotBlank()) "$title | $description" else title
                        val dateMillis = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                        val idToUpdate = taskId?.toLongOrNull() ?: 0L

                        viewModel.guardarEntradaDiarioAnimal(
                            id = idToUpdate,
                            cercadoId = selectedCercado!!.id,
                            tipo = selectedType,
                            desc = finalDesc,
                            fecha = dateMillis,
                            foto = null,
                            cantidad = 0f
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && selectedCercado != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(if (isEditMode) Res.string.btn_update else Res.string.animal_diary_register_btn),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}