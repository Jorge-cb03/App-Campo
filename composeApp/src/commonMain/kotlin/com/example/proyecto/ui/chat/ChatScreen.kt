package com.example.proyecto.ui.chat

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.proyecto.ui.garden.GardenViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: GardenViewModel = koinViewModel()
) {
    val chatHistory by viewModel.geminiChatHistory.collectAsState()
    val isLoading by viewModel.isGeminiLoading.collectAsState()
    var textInput by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje para que no se quede atrás al escribir
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size - if (isLoading) 0 else 1)
        }
    }

    val sendAction = {
        if (textInput.isNotBlank() && !isLoading) {
            viewModel.sendMessageToGemini(textInput.trim())
            textInput = ""
            // Mantenemos el teclado abierto por si quiere seguir escribiendo rápido
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // ¡ELIMINA EL HUECO GIGANTE!
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { keyboardController?.hide() }) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Asistente IA", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(chatHistory) { mensaje ->
                    val isUser = mensaje.isUser
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                        Card(
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isUser) 16.dp else 0.dp, bottomEnd = if (isUser) 0.dp else 16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(text = mensaje.text, modifier = Modifier.padding(12.dp), color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                        }
                    }
                }
                if (isLoading) {
                    item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp) } }
                }
            }

            // Distancia milimétrica, justo pegado al teclado
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = textInput, onValueChange = { textInput = it },
                    placeholder = { Text("Pregúntame...") },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp), maxLines = 3, enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { sendAction() })
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = sendAction, enabled = textInput.isNotBlank() && !isLoading, colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}