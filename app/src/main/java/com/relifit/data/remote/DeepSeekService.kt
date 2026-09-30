package com.relifit.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * 食物营养估算结果
 */
data class FoodNutritionEstimate(
    val name: String,
    val quantity: String,
    val kcal: Double,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double,
    val note: String = ""
)

/**
 * AI 生成的高蛋白控油盐菜谱结果
 */
data class GeneratedRecipe(
    val name: String,
    val category: String,
    val prepTimeMin: Int,
    val difficulty: String,
    val oilGram: Double,
    val saltGram: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val kcal: Double,
    val ingredients: String,
    val instructions: String
)

/**
 * DeepSeek AI 营养与菜谱服务
 * 纯原生 Android HTTPS 请求（零外部第三方库），直连 DeepSeek OpenAPI
 */
class DeepSeekService {

    companion object {
        private const val API_URL = "https://api.deepseek.com/chat/completions"
        const val DEFAULT_MODEL = "deepseek-chat"
        private const val TIMEOUT_MS = 30000
    }

    /**
     * 测试 API Key 是否有效
     */
    suspend fun testApiKey(apiKey: String, model: String = DEFAULT_MODEL): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("API Key 不能为空"))
            val response = callChatCompletions(
                apiKey = apiKey,
                systemPrompt = "你是一个助手，请只回复 OK 两个字母。",
                userMessage = "ping",
                expectJson = false,
                model = model
            )
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 针对任意食物名称/描述，调用 DeepSeek 估算营养热量与三大营养素
     */
    suspend fun estimateFoodNutrition(
        apiKey: String,
        query: String,
        model: String = DEFAULT_MODEL
    ): Result<FoodNutritionEstimate> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("请先配置 DeepSeek API Key"))
            if (query.isBlank()) return@withContext Result.failure(IllegalArgumentException("请输入食物名称"))

            val systemPrompt = """
                你是一位专业临床营养师与健身营养顾问。
                请针对用户输入的食物名称或描述（可能包含分量，例如“一碗牛肉面”、“200g香煎三文鱼”、“麦当劳双层吉士堡”），精准估算其对应的营养成分与推荐每份分量。
                请严格输出标准 JSON 格式，不要包含任何 markdown 标记或附加文本。
                JSON 字段说明：
                {
                  "name": "食物规范名称（如：红烧牛肉面）",
                  "quantity": "推荐每份分量说明（如：1 碗 (约400g) 或 1 份 (150g)）",
                  "kcal": 550.0,
                  "carbsG": 70.0,
                  "proteinG": 25.0,
                  "fatG": 18.0,
                  "note": "简短的一两句健康/控油盐小建议"
                }
            """.trimIndent()

            val content = callChatCompletions(apiKey, systemPrompt, query, expectJson = true, model = model)
            val json = JSONObject(extractJson(content))

            val estimate = FoodNutritionEstimate(
                name = json.optString("name", query.trim()),
                quantity = json.optString("quantity", "1 份"),
                kcal = json.optDouble("kcal", 0.0),
                carbsG = json.optDouble("carbsG", 0.0),
                proteinG = json.optDouble("proteinG", 0.0),
                fatG = json.optDouble("fatG", 0.0),
                note = json.optString("note", "")
            )
            Result.success(estimate)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 针对用户需求或菜品灵感，调用 DeepSeek 智能生成严控油盐的高蛋白健身菜谱
     */
    suspend fun generateRecipe(
        apiKey: String,
        prompt: String,
        model: String = DEFAULT_MODEL
    ): Result<GeneratedRecipe> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("请先配置 DeepSeek API Key"))
            if (prompt.isBlank()) return@withContext Result.failure(IllegalArgumentException("请输入菜谱名称或想法"))

            val systemPrompt = """
                你是一位专业健身营养主厨，精通低脂、低盐、高蛋白的科学健身饮食。
                请根据用户的需求或菜品想法，设计一份严格控油（油≤5g）、控盐（盐≤2g）、高蛋白的健身标准化菜谱。
                请严格输出标准 JSON 格式，不要包含任何 markdown 标记或附加文本。
                JSON 字段规范：
                {
                  "name": "菜品名称（如：黑椒芦笋炒牛肉粒）",
                  "category": "分类必须为以下四者之一：高蛋白增肌、极低脂减脂、优质脂肪、均衡轻食",
                  "prepTimeMin": 15,
                  "difficulty": "简单 或 中等 或 稍难",
                  "oilGram": 3.0,
                  "saltGram": 1.5,
                  "proteinG": 38.0,
                  "carbsG": 8.0,
                  "fatG": 5.0,
                  "kcal": 240.0,
                  "ingredients": "食材清单（换行罗列，必须明确标明油和盐的精确克数，如：牛里脊 200g\n芦笋 100g\n橄榄油 3g\n食用盐 1.5g\n黑胡椒碎 2g\n生抽 5ml）",
                  "instructions": "少油少盐健康烹饪步骤（换行标号，如：1. 牛肉切粒加生抽与黑胡椒腌制10分钟\n2. 不粘锅刷入3g橄榄油，中火翻炒牛肉粒至变色盛出\n3. 锅底余热下芦笋翻炒1分钟，加入牛肉粒混合\n4. 撒入1.5g食用盐翻炒均匀即可出锅）"
                }
            """.trimIndent()

            val content = callChatCompletions(apiKey, systemPrompt, prompt, expectJson = true, model = model)
            val json = JSONObject(extractJson(content))

            val recipe = GeneratedRecipe(
                name = json.optString("name", prompt.trim()),
                category = json.optString("category", "高蛋白增肌"),
                prepTimeMin = json.optInt("prepTimeMin", 15),
                difficulty = json.optString("difficulty", "简单"),
                oilGram = json.optDouble("oilGram", 3.0),
                saltGram = json.optDouble("saltGram", 1.5),
                proteinG = json.optDouble("proteinG", 30.0),
                carbsG = json.optDouble("carbsG", 10.0),
                fatG = json.optDouble("fatG", 5.0),
                kcal = json.optDouble("kcal", 200.0),
                ingredients = json.optString("ingredients", ""),
                instructions = json.optString("instructions", "")
            )
            Result.success(recipe)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun callChatCompletions(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        expectJson: Boolean,
        model: String = DEFAULT_MODEL
    ): String {
        val url = URL(API_URL)
        val conn = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val requestBody = JSONObject().apply {
            put("model", model.ifBlank { DEFAULT_MODEL })
            put("temperature", 0.3)
            if (expectJson) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userMessage))
            }
            put("messages", messages)
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val errText = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val errMsg = try {
                val errObj = JSONObject(errText)
                errObj.optJSONObject("error")?.optString("message") ?: errObj.optString("message", errText)
            } catch (_: Exception) {
                errText
            }
            throw RuntimeException("DeepSeek 调用失败 ($responseCode): ${errMsg.ifBlank { "网络或鉴权错误" }}")
        }

        val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(responseText)
        val choices = root.getJSONArray("choices")
        if (choices.length() == 0) throw RuntimeException("DeepSeek 未返回有效结果")
        val message = choices.getJSONObject(0).getJSONObject("message")
        return message.getString("content")
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
            return trimmed.substring(start, end + 1)
        }
        return trimmed
    }
}
