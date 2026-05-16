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

    var animalAEditar by remember { mutableStateOf<AnimalEntity?>(null) }
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
                        AnimalIndividualCard(animal, ficha, { animalAEditar = animal }, { viewModel.borrarAnimal(animal) })
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
                                // Usamos el stringResource multi-idioma para "Recoger huevos de..."
                                Text(stringResource(Res.string.action_collect_eggs_from, tipo))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }

            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { showFeedDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Restaurant, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    // String multi-idioma "Alimentar todo el cercado"
                    Text(stringResource(Res.string.action_feed_cercado), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }

    if (showEggDialogForType != null && cercadoActual != null) {
        CollectEggsDialog(
            onConfirm = { cantidad ->
                viewModel.registrarPuestaGrupo(cercadoActual, showEggDialogForType!!, cantidad)
                showEggDialogForType = null
            },
            onDismiss = { showEggDialogForType = null }
        )
    }

    if (showFeedDialog && cercadoActual != null) {
        FeedDialog(
            onConfirm = { sacos ->
                viewModel.alimentarGrupo(cercadoActual, "Cercado Completo", sacos)
                showFeedDialog = false
            },
            onDismiss = { showFeedDialog = false }
        )
    }

    if (animalAEditar != null) {
        var nuevoNombre by remember { mutableStateOf(animalAEditar!!.nombre) }
        AlertDialog(
            onDismissRequest = { animalAEditar = null },
            title = { Text(stringResource(Res.string.edit_animal_title)) },
            text = { OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text(stringResource(Res.string.animal_name_label)) }) },
            confirmButton = { TextButton(onClick = { viewModel.editarAnimal(animalAEditar!!, nuevoNombre); animalAEditar = null }) { Text(stringResource(Res.string.btn_save)) } },
            dismissButton = { TextButton(onClick = { animalAEditar = null }) { Text(stringResource(Res.string.btn_cancel)) } }
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