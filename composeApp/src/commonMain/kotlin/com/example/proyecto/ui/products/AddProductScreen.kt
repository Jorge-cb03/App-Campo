package com.example.proyecto.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
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
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavController, productId: Long? = null, viewModel: GardenViewModel = koinViewModel()) {
    val isEditMode = productId != null
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
    val apiResults by viewModel.apiSearchResults.collectAsState()

    LaunchedEffect(productId) {
        if (isEditMode && productId != null) {
            viewModel.getProductoById(productId)?.let { p ->
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { keyboardController?.hide() }) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) stringResource(Res.string.product_edit_title) else stringResource(Res.string.product_add_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        // ── CLAVE: imePadding() ANTES de verticalScroll ──────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()                          // ← sube el contenido con el teclado
                .verticalScroll(rememberScrollState()) // ← scroll dentro del espacio restante
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.product_tech_sheet), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    if (!selectedImageUrl.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = selectedImageUrl, contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(stringResource(Res.string.product_linked), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (perenualId != null) Text("ID: $perenualId", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Box {
                        OutlinedTextField(
                            value = stringResource(getLocalizedTypeName(selectedType)), onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(Res.string.product_category)) },
                            trailingIcon = { IconButton(onClick = { expandedType = true; keyboardController?.hide() }) { Icon(Icons.Default.ArrowDropDown, null) } },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            ProductType.entries.forEach { type ->
                                DropdownMenuItem(text = { Text(stringResource(getLocalizedTypeName(type))) }, onClick = { selectedType = type; expandedType = false })
                            }
                        }
                    }
                    if (isSeed || selectedType == ProductType.VEGETABLE) {
                        Button(
                            onClick = { showApiSearchDialog = true; searchQuery = ""; viewModel.buscarCultivoApi(""); keyboardController?.hide() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) { Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(Res.string.product_select_db)) }
                    }
                    HuertaInput(value = name, onValueChange = { name = it }, label = stringResource(Res.string.product_name_label), icon = Icons.Default.Edit, imeAction = ImeAction.Next)
                    HuertaInput(value = stock, onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) stock = it }, label = "${stringResource(Res.string.product_stock_label)} $unitLabel", icon = Icons.Default.Inventory, imeAction = ImeAction.Next)
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        label = { Text(stringResource(Res.string.product_notes_label)) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                }
            }
            Button(
                onClick = { viewModel.guardarProducto(id = productId ?: 0L, n = name, c = selectedType.name, s = stock.toDoubleOrNull() ?: 0.0, perenualId = perenualId, imagenUrl = selectedImageUrl, nombreCientifico = scientificName, notas = notes); showSuccessDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp), enabled = name.isNotBlank(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (isEditMode) stringResource(Res.string.product_save_changes) else stringResource(Res.string.product_save_sheet), fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }

    if (showApiSearchDialog) {
        AlertDialog(
            onDismissRequest = { showApiSearchDialog = false },
            title = { Text(stringResource(Res.string.product_catalog_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery, onValueChange = { searchQuery = it; viewModel.buscarCultivoApi(it) },
                        label = { Text(stringResource(Res.string.product_filter_hint)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(apiResults) { crop ->
                                ListItem(
                                    headlineContent = { Text(crop.commonName, fontWeight = FontWeight.Medium) },
                                    leadingContent = { AsyncImage(model = crop.defaultImage?.regularUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentScale = ContentScale.Crop, placeholder = rememberVectorPainter(Icons.Default.Eco), error = rememberVectorPainter(Icons.Default.Eco)) },
                                    modifier = Modifier.clickable { name = crop.commonName; perenualId = crop.id; selectedImageUrl = crop.defaultImage?.regularUrl; scientificName = crop.scientificName.firstOrNull(); showApiSearchDialog = false }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showApiSearchDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title)) }, text = { Text(msgSaved) },
            confirmButton = { Button(onClick = { showSuccessDialog = false; navController.popBackStack() }) { Text(stringResource(Res.string.dialog_btn_ok)) } },
            containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)
        )
    }
}