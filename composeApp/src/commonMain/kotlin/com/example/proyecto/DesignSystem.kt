import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1. TUS COLORES (Copiados de las fotos) ---
object AppColors {
    val BgDark = Color(0xFF121212)       // Fondo negro suave
    val CardDark = Color(0xFF1E1E1E)     // Fondo de tarjetas
    val Primary = Color(0xFF5F9F70)      // Verde Huerta
    val TextWhite = Color(0xFFFFFFFF)
    val TextGray = Color(0xFFA0A0A0)
    val InputBg = Color(0xFF2C2C2E)      // Fondo de los inputs
    val Danger = Color(0xFFE57373)       // Rojo suave
}

// --- 2. TUS COMPONENTES CON ESTILO ---

// Úsalo en lugar de 'OutlinedTextField' para que se vea oscuro y redondo (Login)
@Composable
fun HuertaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AppColors.TextGray) },
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // Bordes muy redondos
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppColors.InputBg,
            unfocusedContainerColor = AppColors.InputBg,
            focusedTextColor = AppColors.TextWhite,
            unfocusedTextColor = AppColors.TextWhite,
            focusedIndicatorColor = AppColors.Primary, // Borde verde al tocar
            unfocusedIndicatorColor = Color.Transparent // Sin borde al soltar
        )
    )
}

// Úsalo para envolver tus secciones (Dashboard, Items)
@Composable
fun HuertaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp), // Estilo "burbuja" moderno
        colors = CardDefaults.cardColors(containerColor = AppColors.CardDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

// Tu botón verde principal
@Composable
fun HuertaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(8.dp))
    }
}

// Las "píldoras" para seleccionar Jardinera
@Composable
fun HuertaChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50), // Completamente redondo
        color = if (isSelected) AppColors.Primary else AppColors.CardDark,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else AppColors.TextGray,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            fontWeight = FontWeight.Medium
        )
    }
}