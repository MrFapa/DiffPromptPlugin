package com.github.mrfapa.diffprompt.data

data class CompletionTokensDetails(
    val reasoning_tokens: Int,
    val audio_tokens: Int,
    val accepted_prediction_tokens: Int,
    val rejected_prediction_tokens: Int
)