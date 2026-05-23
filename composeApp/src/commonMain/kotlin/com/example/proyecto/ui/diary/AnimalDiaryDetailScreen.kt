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
import kotlinx.datetime.*
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

    // Actualiza el cercado cuando tengamos tanto la entrada como la lista de cercados
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
                            // ── Editar ──────────────────────────────────────
                            DropdownMenuItem(
                                text = { Text("Editar registro") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    showMenu = false
                                    entrada?.let { item ->
                                        navController.navigate("diary_fauna_task/${item.fecha}?taskId=${item.id}")
                                    }
                                }
                            )
                            // ── Eliminar ────────────────────────────────────
                            DropdownMenuItem(
                                text = { Text("Eliminar registro", color = RedDanger) },
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
            val date = Instant.fromEpochMilliseconds(item.fecha)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val formattedDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {

                // ── CABECERA — idéntica a DiaryDetailScreen ──────────────
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

                    // Badge de tipo de acción — igual que en jardinería
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

                // ── CUERPO — mismo layout que DiaryDetailScreen ──────────
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
                            // Fecha
                            DetailInfoBox(
                                icon = Icons.Rounded.CalendarToday,
                                label = "Fecha",
                                value = formattedDate
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            // Cercado (equivalente al Bancal en jardinería)
                            DetailInfoBox(
                                icon = Icons.Rounded.Fence,
                                label = "Cercado",
                                value = cercadoAsociado?.let { "${it.numero} - ${it.nombre}" }
                                    ?: "Sin cercado"
                            )
                        }
                    }

                    // Sección de Notas
                    Column {
                        Text(
                            text = "Notas del ganadero",
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
                                text = item.descripcion.ifBlank { "Sin notas adicionales." },
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
            title = { Text("Eliminar registro", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que quieres eliminar este registro? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarEntradaDiarioAnimal(taskId)
                        showDeleteConfirm = false
                        showSuccessDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    // ── Diálogo: borrado exitoso (igual que jardinería) ──────────────────
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("¡Listo!", fontWeight = FontWeight.Bold) },
            text = { Text("El registro de animales se ha eliminado correctamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        navController.popBackStack()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Aceptar") }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}