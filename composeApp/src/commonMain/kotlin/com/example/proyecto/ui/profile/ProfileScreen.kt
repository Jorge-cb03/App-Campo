package com.example.proyecto.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.proyecto.ui.HuertaInput
import com.example.proyecto.ui.garden.GardenViewModel
import com.example.proyecto.ui.navigation.AppScreens
import com.example.proyecto.util.MediaManager
import org.jetbrains.compose.resources.stringResource
import huertomanager.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    viewModel: GardenViewModel = koinViewModel()
) {
    val usuario by viewModel.usuarioActivo.collectAsState()

    var userName by remember(usuario) { mutableStateOf(usuario?.nombre ?: "Usuario") }
    var userEmail by remember(usuario) { mutableStateOf(usuario?.email ?: "usuario@email.com") }
    var profilePhotoBytes by remember(usuario) { mutableStateOf(usuario?.fotoPerfil) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val msgUpdated = stringResource(Res.string.dialog_success_profile_updated)

    val launcher = MediaManager.rememberLauncher { bytes ->
        if (bytes != null) {
            profilePhotoBytes = bytes
            showPhotoOptions = false
            viewModel.guardarPerfil(userName, userEmail, bytes)
            showSuccessDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Fondo con degradado sutil en la parte superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Avatar Minimalista
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .clickable { showPhotoOptions = true },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = CircleShape
                ) {
                    if (profilePhotoBytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(profilePhotoBytes, 0, profilePhotoBytes!!.size)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            null,
                            modifier = Modifier.padding(32.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
                }

                // Botón Cámara pequeño y flotante
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable { showPhotoOptions = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Nombre y Email
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if(userName == "Usuario") stringResource(Res.string.profile_user_default) else userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(
                    onClick = { tempName = userName; showEditNameDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(40.dp))

            // Sección de Ajustes
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.profile_settings).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        SettingItem(
                            icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            title = stringResource(if (isDarkTheme) Res.string.pref_dark_mode else Res.string.pref_light_mode),
                            iconColor = if (isDarkTheme) Color(0xFF9FA8DA) else Color(0xFFFFB74D),
                            trailingContent = {
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = onToggleTheme,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingItem(
                            icon = Icons.Default.Info,
                            title = stringResource(Res.string.about_title),
                            iconColor = MaterialTheme.colorScheme.secondary,
                            onClick = { navController.navigate(AppScreens.About) }
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Botón Cerrar Sesión Minimalista
                TextButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.profile_logout), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }

    // --- DIÁLOGOS REDISEÑADOS (M3) ---

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(Res.string.profile_edit_dialog_title), fontWeight = FontWeight.Bold) },
            text = { HuertaInput(tempName, { tempName = it }, stringResource(Res.string.profile_new_name_hint), Icons.Default.Person, imeAction = ImeAction.Done) },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            userName = tempName
                            viewModel.guardarPerfil(userName, userEmail, profilePhotoBytes)
                            showEditNameDialog = false
                            showSuccessDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(Res.string.btn_save)) }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text(stringResource(Res.string.btn_cancel)) } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(stringResource(Res.string.dialog_success_title), fontWeight = FontWeight.Bold) },
            text = { Text(msgUpdated) },
            confirmButton = { Button(onClick = { showSuccessDialog = false }, shape = RoundedCornerShape(12.dp)) { Text(stringResource(Res.string.dialog_btn_ok)) } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showPhotoOptions) {
        Dialog(onDismissRequest = { showPhotoOptions = false }) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.profile_change_photo_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { launcher.launchCamera(); showPhotoOptions = false },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(Res.string.diary_btn_take_photo), fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { launcher.launchGallery(); showPhotoOptions = false },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.Outlined.Image, null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(Res.string.diary_btn_gallery), color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(Modifier.height(24.dp))

                    TextButton(onClick = { showPhotoOptions = false }) {
                        Text(stringResource(Res.string.btn_cancel), color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(Res.string.profile_logout), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.profile_logout_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        navController.navigate(AppScreens.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(Res.string.btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(Res.string.btn_cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    iconColor: Color,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}