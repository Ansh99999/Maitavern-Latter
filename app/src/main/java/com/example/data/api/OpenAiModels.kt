package com.example.data.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float? = null,
    val max_tokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val message: OpenAiMessage? = null,
    val finish_reason: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiResponse(
    val choices: List<OpenAiChoice>? = null
)
