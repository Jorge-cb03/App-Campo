package com.example.proyecto.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.proyecto.data.database.entity.AlertaEntity
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.theme.GreenPrimary
import com.example.proyecto.ui.theme.RedDanger
import huertomanager.composeapp.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    navController: NavController,
    viewModel: GardenViewModel = koinViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    var alertToEdit by remember { mutableStateOf<AlertaEntity?>(null) }
    var alertToDelete by remember { mutableStateOf<AlertaEntity?>(null) }

    val msgSaved = stringResource(Res.string.dialog_success_alert_saved)
    val msgDeleted = stringResource(Res.string.dialog_success_alert_deleted)

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.alerts_screen_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = padding.calculateTopPadding())
        ) {
            if (alerts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(Res.string.garden_no_history), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(alerts.sortedBy { it.dateTimeEpochMillis }) { alert ->
                        AlertItem(alert = alert, onEdit = { alertToEdit = alert }, onDelete = { alertToDelete = alert })
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GreenPrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.add_alert_btn))
            }
        }
    }

    if (showAddDialog) {
        // Al crear nueva, pasamos isEdit = false para que use la hora actual por defecto
        AlertEditorDialog(
            isEdit = false,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, date, hour, minute ->
                val ldt = LocalDateTime(date.year, date.month, date.dayOfMonth, hour, minute, 0, 0)
                val epoch = ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                viewModel.addAlert(title, desc, epoch)
                showAddDialog = false
                successMessage = msgSaved
                showSuccessDialog = true
            }
        )
    }

    if (alertToEdit != null) {
        val alert = alertToEdit!!
        val instant = Instant.fromEpochMilliseconds(alert.dateTimeEpochMillis)
        val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        AlertEditorDialog(
            initialTitle = alert.title,
            initialDesc = alert.description,
            initialDate = ldt.date,
            initialHour = ldt.hour,
            initialMinute = ldt.minute,
            isEdit = true,
            onDismiss = { alertToEdit = null },
            onSave = { title, desc, date, hour, minute ->
                val newLdt = LocalDateTime(date.year, date.month, date.dayOfMonth, hour, minute, 0, 0)
                val epoch = newLdt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                viewModel.updateAlert(alert.id, title, desc, epoch)
                alertToEdit = null
                successMessage = msgSaved
                showSuccessDialog = true
            }
        )
    }

    if (alertToDelete != null) {
        AlertDialog(
            onDismissRequest = { alertToDelete = null },
            icon = { Icon(Icons.Default.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.dialog_warning_title)) },
            text = { Text(stringResource(Res.string.alert_menu_delete) + "?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteAlert(alertToDelete!!.id); alertToDelete = null; successMessage = msgDeleted; showSuccessDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = RedDanger)) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = { TextButton(onClick = { alertToDelete = null }) { Text(stringResource(Res.string.btn_cancel)) } }
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
}

@Composable
fun AlertItem(alert: AlertaEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val instant = Instant.fromEpochMilliseconds(alert.dateTimeEpochMillis)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dateStr = "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
    val timeStr = "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(GreenPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, null, tint = GreenPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = alert.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(text = alert.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = GreenPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text(text = "$dateStr - $timeStr", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, null, tint = Color.Gray) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.menu_edit)) },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.menu_delete), color = RedDanger) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertEditorDialog(
    initialTitle: String = "",
    initialDesc: String = "",
    initialDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    initialHour: Int = 12,
    initialMinute: Int = 0,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, Int, Int) -> Unit
) {
    // Calculamos la hora actual para el caso de nueva alerta
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    var name by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDesc) }
    var selectedDate by remember { mutableStateOf(initialDate) }

    // Si es edición usamos la hora programada, si es nueva usamos la hora actual
    var hour by remember { mutableStateOf(if (isEdit) initialHour else now.hour) }
    var minute by remember { mutableStateOf(if (isEdit) initialMinute else now.minute) }

    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.alert_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(Res.string.alert_name_hint)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(Res.string.add_diary_desc)) }, modifier = Modifier.fillMaxWidth())

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(Res.string.alert_date_label), modifier = Modifier.weight(1f))
                        Text(
                            text = "${selectedDate.dayOfMonth}/${selectedDate.monthNumber}/${selectedDate.year}",
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                                }
                                showDatePicker = false
                            }) { Text(stringResource(Res.string.dialog_btn_ok)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text(stringResource(Res.string.alert_cancel)) }
                        }
                    ) { DatePicker(state = datePickerState) }
                }

                Text(stringResource(Res.string.alert_notification_time), style = MaterialTheme.typography.labelMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { if (hour < 23) hour++ else hour = 0 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                        Text(hour.toString().padStart(2, '0'), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (hour > 0) hour-- else hour = 23 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { if (minute < 59) minute++ else minute = 0 }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                        Text(minute.toString().padStart(2, '0'), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (minute > 0) minute-- else minute = 59 }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, desc, selectedDate, hour, minute) }, enabled = name.isNotBlank()) { Text(stringResource(Res.string.alert_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.alert_cancel)) } }
    )
}