package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.util.StringUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MyPluginTest : BasePlatformTestCase() {

    private val RESPONSE =
        "1. \n```java\nimport java.util.ArrayDeque;\nimport java.util.Deque;\nString greetUser(String name) {\n    StringBuilder message = new StringBuilder();\n    message.append(\"Hello \").append(name).append(\"!\");\n    return message.toString();\n}\n```\n\n2. \n```java\nString sayHello(String name) {\n    return String.format(\"Hello %s!\", name);\n}\n```"

    private val EXTRACTED_METHOD1 = """
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
    """.trimIndent()

    private val EXTRACTED_METHOD2 = """
        String sayHello(String name) {
            return String.format("Hello %s!", name);
        }
    """.trimIndent()

    private val TEST_CLASS = """
        public class TestClass {
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
        String sayHello(String name) {
            return String.format("Hello %s!", name);
        }
        }
    """.trimIndent()

    private val METHOD_LIST = listOf(EXTRACTED_METHOD1, EXTRACTED_METHOD2)

    fun testParsingOfReferences() {
        val result = StringUtils.parse(RESPONSE)
        for (i in METHOD_LIST.indices) {
            assertEquals(METHOD_LIST[i], result[i])
        }
    }
}
