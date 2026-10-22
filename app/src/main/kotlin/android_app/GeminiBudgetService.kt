package android_app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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

    private companion object {
        const val MAX_ATTEMPTS = 4
    }

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

        var lastError: Exception? = null

        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                val raw = generativeModel.generateContent(promptText).text.orEmpty()
                val clean = raw.replace("```json", "").replace("```", "").trim()
                val dto = gson.fromJson(clean, BudgetEstimateDto::class.java)
                    ?: throw IllegalStateException("Gemini trả về dữ liệu rỗng")

                val total = dto.transportCost + dto.accommodationCost +
                        dto.foodCost + dto.entertainmentCost
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
                lastError = e
                println(">>> [Gemini Budget] lần $attempt lỗi: ${e::class.simpleName}: ${e.message}")
                if (attempt < MAX_ATTEMPTS && isTransient(e)) {
                    delay(1500L * attempt) // 1.5s, 3s, 4.5s
                } else {
                    break
                }
            }
        }

        throw IllegalStateException(friendlyMessage(lastError))
    }

    /** Lỗi tạm thời của server (quá tải, hết hạn mức tức thời, mạng) nên đáng thử lại */
    private fun isTransient(e: Exception): Boolean {
        val msg = (e.message.orEmpty() + " " + e.cause?.message.orEmpty()).lowercase()
        return "503" in msg || "unavailable" in msg || "high demand" in msg ||
                "overloaded" in msg || "timeout" in msg || "timed out" in msg ||
                "unable to resolve host" in msg || "missingfieldexception" in msg
    }

    private fun friendlyMessage(e: Exception?): String {
        val msg = (e?.message.orEmpty() + " " + e?.cause?.message.orEmpty()).lowercase()
        return when {
            "503" in msg || "unavailable" in msg || "high demand" in msg || "missingfieldexception" in msg ->
                "Dịch vụ AI đang quá tải. Vui lòng thử lại sau ít phút."
            "429" in msg || "quota" in msg ->
                "Đã hết hạn mức gọi AI. Vui lòng thử lại sau."
            "unable to resolve host" in msg ->
                "Không có kết nối mạng."
            else -> "Không tính được ngân sách. Vui lòng thử lại."
        }
    }

    private fun formatVnd(amount: Long): String =
        String.format(Locale.US, "%,d", amount).replace(',', '.') + "đ"
}