package android_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import android_app.ui.theme.TravelAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. CHUẨN BỊ API KEYS
        // Lưu ý: Đảm bảo key Gemini của bạn đã được cấu hình gọi tới model gemini-flash-latest
        val geminiKey = "AQ.Ab8RN6KN_pdWeE38ANxu9Os1B1JmyzoNWOwU104ckgUj9KEyOw"
        val geoapifyKey = "2578b45979c64af6a1c192e486b9181d"

        // 2. KHỞI TẠO CÁC SERVICE LÕI
        val aiParser = AiQueryParser(geminiApiKey = geminiKey)

        // (Khởi tạo sẵn BudgetService và TripRepository cho các màn hình quản lý chuyến đi sau này)
        val budgetService = GeminiBudgetService(geminiApiKey = geminiKey)
        val tripRepository = TripRepository(budgetService = budgetService)

        // 3. KHỞI TẠO KHO DỮ LIỆU ĐỊA ĐIỂM (Dùng Geoapify & AI thay vì Google Places cũ)
        val placeRepository = PlaceRepository(apiKey = geoapifyKey, aiParser = aiParser)

        // 4. KHỞI TẠO CẦU NỐI (ViewModel)
        val viewModel = PlaceViewModel(repository = placeRepository)

        // 5. GẮN VÀO GIAO DIỆN (UI)
        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Truyền ViewModel vào khung giao diện chính chứa các Navigation Route
                    MainTravelAppScreen(viewModel = viewModel)
                }
            }
        }
    }
}