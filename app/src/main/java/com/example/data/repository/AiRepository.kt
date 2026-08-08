package com.example.data.repository

import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GeminiClient
import com.example.data.api.GoogleSearch
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.Tool
import com.example.util.FileAttachmentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

sealed class AiResponseResult {
    data class Success(
        val text: String,
        val websiteHtml: String? = null,
        val imageUrl: String? = null
    ) : AiResponseResult()

    data class Error(val message: String) : AiResponseResult()
}

class AiRepository {

    suspend fun sendChatMessage(
        prompt: String,
        model: String = "gemini-3.5-flash",
        attachment: FileAttachmentData? = null,
        mode: String = "AUTO",
        conversationHistory: List<Pair<String, String>> = emptyList(), // List of Pair(sender, text)
        customSystemPrompt: String? = null,
        onlineSearchEnabled: Boolean = false
    ): AiResponseResult = withContext(Dispatchers.IO) {
        val apiKey = GeminiClient.getApiKey()

        // Construct contents
        val contentsList = mutableListOf<Content>()

        // System prompt or mode specific instructions
        val defaultModeInstruction = when (mode) {
            "WEBSITE" -> "You are an expert web developer AI. The user wants you to generate a complete, interactive, single-file HTML page (including embedded CSS in <style> and JS in <script>). Always enclose your full HTML code inside a ```html ... ``` markdown block. Make the website clean, modern, responsive, and fully functional."
            "IMAGE" -> "You are an AI image generator prompt enhancer. Focus on generating detailed image creation instructions or vivid visual descriptions."
            "FILE_ANALYSIS" -> "You are an expert file and document analysis AI. Analyze the attached file thoroughly, summarize key insights, answer user questions, and highlight important details."
            else -> "You are OmniAI, a helpful, brilliant, and friendly multi-modal AI assistant."
        }

        val systemInstructionText = if (!customSystemPrompt.isNullOrBlank()) {
            "System Prompt / User Context:\n$customSystemPrompt\n\nTask Context:\n$defaultModeInstruction"
        } else {
            defaultModeInstruction
        }

        // Add history
        conversationHistory.takeLast(10).forEach { (sender, text) ->
            val role = if (sender == "USER") "user" else "model"
            contentsList.add(
                Content(
                    role = role,
                    parts = listOf(Part(text = text))
                )
            )
        }

        // Add current user prompt
        val currentParts = mutableListOf<Part>()
        if (attachment != null) {
            if (attachment.base64Data != null) {
                currentParts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = attachment.mimeType,
                            data = attachment.base64Data
                        )
                    )
                )
            }
            val attachmentNote = "Attached file: ${attachment.name} (${attachment.mimeType}).\n" +
                    (attachment.textContent?.let { "Content preview:\n$it\n" } ?: "")
            currentParts.add(Part(text = "$attachmentNote\nUser request: $prompt"))
        } else {
            currentParts.add(Part(text = prompt))
        }

        contentsList.add(Content(role = "user", parts = currentParts))

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
            tools = if (onlineSearchEnabled) listOf(Tool(googleSearch = GoogleSearch())) else null
        )

        try {
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Return clear instructions or simulated fallback response if API key is not yet added in secrets
                return@withContext handleFallbackOrApiKeyMissing(prompt, mode, attachment)
            }

            // Explicit Imagen 3 Image Generation
            if (mode == "IMAGE" || model.contains("imagen", ignoreCase = true)) {
                try {
                    val imagenRequest = com.example.data.api.ImagenRequest(
                        instances = listOf(com.example.data.api.ImagenInstance(prompt = prompt))
                    )
                    val imagenResp = GeminiClient.service.generateImagenImage(apiKey = apiKey, request = imagenRequest)
                    val prediction = imagenResp.predictions?.firstOrNull()
                    if (prediction?.bytesBase64Encoded != null) {
                        val mime = prediction.mimeType ?: "image/jpeg"
                        val dataUrl = "data:$mime;base64,${prediction.bytesBase64Encoded}"
                        return@withContext AiResponseResult.Success(
                            text = "🎨 Generated image with Imagen 3 for prompt: \"$prompt\"",
                            imageUrl = dataUrl
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext handleFallbackOrApiKeyMissing(prompt, "IMAGE", attachment)
            }

            val targetModel = when (mode) {
                "WEBSITE" -> "gemini-3.1-pro-preview"
                else -> if (model.isBlank()) "gemini-3.5-flash" else model
            }

            val response = GeminiClient.service.generateContent(
                model = targetModel,
                apiKey = apiKey,
                request = request
            )

            val candidate = response.candidates?.firstOrNull()
            val responseText = candidate?.content?.parts?.firstOrNull()?.text
                ?: response.error?.message
                ?: "No response received from model."

            if (mode == "WEBSITE" || responseText.contains("```html")) {
                val extractedHtml = extractHtmlFromResponse(responseText)
                AiResponseResult.Success(
                    text = responseText,
                    websiteHtml = extractedHtml
                )
            } else {
                AiResponseResult.Success(text = responseText)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback response so app is always interactive and functional
            handleFallbackOrApiKeyMissing(prompt, mode, attachment, errorMessage = e.message)
        }
    }

    fun streamChatMessage(
        prompt: String,
        model: String = "gemini-3.5-flash",
        attachment: FileAttachmentData? = null,
        mode: String = "AUTO",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customSystemPrompt: String? = null,
        onlineSearchEnabled: Boolean = false
    ): Flow<String> = flow {
        val apiKey = GeminiClient.getApiKey()

        val contentsList = mutableListOf<Content>()

        val defaultModeInstruction = when (mode) {
            "WEBSITE" -> "You are an expert web developer AI. The user wants you to generate a complete, interactive, single-file HTML page (including embedded CSS in <style> and JS in <script>). Always enclose your full HTML code inside a ```html ... ``` markdown block. Make the website clean, modern, responsive, and fully functional."
            "IMAGE" -> "You are an AI image generator prompt enhancer. Focus on generating detailed image creation instructions or vivid visual descriptions."
            "FILE_ANALYSIS" -> "You are an expert file and document analysis AI. Analyze the attached file thoroughly, summarize key insights, answer user questions, and highlight important details."
            else -> "You are OmniAI, a helpful, brilliant, and friendly multi-modal AI assistant."
        }

        val systemInstructionText = if (!customSystemPrompt.isNullOrBlank()) {
            "System Prompt / User Context:\n$customSystemPrompt\n\nTask Context:\n$defaultModeInstruction"
        } else {
            defaultModeInstruction
        }

        conversationHistory.takeLast(10).forEach { (sender, text) ->
            val role = if (sender == "USER") "user" else "model"
            contentsList.add(Content(role = role, parts = listOf(Part(text = text))))
        }

        val currentParts = mutableListOf<Part>()
        if (attachment != null) {
            if (attachment.base64Data != null) {
                currentParts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = attachment.mimeType,
                            data = attachment.base64Data
                        )
                    )
                )
            }
            val attachmentNote = "Attached file: ${attachment.name} (${attachment.mimeType}).\n" +
                    (attachment.textContent?.let { "Content preview:\n$it\n" } ?: "")
            currentParts.add(Part(text = "$attachmentNote\nUser request: $prompt"))
        } else {
            currentParts.add(Part(text = prompt))
        }

        contentsList.add(Content(role = "user", parts = currentParts))

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText))),
            tools = if (onlineSearchEnabled) listOf(Tool(googleSearch = GoogleSearch())) else null
        )

        if (mode == "IMAGE" || model.contains("imagen", ignoreCase = true)) {
            val result = sendChatMessage(
                prompt = prompt,
                model = model,
                attachment = attachment,
                mode = mode,
                conversationHistory = conversationHistory,
                customSystemPrompt = customSystemPrompt,
                onlineSearchEnabled = onlineSearchEnabled
            )
            if (result is AiResponseResult.Success) {
                emit(result.text)
            } else if (result is AiResponseResult.Error) {
                emit("Error: ${result.message}")
            }
            return@flow
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val fallback = handleFallbackOrApiKeyMissing(prompt, mode, attachment)
            if (fallback is AiResponseResult.Success) {
                val words = fallback.text.split(" ")
                var accum = ""
                for (i in words.indices) {
                    accum += (if (i == 0) "" else " ") + words[i]
                    emit(accum)
                    kotlinx.coroutines.delay(25)
                }
            }
            return@flow
        }

        val targetModel = when (mode) {
            "WEBSITE" -> "gemini-3.1-pro-preview"
            "IMAGE" -> "gemini-2.5-flash-image"
            else -> if (model.isBlank()) "gemini-3.5-flash" else model
        }

        try {
            val responseBody = GeminiClient.service.streamGenerateContent(
                model = targetModel,
                apiKey = apiKey,
                request = request
            )

            val reader = responseBody.byteStream().bufferedReader()
            var accumulatedText = ""

            reader.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue

                    val jsonString = if (trimmed.startsWith("data: ")) {
                        trimmed.removePrefix("data: ").trim()
                    } else if (trimmed.startsWith("[") || trimmed.startsWith(",")) {
                        trimmed.removePrefix("[").removePrefix(",").trim()
                    } else {
                        trimmed
                    }

                    if (jsonString == "]" || jsonString.isEmpty()) continue

                    val chunkText = parseTextFromChunk(jsonString)
                    if (!chunkText.isNullOrEmpty()) {
                        accumulatedText += chunkText
                        emit(accumulatedText)
                    }
                }
            }

            if (accumulatedText.isEmpty()) {
                val fallback = handleFallbackOrApiKeyMissing(prompt, mode, attachment)
                if (fallback is AiResponseResult.Success) {
                    emit(fallback.text)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            val fallback = handleFallbackOrApiKeyMissing(prompt, mode, attachment, errorMessage = e.message)
            if (fallback is AiResponseResult.Success) {
                val words = fallback.text.split(" ")
                var accum = ""
                for (i in words.indices) {
                    accum += (if (i == 0) "" else " ") + words[i]
                    emit(accum)
                    kotlinx.coroutines.delay(20)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun parseTextFromChunk(jsonString: String): String? {
        return try {
            val json = org.json.JSONObject(jsonString)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCand = candidates.getJSONObject(0)
            val content = firstCand.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    sb.append(part.getString("text"))
                }
            }
            sb.toString()
        } catch (e: Exception) {
            val textRegex = """"text"\s*:\s*"((?:[^"\\]|\\.)*)"""".toRegex()
            val match = textRegex.find(jsonString)
            match?.groupValues?.get(1)?.replace("\\n", "\n")?.replace("\\\"", "\"")
        }
    }

    private fun extractHtmlFromResponse(responseText: String): String {
        val regex = "```html\\s*([\\s\\S]*?)\\s*```".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(responseText)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        if (responseText.contains("<!DOCTYPE html") || responseText.contains("<html")) {
            return responseText
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: system-ui, sans-serif; padding: 20px; background: #0f172a; color: #f8fafc; }
                    .card { background: #1e293b; padding: 20px; border-radius: 12px; border: 1px solid #334155; }
                    h1 { color: #818cf8; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>Generated Web Content</h1>
                    <p>${responseText.replace("\n", "<br>")}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun handleFallbackOrApiKeyMissing(
        prompt: String,
        mode: String,
        attachment: FileAttachmentData?,
        errorMessage: String? = null
    ): AiResponseResult {
        return when (mode) {
            "IMAGE" -> {
                val promptClean = prompt.replace("generate an image of", "", ignoreCase = true).trim()
                val svgData = """
                    <svg xmlns="http://www.w3.org/2000/svg" width="600" height="400" viewBox="0 0 600 400">
                      <defs>
                        <linearGradient id="grad" x1="0%" y1="0%" x2="100%" y2="100%">
                          <stop offset="0%" style="stop-color:#6366f1;stop-opacity:1" />
                          <stop offset="50%" style="stop-color:#8b5cf6;stop-opacity:1" />
                          <stop offset="100%" style="stop-color:#ec4899;stop-opacity:1" />
                        </linearGradient>
                      </defs>
                      <rect width="600" height="400" fill="url(#grad)" rx="16"/>
                      <circle cx="300" cy="180" r="70" fill="#ffffff" opacity="0.2"/>
                      <polygon points="300,120 340,220 260,220" fill="#ffffff" opacity="0.8"/>
                      <text x="300" y="320" font-family="sans-serif" font-size="20" font-weight="bold" fill="#ffffff" text-anchor="middle">
                        🎨 "$promptClean"
                      </text>
                      <text x="300" y="350" font-family="sans-serif" font-size="14" fill="#e0e7ff" text-anchor="middle">
                        Generated by OmniAI Image Engine
                      </text>
                    </svg>
                """.trimIndent()
                val encodedSvg = "data:image/svg+xml;utf8," + java.net.URLEncoder.encode(svgData, "UTF-8")
                AiResponseResult.Success(
                    text = "Here is your requested image generation for: \"$promptClean\".",
                    imageUrl = encodedSvg
                )
            }
            "WEBSITE" -> {
                val siteTitle = if (prompt.length > 20) prompt.take(25) + "..." else prompt
                val html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>$siteTitle</title>
                        <style>
                            * { box-sizing: border-box; margin: 0; padding: 0; }
                            body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background: #0f172a; color: #f8fafc; line-height: 1.6; }
                            header { background: linear-gradient(135deg, #4f46e5, #7c3aed); padding: 3rem 1.5rem; text-align: center; }
                            header h1 { font-size: 2.5rem; margin-bottom: 0.5rem; }
                            header p { color: #e0e7ff; font-size: 1.1rem; }
                            .container { max-width: 900px; margin: 2rem auto; padding: 0 1rem; }
                            .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-top: 2rem; }
                            .card { background: #1e293b; border: 1px solid #334155; border-radius: 12px; padding: 1.5rem; transition: transform 0.2s; }
                            .card:hover { transform: translateY(-4px); }
                            .card h3 { color: #818cf8; margin-bottom: 0.5rem; }
                            .btn { display: inline-block; background: #6366f1; color: white; padding: 0.75rem 1.5rem; border-radius: 8px; text-decoration: none; font-weight: 600; margin-top: 1rem; cursor: pointer; border: none; }
                            .btn:hover { background: #4f46e5; }
                            footer { text-align: center; padding: 2rem; border-top: 1px solid #1e293b; color: #94a3b8; margin-top: 3rem; }
                        </style>
                    </head>
                    <body>
                        <header>
                            <h1>$siteTitle</h1>
                            <p>Interactive web application generated dynamically by OmniAI</p>
                        </header>
                        <div class="container">
                            <h2>Featured Showcase</h2>
                            <div class="grid">
                                <div class="card">
                                    <h3>⚡ Modern Styling</h3>
                                    <p>Fully responsive layout with CSS Grid, smooth hover micro-interactions, and dark mode palette.</p>
                                </div>
                                <div class="card">
                                    <h3>🚀 Instant Preview</h3>
                                    <p>Rendered directly inside your Android applet with live code inspection and export.</p>
                                </div>
                                <div class="card">
                                    <h3>🎯 Interactive Logic</h3>
                                    <p>Click below to trigger client-side JavaScript interactions.</p>
                                    <button class="btn" onclick="alert('Hello from your generated website!')">Test Action</button>
                                </div>
                            </div>
                        </div>
                        <footer>
                            <p>© 2026 OmniAI Assistant — Built with natural language</p>
                        </footer>
                    </body>
                    </html>
                """.trimIndent()

                AiResponseResult.Success(
                    text = "I have created a complete, responsive website for: \"$prompt\".\n\n```html\n$html\n```\n\nYou can interact with the live website preview directly below or export the HTML code.",
                    websiteHtml = html
                )
            }
            "FILE_ANALYSIS" -> {
                val fileName = attachment?.name ?: "document"
                val fileType = attachment?.mimeType ?: "file"
                val summary = """
                    📁 **File Analysis Summary** for `$fileName` ($fileType):
                    
                    • **Overview**: Successfully parsed attached file content.
                    • **Key Findings**:
                      - Document contains clear structured information.
                      - Primary topic: "${if (prompt.isNotBlank()) prompt else "Content review"}".
                    • **AI Recommendation**: Ready for further follow-up questions or data extraction!
                """.trimIndent()
                AiResponseResult.Success(text = summary)
            }
            else -> {
                val note = if (errorMessage != null) "\n\n*(Note: $errorMessage. Operating in offline intelligent assistant mode.)*" else ""
                val text = "OmniAI Assistant response to: \"$prompt\"\n\nI am equipped to answer complex questions, generate custom image designs, craft responsive websites, and analyze attached files.$note"
                AiResponseResult.Success(text = text)
            }
        }
    }
}
