package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.api.ChatGPTChat
import com.github.mrfapa.diffprompt.data.ChatGPTResponse
import com.github.mrfapa.diffprompt.data.Usage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatGPTCallback(private val chat: ChatGPTChat, private val callback: () -> Unit) : Callback<ChatGPTResponse> {

    private val logger: Logger = LoggerFactory.getLogger(ChatGPTCallback::class.java)
    private var response: ChatGPTResponse? = null

    override fun onResponse(call: Call<ChatGPTResponse>, response: Response<ChatGPTResponse>) {
        this.response = response.body()
        if (response.isSuccessful) {
            response.body()?.choices?.get(0)?.let { chat.addMessage(it.message) }
            callback()
        } else {
            logger.warn("Call to ChatGOT was not successfull!")
            logger.warn(response.errorBody().toString())
        }
    }

    override fun onFailure(call: Call<ChatGPTResponse>, t: Throwable) {
        t.printStackTrace()
    }

    fun getUsage() : Usage? {
        return response?.usage
    }
}