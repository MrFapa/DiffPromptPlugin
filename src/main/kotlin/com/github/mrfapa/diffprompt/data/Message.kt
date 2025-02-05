package com.github.mrfapa.diffprompt.data

import com.github.mrfapa.diffprompt.RoleTypes

data class Message(
    var role: RoleTypes,
    var content: String,
    val refusal: Any? = null
)
