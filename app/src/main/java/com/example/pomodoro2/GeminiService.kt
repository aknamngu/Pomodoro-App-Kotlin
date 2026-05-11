package com.example.pomodoro2

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    // API Key này sẽ được lấy tự động từ local.properties thông qua BuildConfig
    // Kiều nhớ làm bước cấu hình build.gradle.kts tui chỉ ở dưới để không bị lỗi đỏ nhé!
    private val API_KEY = BuildConfig.GEMINI_API_KEY

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    /**
     * Hàm phân tích nhiệm vụ:
     * Nếu có mạng và Key đúng -> Trả về kết quả từ Gemini AI.
     * Nếu lỗi hoặc không có Key -> Trả về 4 bước mặc định (Dữ liệu cứng).
     */
    suspend fun analyzeTask(task: String, language: String): String = withContext(Dispatchers.IO) {
        // 1. Kiểm tra nếu Key trống thì dùng ngay dữ liệu dự phòng
        if (API_KEY.isBlank() || API_KEY == "YOUR_API_KEY_HERE") {
            return@withContext getDefaultSteps(language)
        }

        try {
            // 2. Chuẩn bị câu lệnh gửi cho AI
            val prompt = if (language == "vi") {
                "Tôi đang sử dụng phương pháp Pomodoro. Hãy chia nhỏ nhiệm vụ sau thành đúng 4 bước cực kỳ ngắn gọn, mỗi bước 1 dòng, đánh số 1, 2, 3, 4. Nhiệm vụ là: \"$task\""
            } else {
                "I am using the Pomodoro technique. Please break down the following task into exactly 4 concise steps, each on a new line, numbered 1, 2, 3, 4. Task: \"$task\""
            }

            // 3. Gọi Gemini AI
            val response = model.generateContent(content {
                text(prompt)
            })

            // 4. Trả về kết quả từ AI, nếu AI trả về rỗng thì dùng dữ liệu dự phòng
            response.text ?: getDefaultSteps(language)

        } catch (e: Exception) {
            // Nếu có lỗi (mất mạng, sai Key...), in ra lỗi và dùng dữ liệu dự phòng để App không bị văng
            e.printStackTrace()
            getDefaultSteps(language)
        }
    }

    // Hàm phụ để lấy dữ liệu cứng (Dự phòng)
    private fun getDefaultSteps(language: String): String {
        return if (language == "vi") {
            "1. Xác định mục tiêu\n2. Chuẩn bị công cụ\n3. Tập trung thực hiện\n4. Tổng kết kết quả"
        } else {
            "1. Define goal\n2. Prepare tools\n3. Focus on execution\n4. Summarize results"
        }
    }
}