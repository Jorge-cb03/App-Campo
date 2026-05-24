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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.database.entity.EntradaDiarioAnimalEntity
import com.example.proyecto.ui.animals.AnimalsViewModel
import com.example.proyecto.ui.theme.RedDanger
import huertomanager.composeapp.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDiaryDetailScreen(
    navController: NavController,
    taskId: Long,
    viewModel: AnimalsViewModel = koinViewModel()
) {
    var entrada by remember { mutableStateOf<EntradaDiarioAnimalEntity?>(null) }
    var cercadoAsociado by remember { mutableStateOf<CercadoEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val cercados by viewModel.cercados.collectAsState(initial = emptyList())
    val scrollState = rememberScrollState()

    LaunchedEffect(taskId) {
        entrada = viewModel.getEntradaDiarioAnimalById(taskId)
    }

    LaunchedEffect(entrada, cercados) {
        entrada?.let { e ->
            cercadoAsociado = cercados.find { c -> c.id == e.cercadoId }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Box(modifier = Modifier.padding(8.dp)) {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                CircleShape
                            )
                        ) {
                            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurface)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.diary_edit_record)) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    entrada?.let { item ->
                                        navController.navigate("diary_fauna_task/${item.fecha}?taskId=${item.id}")
                                    }
                                }
                            )
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
        }
    ) { padding ->

        if (entrada == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val item = entrada!!
            val sinNotas = stringResource(Res.string.diary_no_notes_short)
            val sinCercado = stringResource(Res.string.cercado_none)

            val date = Instant.fromEpochMilliseconds(item.fecha)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val formattedDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

                // ── CABECERA ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                ) {
                    if (item.foto != null) {
                        val bitmap = BitmapFactory
                            .decodeByteArray(item.foto, 0, item.foto.size)
                            .asImageBitmap()
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        )
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.tipoAccion.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // ── CUERPO ────────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // Tarjeta: Fecha | Cercado
                    OutlinedCard(
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            DetailInfoBox(
                                icon = Icons.Rounded.CalendarToday,
                                label = stringResource(Res.string.diary_detail_date),
                                value = formattedDate
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            DetailInfoBox(
                                icon = Icons.Rounded.Fence,
                                label = stringResource(Res.string.cercado_label),
                                value = cercadoAsociado?.let { "${it.numero} - ${it.nombre}" }
                                    ?: sinCercado
                            )
                        }
                    }

                    // Sección de Notas
                    Column {
                        Text(
                            text = stringResource(Res.string.diary_animal_notes_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.descripcion.ifBlank { sinNotas },
                                style = MaterialTheme.typography.bodyLarge,
                                lineHeight = 26.sp,
                                modifier = Modifier.padding(20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // ── Diálogo: confirmar borrado ───────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Warning, null, tint = RedDanger) },
            title = { Text(stringResource(Res.string.diary_delete_record), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.diary_delete_confirm_animal)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarEntradaDiarioAnimal(taskId)
                        showDeleteConfirm = false
                        showSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(Res.string.btn_cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Diálogo: borrado exitoso ─────────────────────────────────────────
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_done_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.success_animal_diary_deleted)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(Res.string.btn_accept)) }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}