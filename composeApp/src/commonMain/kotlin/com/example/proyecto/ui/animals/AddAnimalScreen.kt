package com.example.proyecto.ui.animals

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.data.database.entity.CercadoEntity
import com.example.proyecto.data.repository.FichaAnimal
import com.example.proyecto.util.MediaManager
import huertomanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnimalScreen(
    navController: NavController,
    viewModel: AnimalsViewModel = koinViewModel()
) {
    var name by remember { mutableStateOf("") }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }

    val catalogo: List<FichaAnimal> = viewModel.catalogo
    var expandedTipo by remember { mutableStateOf(false) }
    var selectedFicha by remember { mutableStateOf(catalogo.first()) }
    var isLayer by remember { mutableStateOf(selectedFicha.esPonedora) }

    val cercadosList by viewModel.cercados.collectAsState(initial = emptyList())
    var expandedCercado by remember { mutableStateOf(false) }
    var cercadoSeleccionado by remember { mutableStateOf<CercadoEntity?>(null) }
    var showNewCercadoDialog by remember { mutableStateOf(false) }

    val launcher = MediaManager.rememberLauncher { bytes -> photoBytes = bytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.add_animal_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable { launcher.launchGallery() },
                contentAlignment = Alignment.Center
            ) {
                if (photoBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes!!.size)
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    AsyncImage(model = selectedFicha.imagenUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp), tint = Color.White)
            }

            Spacer(Modifier.height(24.dp))

            ExposedDropdownMenuBox(
                expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }, modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedFicha.nombre, onValueChange = {}, readOnly = true,
                    label = { Text(stringResource(Res.string.add_animal_type_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                )
                ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                    catalogo.forEach { ficha ->
                        DropdownMenuItem(
                            text = { Text(ficha.nombre) },
                            onClick = { selectedFicha = ficha; isLayer = ficha.esPonedora; expandedTipo = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expandedCercado, onExpandedChange = { expandedCercado = !expandedCercado }, modifier = Modifier.weight(1f)
                ) {
                    val placeholder = stringResource(Res.string.cercado_select_placeholder)
                    OutlinedTextField(
                        value = cercadoSeleccionado?.let { stringResource(Res.string.cercado_dropdown_format, it.numero, it.nombre) } ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(Res.string.cercado_label)) },
                        placeholder = { Text(placeholder) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCercado) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(16.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedCercado, onDismissRequest = { expandedCercado = false }) {
                        cercadosList.forEach { cercado ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.cercado_dropdown_format, cercado.numero, cercado.nombre)) },
                                onClick = { cercadoSeleccionado = cercado; expandedCercado = false }
                            )
                        }
                    }
                }
                IconButton(onClick = { showNewCercadoDialog = true }) { Icon(Icons.Default.Add, null) }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(Res.string.add_animal_name_hint)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.add_animal_is_layer), modifier = Modifier.weight(1f))
                Switch(checked = isLayer, onCheckedChange = { isLayer = it })
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    cercadoSeleccionado?.let { cercado ->
                        viewModel.addAnimal(name, selectedFicha.nombre, cercado.id, isLayer, photoBytes)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && cercadoSeleccionado != null
            ) { Text(stringResource(Res.string.btn_save)) }
        }
    }

    if (showNewCercadoDialog) {
        NewCercadoDialog(
            onConfirm = { num, nom -> viewModel.addCercado(num, nom); showNewCercadoDialog = false },
            onDismiss = { showNewCercadoDialog = false }
        )
    }
}

@Composable
fun NewCercadoDialog(onConfirm: (Int, String) -> Unit, onDismiss: () -> Unit) {
    var num by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_cercado_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = num, onValueChange = { if(it.all { c -> c.isDigit() }) num = it },
                    label = { Text(stringResource(Res.string.cercado_num_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nom, onValueChange = { nom = it },
                    label = { Text(stringResource(Res.string.cercado_name_hint)) }
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(num.toIntOrNull() ?: 0, nom) }) { Text(stringResource(Res.string.btn_create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.btn_cancel)) } }
    )
}