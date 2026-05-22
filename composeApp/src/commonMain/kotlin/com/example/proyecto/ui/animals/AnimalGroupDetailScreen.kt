package com.example.proyecto.ui.animals

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.AnimalEntity
import com.example.proyecto.data.repository.FichaAnimal
import huertomanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalGroupDetailScreen(
    navController: NavController,
    cercadoIdString: String,
    viewModel: AnimalsViewModel = koinViewModel()
) {
    val cercadoId = cercadoIdString.toLongOrNull() ?: return

    val cercados by viewModel.cercados.collectAsState(initial = emptyList())
    val animales by viewModel.animales.collectAsState(initial = emptyList())

    val cercadoActual = cercados.find { it.id == cercadoId }
    val animalesEnCercado = animales.filter { it.cercadoId == cercadoId }
    val animalesAgrupadosPorTipo = animalesEnCercado.groupBy { it.tipo }

    // --- ESTADOS PARA DIÁLOGOS Y REALIMENTACIÓN ---
    var animalAEditar by remember { mutableStateOf<AnimalEntity?>(null) }
    var animalADeleteConfirm by remember { mutableStateOf<AnimalEntity?>(null) } // Captura para confirmación
    var successType by remember { mutableStateOf<String?>(null) } // "eggs", "feed", "edited", "deleted"

    var showEggDialogForType by remember { mutableStateOf<String?>(null) }
    var showFeedDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(cercadoActual?.let { stringResource(Res.string.cercado_dropdown_format, it.numero, it.nombre) } ?: "Detalle")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                animalesAgrupadosPorTipo.forEach { (tipo, listaAnimales) ->
                    item {
                        Text(
                            text = tipo.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }

                    items(listaAnimales) { animal ->
                        val ficha = viewModel.getFichaPorNombre(animal.tipo)
                        AnimalIndividualCard(
                            animal = animal,
                            ficha = ficha,
                            onEdit = { animalAEditar = animal },
                            onDelete = { animalADeleteConfirm = animal } // Abre diálogo en vez de borrar directo
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (listaAnimales.any { it.esPonedora }) {
                        item {
                            Button(
                                onClick = { showEggDialogForType = tipo },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Egg, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.action_collect_eggs_from, tipo))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            // BOTÓN GENERAL ALIMENTAR (Se bloquea si el cercado está vacío)
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { showFeedDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = animalesEnCercado.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = if (animalesEnCercado.isNotEmpty()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.action_feed_cercado),
                        color = if (animalesEnCercado.isNotEmpty()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // --- Diálogo para Recoger Huevos ---
    if (showEggDialogForType != null && cercadoActual != null) {
        CollectEggsDialog(
            onConfirm = { cantidad ->
                viewModel.registrarPuestaGrupo(cercadoActual, showEggDialogForType!!, cantidad)
                showEggDialogForType = null
                successType = "eggs"
            },
            onDismiss = { showEggDialogForType = null }
        )
    }

    // --- Diálogo para Alimentar ---
    if (showFeedDialog && cercadoActual != null) {
        FeedDialog(
            onConfirm = { sacos ->
                viewModel.alimentarGrupo(cercadoActual, "Cercado Completo", sacos)
                showFeedDialog = false
                successType = "feed"
            },
            onDismiss = { showFeedDialog = false }
        )
    }

    // --- Diálogo para Editar ---
    if (animalAEditar != null) {
        var nuevoNombre by remember { mutableStateOf(animalAEditar!!.nombre) }
        AlertDialog(
            onDismissRequest = { animalAEditar = null },
            title = { Text(stringResource(Res.string.edit_animal_title)) },
            text = { OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text(stringResource(Res.string.animal_name_label)) }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.editarAnimal(animalAEditar!!, nuevoNombre)
                    animalAEditar = null
                    successType = "edited"
                }) { Text(stringResource(Res.string.btn_save)) }
            },
            dismissButton = { TextButton(onClick = { animalAEditar = null }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    // --- 1. NUEVO DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN ---
    if (animalADeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { animalADeleteConfirm = null },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(Res.string.confirm_delete_animal_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.confirm_delete_animal_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarAnimal(animalADeleteConfirm!!)
                        animalADeleteConfirm = null
                        successType = "deleted" // Al borrar, dispara éxito
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(Res.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { animalADeleteConfirm = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }

    // --- 2. NUEVO DIÁLOGO GENERAL DE OPERACIÓN EXITOSA ---
    if (successType != null) {
        AlertDialog(
            onDismissRequest = { successType = null },
            title = { Text(stringResource(Res.string.success_title)) },
            text = {
                Text(
                    when (successType) {
                        "eggs" -> stringResource(Res.string.success_eggs_collected)
                        "feed" -> stringResource(Res.string.success_feed_given)
                        "edited" -> stringResource(Res.string.success_animal_edited)
                        else -> stringResource(Res.string.success_animal_deleted)
                    }
                )
            },
            confirmButton = {
                Button(onClick = { successType = null }) {
                    Text(stringResource(Res.string.btn_confirm))
                }
            }
        )
    }
}
@Composable
fun FeedDialog(onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var cantidad by remember { mutableStateOf("1.0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.feed_cercado_title)) },
        text = {
            OutlinedTextField(
                value = cantidad, onValueChange = { cantidad = it },
                label = { Text(stringResource(Res.string.feed_sacks_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onConfirm(cantidad.toDoubleOrNull() ?: 0.0) }) { Text(stringResource(Res.string.btn_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel)) } }
    )
}

@Composable
fun CollectEggsDialog(onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var cantidad by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_collect_eggs_title)) },
        text = {
            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) { cantidad = it }
                },
                label = { Text(stringResource(Res.string.dialog_collect_eggs_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(cantidad.toDoubleOrNull() ?: 0.0) },
                enabled = cantidad.isNotBlank()
            ) { Text(stringResource(Res.string.btn_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel)) } }
    )
}

@Composable
fun AnimalIndividualCard(animal: AnimalEntity, ficha: FichaAnimal?, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                if (animal.fotoPerfil != null) {
                    val bitmap = BitmapFactory.decodeByteArray(animal.fotoPerfil, 0, animal.fotoPerfil.size)
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (ficha != null) {
                    AsyncImage(model = ficha.imagenUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(animal.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}