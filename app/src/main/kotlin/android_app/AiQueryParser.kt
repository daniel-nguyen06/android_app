package android_app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException

data class ParsedPlaceQuery(
    /** Câu tìm kiếm tự nhiên để gửi thẳng cho Google Maps */
    @SerializedName("searchQuery") val searchQuery: String? = null,
    /** true nếu người dùng muốn tìm quanh vị trí hiện tại của họ */
    @SerializedName("nearby") val nearby: Boolean = false
)

class AiQueryParser(
    geminiApiKey: String,
    modelName: String = BuildConfig.GEMINI_MODEL
) {
    private val gson = Gson()

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = geminiApiKey,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0f
        }
    )

    suspend fun parseUserPrompt(userInput: String): ParsedPlaceQuery {
        val input = userInput.trim()
        if (input.isEmpty()) return ParsedPlaceQuery()

        val promptText = """
            Bạn giúp một ứng dụng du lịch chuyển câu người dùng nhập thành câu tìm kiếm cho Google Maps.
            Câu người dùng: "$input"

            Quy tắc:
            - searchQuery: câu tìm kiếm ngắn gọn, tự nhiên, giữ nguyên ngôn ngữ của người dùng. Giữ tên thương hiệu, loại hình (quán cà phê, nhà hàng, khách sạn, điểm tham quan...) và địa danh nếu có. Bỏ các từ thừa như "tìm", "gần đây", "cho tôi". Nếu người dùng chỉ nhập một địa danh thì tìm các điểm tham quan ở đó.
            - nearby: true nếu người dùng muốn tìm quanh vị trí hiện tại của họ (ví dụ "gần đây", "gần tôi", "near me"), ngược lại false.

            Chỉ trả về JSON: {"searchQuery": "...", "nearby": true hoặc false}
        """.trimIndent()

        return try {
            val raw = generativeModel.generateContent(promptText).text.orEmpty()
            val cleanJson = raw.replace("```json", "").replace("```", "").trim()
            val parsed = gson.fromJson(cleanJson, ParsedPlaceQuery::class.java)
            if (parsed?.searchQuery.isNullOrBlank()) fallback(input) else parsed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println(">>> [Gemini lỗi] ${e::class.simpleName}: ${e.message}")
            fallback(input)
        }
    }

    // Gemini lỗi thì dùng nguyên văn người dùng nhập, không tự đoán từ khóa
    private fun fallback(input: String) = ParsedPlaceQuery(searchQuery = input, nearby = false)
}