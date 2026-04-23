package com.example.proyecto.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.proyecto.domain.model.ProductType
import com.example.proyecto.ui.HuertaInput
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.data.repository.PerenualSpecies
import com.example.proyecto.ui.theme.GreenPrimary
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: Long? = null,
    viewModel: GardenViewModel = koinViewModel()
) {
    val isEditMode = productId != null

    // Estados para los campos de texto
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var perenualId by remember { mutableStateOf<Int?>(null) }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    var scientificName by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf(ProductType.SEED) }

    var expandedType by remember { mutableStateOf(false) }
    var showApiSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val msgSaved = stringResource(Res.string.dialog_success_product_saved)
    val apiResults: List<PerenualSpecies> by viewModel.apiSearchResults.collectAsState(emptyList())

    // --- CARGA DE DATOS PARA EDICIÓN ---
    LaunchedEffect(productId) {
        if (isEditMode && productId != null) {
            val producto = viewModel.getProductoById(productId)
            producto?.let { p ->
                // Actualizamos los estados con los datos de la base de datos
                name = p.nombre
                stock = if (p.stock % 1.0 == 0.0) p.stock.toInt().toString() else p.stock.toString()
                notes = p.notasCultivo ?: ""
                perenualId = p.perenualId
                selectedImageUrl = p.imagenUrl
                scientificName = p.nombreCientifico
                selectedType = try { ProductType.valueOf(p.categoria) } catch (e: Exception) { ProductType.SEED }
            }
        }
    }

    val isSeed = selectedType == ProductType.SEED
    val unitLabel = if (selectedType == ProductType.FERTILIZER || selectedType == ProductType.CHEMICAL) " (kg/L)" else stringResource(Res.string.product_units)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) stringResource(Res.string.product_edit_title) else stringResource(Res.string.product_add_title)) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.product_tech_sheet), style = MaterialTheme.typography.titleMedium, color = GreenPrimary)

                    if (!selectedImageUrl.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = selectedImageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(Res.string.product_linked), color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (perenualId != null) Text("ID: $perenualId", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }

                    Box {
                        OutlinedTextField(
                            value = stringResource(getLocalizedTypeName(selectedType)),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(Res.string.product_category)) },
                            trailingIcon = { IconButton(onClick = { expandedType = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            ProductType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(getLocalizedTypeName(type))) },
                                    onClick = { selectedType = type; expandedType = false }
                                )
                            }
                        }
                    }

                    if (isSeed) {
                        Button(
                            onClick = {
                                showApiSearchDialog = true
                                searchQuery = ""
                                viewModel.buscarCultivoApi("")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.product_select_db))
                        }
                    }

                    HuertaInput(
                        value = name,
                        onValueChange = { name = it },
                        label = stringResource(Res.string.product_name_label),
                        icon = Icons.Default.Edit,
                        imeAction = ImeAction.Next
                    )

                    HuertaInput(
                        value = stock,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) stock = it },
                        label = "${stringResource(Res.string.product_stock_label)} $unitLabel",
                        icon = Icons.Default.Inventory,
                        imeAction = ImeAction.Next
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(Res.string.product_notes_label)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                }
            }

            Button(
                onClick = {
                    val idToSave = productId ?: 0L
                    viewModel.guardarProducto(
                        id = idToSave,
                        n = name,
                        c = selectedType.name,
                        s = stock.toDoubleOrNull() ?: 0.0,
                        perenualId = perenualId,
                        imagenUrl = selectedImageUrl,
                        nombreCientifico = scientificName,
                        notas = notes
                    )
                    showSuccessDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) { Text(if (isEditMode) stringResource(Res.string.product_save_changes) else stringResource(Res.string.product_save_sheet)) }
        }
    }

    if (showApiSearchDialog) {
        AlertDialog(
            onDismissRequest = { showApiSearchDialog = false },
            title = { Text(stringResource(Res.string.product_catalog_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it; viewModel.buscarCultivoApi(it) },
                        label = { Text(stringResource(Res.string.product_filter_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(apiResults) { crop ->
                                ListItem(
                                    headlineContent = { Text(crop.commonName) },
                                    leadingContent = {
                                        AsyncImage(
                                            model = crop.defaultImage?.regularUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White),
                                            contentScale = ContentScale.Crop,
                                            placeholder = rememberVectorPainter(Icons.Default.Eco),
                                            error = rememberVectorPainter(Icons.Default.Eco)
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        name = crop.commonName
                                        perenualId = crop.id
                                        selectedImageUrl = crop.defaultImage?.regularUrl
                                        scientificName = crop.scientificName.firstOrNull()
                                        showApiSearchDialog = false
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showApiSearchDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title)) },
            text = { Text(msgSaved) },
            confirmButton = { Button(onClick = { showSuccessDialog = false; navController.popBackStack() }) { Text(stringResource(Res.string.dialog_btn_ok)) } }
        )
    }
}

fun getLocalizedTypeName(type: ProductType): StringResource {
    return when (type) {
        ProductType.SEED -> Res.string.type_seed
        ProductType.TOOL -> Res.string.type_tool
        ProductType.FERTILIZER -> Res.string.type_fertilizer
        ProductType.CHEMICAL -> Res.string.type_chemical
        ProductType.VEGETABLE -> Res.string.type_vegetable
        ProductType.OTHER -> Res.string.type_other
    }
}