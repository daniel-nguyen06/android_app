package android_app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import java.util.Locale

private data class BudgetEstimateDto(
    val transportCost: Long = 0,
    val accommodationCost: Long = 0,
    val foodCost: Long = 0,
    val entertainmentCost: Long = 0,
    val summaryAdvice: String = ""
)

class GeminiBudgetService(
    geminiApiKey: String,
    modelName: String = BuildConfig.GEMINI_MODEL
) {
    private val gson = Gson()
    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = geminiApiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.2f
        }
    )

    suspend fun estimateBudget(
        destination: String,
        days: Int,
        stopNames: List<String>,
        peopleCount: Int = 1
    ): BudgetBreakdown {
        val stopsDescription = if (stopNames.isNotEmpty()) stopNames.joinToString(", ")
        else "Khám phá tự do quanh khu vực"

        val promptText = """
            Bạn là trợ lý tư vấn tài chính du lịch tại Việt Nam.
            Ước tính ngân sách thực tế bằng VNĐ cho chuyến đi:
            - Điểm đến: $destination
            - Thời gian: $days ngày
            - Số người: $peopleCount người (tính tổng cho cả nhóm)
            - Các địa điểm dự kiến ghé: $stopsDescription

            Chỉ trả về JSON. Mọi số tiền là SỐ NGUYÊN theo VNĐ, không có dấu chấm, dấu phẩy hay chữ:
            {"transportCost": 0, "accommodationCost": 0, "foodCost": 0, "entertainmentCost": 0, "summaryAdvice": "1-2 câu lời khuyên tiết kiệm"}
        """.trimIndent()

        try {
            val raw = generativeModel.generateContent(promptText).text.orEmpty()
            val clean = raw.replace("```json", "").replace("```", "").trim()
            val dto = gson.fromJson(clean, BudgetEstimateDto::class.java)
                ?: throw IllegalStateException("Gemini trả về dữ liệu rỗng")

            val total = dto.transportCost + dto.accommodationCost + dto.foodCost + dto.entertainmentCost
            return BudgetBreakdown(
                transportCost = formatVnd(dto.transportCost),
                accommodationCost = formatVnd(dto.accommodationCost),
                foodCost = formatVnd(dto.foodCost),
                entertainmentCost = formatVnd(dto.entertainmentCost),
                totalEstimated = formatVnd(total),
                summaryAdvice = dto.summaryAdvice
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println(">>> [Gemini Budget lỗi] ${e::class.simpleName}: ${e.message}")
            throw IllegalStateException("Không tính được ngân sách: ${e.message}")
        }
    }

    private fun formatVnd(amount: Long): String =
        String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ"
}