package com.github.mrfapa.diffprompt.api

import com.github.mrfapa.diffprompt.data.ChatGPTResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


interface ChatGPTService {
    @Headers(
        "Content-Type: application/json"
    )
    @POST("completions")
    suspend fun prompt(@Body chatGPTChat: ChatGPTChat): ChatGPTResponse
}