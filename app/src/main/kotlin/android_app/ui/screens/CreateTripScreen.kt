package android_app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp

import android_app.TripViewModel
import android_app.ui.theme.SunsetCoral
import android_app.ui.theme.LagoonTeal
import android_app.ui.theme.SandCream
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    viewModel: TripViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("") }
    
    // States for Date Pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Hàm phụ trợ để tính số ngày tự động
    fun calculateDays(start: String, end: String) {
        if (start.isNotBlank() && end.isNotBlank()) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val startD = LocalDate.parse(start, formatter)
                val endD = LocalDate.parse(end, formatter)
                
                val daysBetween = ChronoUnit.DAYS.between(startD, endD)
                if (daysBetween >= 0) {
                    days = (daysBetween + 1).toString() // Đi về cùng ngày tính là 1 ngày
                } else {
                    days = "0" // Lỗi logic ngày (ngày về trước ngày đi)
                }
            } catch (e: Exception) {
                // Ignore parse errors while typing/formatting
            }
        }
    }
    
    // Xử lý thành công
    LaunchedEffect(uiState.createTripSuccess) {
        if (uiState.createTripSuccess) {
            viewModel.resetCreateState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo chuyến đi mới") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    if (tripName.isNotBlank() && destination.isNotBlank()) {
                        viewModel.createTrip(
                            title = tripName,
                            destination = destination,
                            startDate = startDate,
                            endDate = endDate,
                            days = days.toIntOrNull() ?: 1
                        )
                    }
                },
                icon = { Icon(Icons.Default.Title, contentDescription = null) },
                text = { Text("Lưu chuyến đi", fontWeight = FontWeight.Bold) },
                containerColor = SunsetCoral,
                contentColor = Color.White,
                shape = RoundedCornerShape(percent = 50), // Bo tròn hoàn toàn
                modifier = Modifier.shadow(8.dp, shape = RoundedCornerShape(percent = 50), spotColor = SunsetCoral, ambientColor = SunsetCoral)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Welcome Banner (Thay thế ảnh bìa)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SandCream,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = SunsetCoral,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Lên kế hoạch mới",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ghi lại những hành trình đáng nhớ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Trip Name Input
            OutlinedTextField(
                value = tripName,
                onValueChange = { tripName = it },
                label = { Text("Tên chuyến đi") },
                placeholder = { Text("VD: Trải nghiệm thanh xuân") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, tint = LagoonTeal) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = LagoonTeal
                )
            )
            
            // Destination Input
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Điểm đến (Thành phố/Quốc gia)") },
                placeholder = { Text("VD: Đà Lạt, Nha Trang") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = LagoonTeal) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = LagoonTeal
                )
            )

            // Date Selection
            Text(
                text = "Thời gian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // START DATE FIELD
                val startInteractionSource = remember { MutableInteractionSource() }
                if (startInteractionSource.collectIsPressedAsState().value) {
                    showStartDatePicker = true
                }
                
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { },
                    label = { Text("Ngày đi") },
                    modifier = Modifier.weight(1f),
                    readOnly = true, // Prevent keyboard from showing
                    interactionSource = startInteractionSource,
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = LagoonTeal) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedBorderColor = LagoonTeal
                    )
                )
                
                // END DATE FIELD
                val endInteractionSource = remember { MutableInteractionSource() }
                if (endInteractionSource.collectIsPressedAsState().value) {
                    showEndDatePicker = true
                }
                
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { },
                    label = { Text("Ngày về") },
                    modifier = Modifier.weight(1f),
                    readOnly = true, // Prevent keyboard from showing
                    interactionSource = endInteractionSource,
                    trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = LagoonTeal) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedBorderColor = LagoonTeal
                    )
                )
            }
            
            OutlinedTextField(
                value = days,
                onValueChange = { },
                label = { Text("Tổng số ngày") },
                placeholder = { Text("Tự động tính") },
                readOnly = true, // Auto-calculated
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = LagoonTeal) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = LagoonTeal
                )
            )
            
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Đang tải & Tính toán ngân sách AI...", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            
            uiState.error?.let {
                Text(text = "Lỗi: $it", color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(64.dp)) // Spacing for FAB
        }
        
        // --- DATE PICKER DIALOGS ---
        if (showStartDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            startDate = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            calculateDays(startDate, endDate)
                        }
                        showStartDatePicker = false
                        focusManager.clearFocus()
                    }) {
                        Text("Chọn")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showStartDatePicker = false
                        focusManager.clearFocus()
                    }) {
                        Text("Hủy")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showEndDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            endDate = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            calculateDays(startDate, endDate)
                        }
                        showEndDatePicker = false
                        focusManager.clearFocus()
                    }) {
                        Text("Chọn")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showEndDatePicker = false 
                        focusManager.clearFocus()
                    }) {
                        Text("Hủy")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
