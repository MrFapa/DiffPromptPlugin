package com.github.mrfapa.diffprompt.data

import com.google.gson.annotations.SerializedName

data class ChatGPTResponse(
    val id: String,
    @SerializedName("object") val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage,
    val systemFingerprint: String
)