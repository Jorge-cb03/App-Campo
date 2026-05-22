package com.example.proyecto.ui.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.EntradaDiarioEntity
import com.example.proyecto.ui.animals.AnimalsViewModel
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.RedDanger
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: GardenViewModel = koinViewModel(),
    animalsViewModel: AnimalsViewModel = koinViewModel()
) {
    val historialHuerto by viewModel.historialGeneral.collectAsState()
    val historialGranja by animalsViewModel.diarioAnimales.collectAsState(initial = emptyList())

    val historialCombinado = remember(historialHuerto, historialGranja) {
        val granjaMapeada = historialGranja.map {
            EntradaDiarioEntity(
                id = it.id,
                bancalId = -1L,
                tipoAccion = it.tipoAccion,
                descripcion = it.descripcion,
                fecha = it.fecha,
                foto = null
            )
        }
        (historialHuerto + granjaMapeada).sortedByDescending { it.fecha }
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedDate by remember { mutableStateOf(today) }
    var currentMonth by remember { mutableStateOf(today.monthNumber) }
    var currentYear by remember { mutableStateOf(today.year) }

    var showDeleteConfirm by remember { mutableStateOf<EntradaDiarioEntity?>(null) }
    // Estado para controlar qué entrada de la granja se va a editar
    var animalEntryToEdit by remember { mutableStateOf<EntradaDiarioEntity?>(null) }

    val entriesForSelectedDay = historialCombinado.filter {
        val date = Instant.fromEpochMilliseconds(it.fecha).toLocalDateTime(TimeZone.currentSystemDefault()).date
        date == selectedDate
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val epoch = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    navController.navigate(AppScreens.createAddDiaryRoute(epoch))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.diary_add_btn))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            Text(text = stringResource(Res.string.diary_title), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(24.dp))

            // Calendario
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = { if (currentMonth == 1) { currentMonth = 12; currentYear-- } else currentMonth-- }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Default.ChevronLeft, null) }
                    Text(text = "${getMonthName(currentMonth)} $currentYear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { if (currentMonth == 12) { currentMonth = 1; currentYear++ } else currentMonth++ }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Default.ChevronRight, null) }
                }
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(240.dp)) {
                    val days = getDaysInMonth(currentMonth, currentYear)
                    val firstDay = getFirstDayOfWeek(currentMonth, currentYear)

                    items(firstDay) { Spacer(Modifier.fillMaxSize()) }
                    items(days) { i ->
                        val day = i + 1
                        val isSelected = selectedDate.dayOfMonth == day && selectedDate.monthNumber == currentMonth && selectedDate.year == currentYear
                        val isToday = today.dayOfMonth == day && today.monthNumber == currentMonth && today.year == currentYear

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(width = if (isToday && !isSelected) 1.dp else 0.dp, color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent, shape = CircleShape)
                                .clickable { selectedDate = LocalDate(currentYear, currentMonth, day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = day.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            Text(text = stringResource(Res.string.diary_today_tasks), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            if (entriesForSelectedDay.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.EventAvailable, null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(Res.string.diary_no_tasks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(entriesForSelectedDay) { entrada ->
                        val isGranja = entrada.bancalId == -1L

                        TimelineItem(
                            title = entrada.tipoAccion,
                            desc = entrada.descripcion,
                            time = stringResource(Res.string.diary_status_done),
                            icon = when(entrada.tipoAccion) {
                                "RIEGO" -> Icons.Default.WaterDrop
                                "PODA" -> Icons.Default.ContentCut
                                "PRODUCCIÓN" -> Icons.Default.Egg
                                "ALIMENTACIÓN" -> Icons.Default.Restaurant
                                "ALTA ANIMAL" -> Icons.Default.Pets
                                "BAJA ANIMAL" -> Icons.Default.RemoveCircle
                                else -> Icons.Default.Agriculture
                            },
                            showLine = true,
                            canEdit = true, // <-- CAMBIADO A TRUE: Ahora todo se puede editar
                            onClick = {
                                if (!isGranja) navController.navigate(AppScreens.createDiaryDetailRoute(entrada.id))
                            },
                            onEdit = {
                                if (!isGranja) {
                                    navController.navigate("add_diary_entry/${entrada.fecha}?taskId=${entrada.id}")
                                } else {
                                    // Capturamos el elemento para editarlo inline mediante Dialogo
                                    animalEntryToEdit = entrada
                                }
                            },
                            onDelete = { showDeleteConfirm = entrada }
                        )
                    }
                }
            }
        }
    }

    // --- DIÁLOGO EMERGENTE PARA EDITAR REGISTROS DE ANIMALES ---
    if (animalEntryToEdit != null) {
        val registroOriginal = historialGranja.find { it.id == animalEntryToEdit!!.id }

        var desc by remember(animalEntryToEdit) { mutableStateOf(animalEntryToEdit!!.descripcion) }
        var cantidad by remember(registroOriginal) { mutableStateOf(registroOriginal?.cantidad?.toString()?.removeSuffix(".0") ?: "") }

        AlertDialog(
            onDismissRequest = { animalEntryToEdit = null },
            title = { Text(stringResource(Res.string.edit_farm_entry_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(stringResource(Res.string.animal_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text(stringResource(Res.string.dialog_collect_eggs_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    animalsViewModel.editarEntradaDiarioAnimal(
                        id = animalEntryToEdit!!.id,
                        nuevaDescripcion = desc,
                        nuevaCantidad = cantidad.toDoubleOrNull() ?: 0.0
                    )
                    animalEntryToEdit = null
                }) { Text(stringResource(Res.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { animalEntryToEdit = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.dialog_warning_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.diary_delete_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (showDeleteConfirm!!.bancalId == -1L) {
                            animalsViewModel.eliminarEntradaDiarioAnimal(showDeleteConfirm!!.id)
                        } else {
                            viewModel.eliminarEntradaDiario(showDeleteConfirm!!.id)
                        }
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }
}
@Composable
fun TimelineItem(
    title: String,
    desc: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showLine: Boolean,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
            Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            if (showLine) Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
        }

        Card(
            modifier = Modifier.padding(bottom = 16.dp, start = 8.dp).fillMaxWidth().clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = time, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(text = desc, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            if (canEdit) {
                                DropdownMenuItem(text = { Text(stringResource(Res.string.menu_edit)) }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            }
                            DropdownMenuItem(text = { Text(stringResource(Res.string.menu_delete), color = RedDanger) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) })
                        }
                    }
                }
            }
        }
    }
}

fun getDaysInMonth(month: Int, year: Int): Int {
    val start = LocalDate(year, month, 1)
    val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return start.daysUntil(nextMonth)
}

fun getFirstDayOfWeek(month: Int, year: Int): Int {
    return LocalDate(year, month, 1).dayOfWeek.ordinal
}

@Composable
fun getMonthName(monthNumber: Int): String {
    val res = when(monthNumber) {
        1 -> Res.string.month_1
        2 -> Res.string.month_2
        3 -> Res.string.month_3
        4 -> Res.string.month_4
        5 -> Res.string.month_5
        6 -> Res.string.month_6
        7 -> Res.string.month_7
        8 -> Res.string.month_8
        9 -> Res.string.month_9
        10 -> Res.string.month_10
        11 -> Res.string.month_11
        else -> Res.string.month_12
    }
    return stringResource(res)
}