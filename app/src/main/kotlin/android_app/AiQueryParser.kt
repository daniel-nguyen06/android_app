package android_app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException

data class ParsedPlaceQuery(
    @SerializedName("categories") val categories: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("nearby") val nearby: Boolean = false,
    /** true nếu người dùng chỉ nhập tên một địa danh/thành phố, không kèm loại hình */
    @SerializedName("destinationOnly") val destinationOnly: Boolean = false
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
            Bạn giúp ứng dụng du lịch chuyển câu người dùng thành tham số tìm kiếm địa điểm.
            Câu người dùng: "$input"

            Trả về JSON với các trường:
            - categories: một hoặc nhiều danh mục, ngăn cách bằng dấu phẩy, CHỈ chọn từ danh sách: tourism.sights, beach, leisure.park, entertainment.culture, catering.restaurant, catering.cafe, catering.fast_food, catering.bar, accommodation.hotel, accommodation.hostel, commercial.shopping_mall, entertainment. Nếu chỉ nhập địa danh thì dùng "tourism.sights,beach,leisure.park,entertainment.culture".
            - name: tên thương hiệu/quán cụ thể nếu có, còn lại null. Không đưa loại hình chung (cà phê, nhà hàng) vào đây.
            - location: tên khu vực/địa danh nếu có (thành phố, quận, địa danh), còn lại null.
            - nearby: true nếu người dùng muốn tìm quanh vị trí của họ ("gần đây", "gần tôi", "near me"), ngược lại false.
            - destinationOnly: true nếu người dùng CHỈ nhập tên một địa danh/thành phố, không kèm loại hình hay thương hiệu; ngược lại false.

            Chỉ trả về JSON: {"categories": "...", "name": null, "location": null, "nearby": false, "destinationOnly": false}
        """.trimIndent()

        return try {
            val raw = generativeModel.generateContent(promptText).text.orEmpty()
            val clean = raw.replace("```json", "").replace("```", "").trim()
            val parsed = gson.fromJson(clean, ParsedPlaceQuery::class.java)
            if (parsed == null || parsed.categories.isNullOrBlank()) fallback(input) else parsed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println(">>> [Gemini lỗi] ${e::class.simpleName}: ${e.message}")
            fallback(input)
        }
    }

    // Gemini lỗi: coi nguyên văn là tên địa danh
    private fun fallback(input: String) = ParsedPlaceQuery(
        categories = "tourism.sights,beach,leisure.park,entertainment.culture",
        location = input,
        nearby = false,
        destinationOnly = true
    )
}