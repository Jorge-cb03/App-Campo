package com.example.proyecto.ui.diary

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.BancalEntity
import com.example.proyecto.data.database.entity.EntradaDiarioEntity
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.theme.GreenPrimary
import com.example.proyecto.ui.theme.RedDanger
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(navController: NavController, taskId: Long, viewModel: GardenViewModel = koinViewModel()) {
    var entrada by remember { mutableStateOf<EntradaDiarioEntity?>(null) }
    var bancalAsociado by remember { mutableStateOf<BancalEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(taskId) {
        val tarea = viewModel.getEntradaDiarioById(taskId)
        entrada = tarea
        tarea?.let {
            bancalAsociado = viewModel.getBancalById(it.bancalId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }
                },
                actions = {
                    Box(modifier = Modifier.padding(8.dp)) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) { Icon(Icons.Default.MoreVert, null, tint = Color.White) }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.diary_edit_record)) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    entrada?.let { item ->
                                        navController.navigate("add_diary_entry/${item.fecha}?taskId=${item.id}")
                                    }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.diary_delete_record), color = RedDanger) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = RedDanger) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (entrada == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val item = entrada!!
            val date = Instant.fromEpochMilliseconds(item.fecha).toLocalDateTime(TimeZone.currentSystemDefault())
            val formattedDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // --- CABECERA VISUAL ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (item.foto != null) {
                        val bitmap = BitmapFactory.decodeByteArray(item.foto, 0, item.foto.size).asImageBitmap()
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(GreenPrimary, GreenPrimary.copy(alpha = 0.6f))
                                    )
                                )
                        )
                    }

                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(100.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = getIconForAction(item.tipoAccion),
                                    contentDescription = null,
                                    tint = GreenPrimary,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = item.tipoAccion.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // --- TARJETA DE CONTENIDO ---
                Column(
                    modifier = Modifier
                        .offset(y = (-40).dp)
                        .padding(horizontal = 20.dp)
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(stringResource(Res.string.diary_detail_tech_sheet), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                            Spacer(Modifier.height(16.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                InfoItem(icon = Icons.Rounded.CalendarToday, label = stringResource(Res.string.diary_detail_date), value = formattedDate)
                                InfoItem(icon = Icons.Rounded.Grass, label = stringResource(Res.string.diary_detail_location), value = bancalAsociado?.nombreCultivo ?: "Bancal ${bancalAsociado?.fila ?: "-"}-${bancalAsociado?.columna ?: "-"}")
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.diary_detail_notes_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.descripcion.ifBlank { stringResource(Res.string.diary_detail_no_notes) },
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.diary_delete_record)) },
            text = { Text(stringResource(Res.string.diary_delete_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarEntradaDiario(taskId)
                        showDeleteConfirm = false
                        showSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) { Text(stringResource(Res.string.btn_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title)) },
            text = { Text(stringResource(Res.string.dialog_success_diary_deleted)) },
            confirmButton = { Button(onClick = { showSuccessDialog = false; navController.popBackStack() }) { Text(stringResource(Res.string.dialog_btn_ok)) } }
        )
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(GreenPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun getIconForAction(actionType: String): ImageVector {
    return when (actionType.uppercase().trim()) {
        "SIEMBRA" -> Icons.Rounded.Eco
        "RIEGO" -> Icons.Rounded.WaterDrop
        "COSECHA" -> Icons.Rounded.Agriculture
        "PODA" -> Icons.Rounded.ContentCut
        "FERTILIZANTE", "ABONADO" -> Icons.Rounded.Science
        "TRASPLANTE" -> Icons.Rounded.MoveDown
        else -> Icons.Rounded.EventNote
    }
}