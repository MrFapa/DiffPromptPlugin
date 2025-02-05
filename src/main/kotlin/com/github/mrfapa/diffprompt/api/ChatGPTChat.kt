package com.github.mrfapa.diffprompt.api

import com.github.mrfapa.diffprompt.data.Message

class ChatGPTChat {

    private val model: String = "gpt-4o"
    private var messages: ArrayList<Message> = arrayListOf()


    fun addMessage(message: Message) {
        this.messages.add(message)
    }

    fun getMessages(): List<Message> {
        return messages
    }

    fun getLastMessage(): Message {
        return messages.last()
    }

    override fun toString(): String {
        var messageString = ""
        for (index in 0..messages.lastIndex) {
            messageString += "\n### Message $index: ### \n ${messages[index]}"
        }
        return "ChatGPTChat(model='$model', messages=$messageString)"
    }
}