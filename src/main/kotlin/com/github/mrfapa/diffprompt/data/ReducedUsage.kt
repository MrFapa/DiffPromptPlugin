package com.github.mrfapa.diffprompt.data

data class ReducedUsage(
    var totalTokens: Int,
    var promptTokens: Int,
    var completionTokens: Int,
    var cachedTokens: Int,
)