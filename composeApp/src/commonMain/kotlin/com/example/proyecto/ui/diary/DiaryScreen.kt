package com.example.proyecto.ui.diary

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.ui.HuertaCard
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.ui.theme.GreenPrimary
import com.example.proyecto.ui.theme.RedDanger
import kotlinx.datetime.*
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*

@Composable
fun DiaryScreen(navController: NavController, viewModel: GardenViewModel = koinViewModel()) {
    val historialState = viewModel.historialGeneral.collectAsState()
    val historial = historialState.value

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    var selectedDate by remember { mutableStateOf(today) }
    var currentMonth by remember { mutableStateOf(today.monthNumber) }
    var currentYear by remember { mutableStateOf(today.year) }

    // ESTADO DEL DIÁLOGO (Inicializado al principio)
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    val entriesForSelectedDay = historial.filter {
        val date = Instant.fromEpochMilliseconds(it.fecha).toLocalDateTime(TimeZone.currentSystemDefault()).date
        date == selectedDate
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val epoch = selectedDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    navController.navigate(AppScreens.createAddDiaryRoute(epoch))
                },
                containerColor = GreenPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.diary_add_btn))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(Res.string.diary_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(Modifier.height(20.dp))

            HuertaCard {
                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        IconButton(onClick = { if (currentMonth == 1) { currentMonth = 12; currentYear-- } else currentMonth-- }) { Icon(Icons.Default.ChevronLeft, null) }
                        Text("${getMonthName(currentMonth)} $currentYear", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { if (currentMonth == 12) { currentMonth = 1; currentYear++ } else currentMonth++ }) { Icon(Icons.Default.ChevronRight, null) }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(240.dp).padding(top = 10.dp)) {
                        val days = getDaysInMonth(currentMonth, currentYear)
                        val firstDay = getFirstDayOfWeek(currentMonth, currentYear)
                        items(firstDay) { Spacer(Modifier.fillMaxSize()) }
                        items(days) { i ->
                            val day = i + 1
                            val isSelected = selectedDate.dayOfMonth == day && selectedDate.monthNumber == currentMonth && selectedDate.year == currentYear
                            Box(modifier = Modifier.aspectRatio(1f).padding(4.dp).clip(CircleShape).background(if (isSelected) GreenPrimary else Color.Transparent).clickable { selectedDate = LocalDate(currentYear, currentMonth, day) }, contentAlignment = Alignment.Center) {
                                Text(text = day.toString(), color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(25.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(text = stringResource(Res.string.diary_today_tasks), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (entriesForSelectedDay.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(stringResource(Res.string.diary_no_tasks), color = Color.Gray) }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(entriesForSelectedDay) { entrada ->
                        TimelineItem(
                            title = entrada.tipoAccion,
                            desc = entrada.descripcion,
                            time = stringResource(Res.string.diary_status_done),
                            icon = when(entrada.tipoAccion) {
                                "RIEGO" -> Icons.Default.WaterDrop
                                "PODA" -> Icons.Default.ContentCut
                                else -> Icons.Default.Agriculture
                            },
                            color = GreenPrimary,
                            showLine = true,
                            onClick = {
                                navController.navigate(AppScreens.createDiaryDetailRoute(entrada.id))
                            },
                            onEdit = { navController.navigate("add_diary_entry/${entrada.fecha}?taskId=${entrada.id}") },

                            // --- CORRECCIÓN AQUÍ: No borramos, solo mostramos el diálogo ---
                            onDelete = { showDeleteConfirm = entrada.id }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGO CONFIRMACIÓN ELIMINAR
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.dialog_warning_title)) },
            text = { Text(stringResource(Res.string.diary_delete_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        // AQUÍ ES DONDE BORRAMOS REALMENTE
                        viewModel.eliminarEntradaDiario(showDeleteConfirm!!)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text(stringResource(Res.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
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
    color: Color,
    showLine: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(42.dp)) {
            Box(modifier = Modifier.size(32.dp).border(2.dp, color, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(16.dp)) }
            if (showLine) Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
        }

        HuertaCard(modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(text = title, fontWeight = FontWeight.Bold)
                            Text(text = time, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = desc, fontSize = 13.sp)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null) }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.menu_edit)) },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.menu_delete), color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    // Esta llamada ahora activa el estado showDeleteConfirm en el padre
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            )
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

fun getFirstDayOfWeek(month: Int, year: Int): Int { return LocalDate(year, month, 1).dayOfWeek.ordinal }

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