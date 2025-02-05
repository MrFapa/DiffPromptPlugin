package com.github.mrfapa.diffprompt.api

import com.github.mrfapa.diffprompt.data.ChatGPTResponse
import retrofit2.Retrofit

class ApiWrapper private constructor(
    private val retrofit: Retrofit
) {
    val service: ChatGPTService = retrofit.create(ChatGPTService::class.java)

    companion object {
        fun create(): ApiWrapper {
            val retrofit = ApiClientFactory.createClient("https://api.openai.com/v1/chat/")
            return ApiWrapper(retrofit)
        }
    }

    suspend fun prompt(chatGPTChat: ChatGPTChat): ChatGPTResponse {
        return service.prompt(chatGPTChat)
    }
}
