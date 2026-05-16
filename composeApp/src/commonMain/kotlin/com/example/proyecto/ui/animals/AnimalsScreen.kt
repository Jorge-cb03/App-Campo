package com.example.proyecto.ui.animals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import huertomanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalsScreen(navController: NavController, viewModel: AnimalsViewModel = koinViewModel()) {
    val cercados by viewModel.cercados.collectAsState(initial = emptyList())
    val animales by viewModel.animales.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.farm_management_title)) }) },
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
                            Column {
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
                        }
                    }
                }
            }
        }
    }
}