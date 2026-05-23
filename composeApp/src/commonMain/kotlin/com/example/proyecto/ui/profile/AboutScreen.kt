package com.example.proyecto.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import huertomanager.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    var showDeveloperModal by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.about_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.btn_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(Res.string.about_app_name), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.about_version), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(32.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(Res.string.about_description), textAlign = TextAlign.Center, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = stringResource(Res.string.about_team_title), fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start).padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { showDeveloperModal = true }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.about_team_1), fontWeight = FontWeight.SemiBold)
                    Text("➔", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeveloperModal) {
        Dialog(onDismissRequest = { showDeveloperModal = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Text("J", fontSize = 50.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(Res.string.about_team_1), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.about_dev_role), fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = stringResource(Res.string.about_dev_desc), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { /* Lógica LinkedIn/Github */ }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text(stringResource(Res.string.about_github))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { /* Lógica LinkedIn */ }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                        Text(stringResource(Res.string.about_linkedin))
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    TextButton(onClick = { showDeveloperModal = false }) {
                        Text(stringResource(Res.string.about_back), fontSize = 18.sp)
                    }
                }
            }
        }
    }
}