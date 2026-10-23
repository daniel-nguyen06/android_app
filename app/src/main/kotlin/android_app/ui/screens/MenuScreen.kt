package android_app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.*
import android_app.ui.theme.SunsetCoral
import android_app.ui.theme.MangoGold
import android_app.ui.theme.LagoonTeal
import android_app.ui.theme.SunsetCoral
import android_app.ui.theme.MangoGold
import android_app.ui.theme.LagoonTeal
import android_app.ui.theme.DeepSea
import android_app.ui.theme.SunsetCoralDark
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.runtime.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MenuScreen(
    fullName: String,
    onNavigateToFindLocation: () -> Unit,
    onNavigateToCreateTrip: () -> Unit,
    onNavigateToCreateItinerary: () -> Unit,
    onNavigateToViewItinerary: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }

    var isLoggingOut by remember { mutableStateOf(false) }
    var logoutError by remember { mutableStateOf<String?>(null) }

    val initials = remember(fullName) {
        fullName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .let { words ->
                when {
                    words.isEmpty() -> ""
                    words.size == 1 -> words.first().take(2).uppercase()
                    else -> "${words.first().first()}${words.last().first()}"
                        .uppercase()
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Header Gradient với Avatar và Hình nền mờ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            // Background Image for Header
            AsyncImage(
                model = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1000&auto=format&fit=crop", // Beach theme for menu header
                contentDescription = "Header Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Gradient Overlay to ensure text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Lời chào
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Xin chào,",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = fullName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Quyền lợi hội viên
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO */ }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quyền lợi hội viên",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // 2. Danh sách tính năng (Menu Items)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            MenuItem(
                icon = Icons.Outlined.Place,
                title = "Tìm địa điểm",
                onClick = onNavigateToFindLocation,
                iconTint = SunsetCoral // Golden Hour theme
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            
            MenuItem(
                icon = Icons.Default.Luggage,
                title = "Tạo chuyến đi",
                onClick = onNavigateToCreateTrip,
                iconTint = MangoGold // Golden Hour theme
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            
            MenuItem(
                icon = Icons.Default.EditCalendar,
                title = "Tạo lịch trình",
                onClick = onNavigateToCreateItinerary,
                iconTint = LagoonTeal // Golden Hour theme
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            
            MenuItem(
                icon = Icons.Default.ViewTimeline,
                title = "Xem lịch trình",
                onClick = onNavigateToViewItinerary,
                iconTint = DeepSea // Golden Hour theme
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            MenuItem(
                icon = Icons.Default.Settings,
                title = "Cài đặt",
                onClick = { /* TODO */ }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            MenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = if (isLoggingOut) "Đang đăng xuất..." else "Đăng xuất",
                onClick = {
                    if (!isLoggingOut) {
                        isLoggingOut = true
                        logoutError = null

                        try {
                            auth.signOut()
                            isLoggingOut = false
                            onLogout()
                        } catch (exception: Exception) {
                            isLoggingOut = false
                            logoutError =
                                exception.localizedMessage
                                    ?: "Đăng xuất thất bại."
                        }
                    }
                },
                iconTint = SunsetCoralDark,
                textColor = SunsetCoralDark
            )

            if (logoutError != null) {
                Text(
                    text = logoutError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        // 3. Bottom Card (Khám phá bản đồ)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(LagoonTeal, DeepSea)))
                    .clickable { /* TODO */ }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Map Icon Circle
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Khám phá bản đồ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tìm kiếm các điểm đến hấp dẫn quanh bạn",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
