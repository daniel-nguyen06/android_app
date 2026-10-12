package android_app

import kotlinx.coroutines.runBlocking

// 1. Dùng cú pháp chuẩn fun main(args: Array<String>) { ... }
fun main(args: Array<String>) {
    // 2. Đặt runBlocking vào bên trong ngoặc nhọn
    runBlocking {
        println("==================================================")
        println(">>> TEST LUỒNG TẠO CHUYẾN ĐI & TRACKING CHI TIÊU")
        println("==================================================")

        val geminiKey = "AQ.Ab8RN6KN_pdWeE38ANxu9Os1B1JmyzoNWOwU104ckgUj9KEyOw"

        val budgetService = GeminiBudgetService(geminiApiKey = geminiKey)
        val tripRepo = TripRepository(budgetService = budgetService)

        // 1. TẠO CHUYẾN ĐI
        println("\n[1] Đang tạo chuyến đi và chờ AI tính ngân sách...")
        val myTrip = tripRepo.createTrip(
            title = "Đà Lạt 4 ngày 3 đêm",
            destination = "Đà Lạt",
            startDate = "2026-12-20",
            endDate = "2026-12-23",
            days = 4,
            peopleCount = 2
        )

        println("=> Tên chuyến: ${myTrip.title}")
        println("=> AI dự kiến tổng ngân sách: ${myTrip.budget?.totalEstimated}")
        println("--------------------------------------------------")

        // 2. NHẬP CHI TIÊU THỰC TẾ & NHẬN CẢNH BÁO
        println("\n[2] BẮT ĐẦU ĐI CHƠI VÀ GHI CHÉP CHI TIÊU:\n")

        // Chi tiêu 1: Vé máy bay
        println("Khoản 1: Mua vé máy bay khứ hồi (2.500.000đ)")
        val status1 = tripRepo.addExpenseAndCheckBudget(myTrip.tripId, "Vé máy bay", 2_500_000L, ExpenseCategory.TRANSPORT)
        println("  -> Tổng đã tiêu: ${status1?.totalSpent}đ | Còn lại: ${status1?.remainingBalance}đ")
        status1?.warningMessage?.let { println("  -> $it") }

        // Chi tiêu 2: Khách sạn
        println("\nKhoản 2: Thanh toán tiền phòng khách sạn (1.500.000đ)")
        val status2 = tripRepo.addExpenseAndCheckBudget(myTrip.tripId, "Khách sạn", 1_500_000L, ExpenseCategory.ACCOMMODATION)
        println("  -> Tổng đã tiêu: ${status2?.totalSpent}đ | Còn lại: ${status2?.remainingBalance}đ")
        status2?.warningMessage?.let { println("  -> $it") }

        // Chi tiêu 3: Ăn nhà hàng
        println("\nKhoản 3: Ăn nhà hàng Leguda (800.000đ)")
        val status3 = tripRepo.addExpenseAndCheckBudget(myTrip.tripId, "Ăn tối", 800_000L, ExpenseCategory.FOOD)
        println("  -> Tổng đã tiêu: ${status3?.totalSpent}đ | Còn lại: ${status3?.remainingBalance}đ")
        status3?.warningMessage?.let { println("  -> $it") }

        // In tổng kết
        val finalTrip = tripRepo.getTripDetails(myTrip.tripId)
        println("\n================ BẢNG KÊ CHI TIẾT TỔNG KẾT ================")
        finalTrip?.expenses?.forEachIndexed { index, expense ->
            println("${index + 1}. [${expense.category}] ${expense.title}: ${expense.amount}đ")
        }
    }
}