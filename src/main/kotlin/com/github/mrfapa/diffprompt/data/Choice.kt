package com.github.mrfapa.diffprompt.data

data class Choice(
    val index: Int,
    val message: Message,
    val logprobs: Any?, // Use Any? for nullable fields where type isn't specified (could also be more specific if known)
    val finishReason: String
)