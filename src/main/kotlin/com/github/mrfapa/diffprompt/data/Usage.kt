package com.github.mrfapa.diffprompt.data

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int,
    val prompt_tokens_details: PromptTokensDetails,
    val completion_tokens_details: CompletionTokensDetails
)