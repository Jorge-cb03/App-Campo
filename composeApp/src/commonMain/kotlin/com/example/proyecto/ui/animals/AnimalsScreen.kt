package com.example.proyecto.ui.animals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.data.database.entity.CercadoEntity
import huertomanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(navController: NavController, viewModel: AnimalsViewModel = koinViewModel()) {
    val cercados by viewModel.cercados.collectAsState(initial = emptyList())
    val animales by viewModel.animales.collectAsState(initial = emptyList())

    // Estado para controlar qué cercado estamos editando
    var cercadoAEditar by remember { mutableStateOf<CercadoEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.farm_management_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_animal") }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (cercados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.farm_empty_msg))
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(cercados) { cercado ->
                    val animalesEnEsteCercado = animales.count { it.cercadoId == cercado.id }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable {
                            navController.navigate("animal_group_detail/${cercado.id}")
                        },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fence, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) { // Añadido peso para empujar el botón al final
                                Text(
                                    text = stringResource(Res.string.cercado_format, cercado.numero, cercado.nombre),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(Res.string.animals_count, animalesEnEsteCercado),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            // --- BOTÓN PARA EDITAR NOMBRE/NÚMERO ---
                            IconButton(onClick = { cercadoAEditar = cercado }) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO EMERGENTE PARA EDITAR CERCADO ---
    if (cercadoAEditar != null) {
        var num by remember(cercadoAEditar) { mutableStateOf(cercadoAEditar!!.numero.toString()) }
        var nom by remember(cercadoAEditar) { mutableStateOf(cercadoAEditar!!.nombre) }

        AlertDialog(
            onDismissRequest = { cercadoAEditar = null },
            title = { Text(stringResource(Res.string.edit_cercado_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = num,
                        onValueChange = { if(it.all { c -> c.isDigit() }) num = it },
                        label = { Text(stringResource(Res.string.cercado_num_hint)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text(stringResource(Res.string.cercado_name_hint)) }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.editarCercado(cercadoAEditar!!, num.toIntOrNull() ?: 0, nom)
                    cercadoAEditar = null
                }) { Text(stringResource(Res.string.btn_save)) }
            },
            dismissButton = {
                TextButton(onClick = { cercadoAEditar = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }
}