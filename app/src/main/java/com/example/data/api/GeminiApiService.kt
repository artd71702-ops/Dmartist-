package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

// Request Models
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null,
    val tools: List<Tool>? = null
)

data class Tool(
    @Json(name = "googleSearch") val googleSearch: GoogleSearch? = null
)

data class GoogleSearch(
    val empty: String? = null
)

data class Content(
    val parts: List<Part>,
    val role: String? = null
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String
)

data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseMimeType: String? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: ImageConfig? = null
)

data class ImageConfig(
    val aspectRatio: String = "1:1",
    val imageSize: String = "1K"
)

// Response Models
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val error: ApiError? = null
)

// Imagen Request & Response Models
data class ImagenRequest(
    val instances: List<ImagenInstance>,
    val parameters: ImagenParameters = ImagenParameters()
)

data class ImagenInstance(
    val prompt: String
)

data class ImagenParameters(
    val sampleCount: Int = 1,
    val aspectRatio: String = "1:1",
    val outputMimeType: String = "image/jpeg"
)

data class ImagenResponse(
    val predictions: List<ImagenPrediction>? = null,
    val error: ApiError? = null
)

data class ImagenPrediction(
    val bytesBase64Encoded: String? = null,
    val mimeType: String? = null
)

data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null
)

data class ApiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/{model}:streamGenerateContent")
    @Streaming
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): ResponseBody

    @POST("v1beta/models/imagen-3.0-generate-002:predict")
    suspend fun generateImagenImage(
        @Query("key") apiKey: String,
        @Body request: ImagenRequest
    ): ImagenResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
