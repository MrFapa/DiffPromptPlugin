package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.util.StringUtils
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StringUtilsTest : BasePlatformTestCase() {

    private val sampleCodeWithImports = """
        package com.example;
        import java.util.*;
        import java.util.ArrayDeque;
        import java.util.Deque;
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
    """.trimIndent()

    private val example_method = """
        
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
    """.trimIndent()

    private val sampleCodeWithoutImports = """
        package com.example;

        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
    """.trimIndent()

    fun testExtractImports() {
        val importList = listOf("import java.util.*;", "import java.util.ArrayDeque;", "import java.util.Deque;")

        val result = StringUtils.extractImportList(sampleCodeWithImports)

        assertEquals(importList, result)
    }

    fun testExtractPackage() {
        val pck = "package com.example;"

        val result = StringUtils.extractPackage(sampleCodeWithImports, true)

        assertEquals(pck, result)
    }

    fun testExtractMethodName() {
        val methodName = "greetUser"

        val result = StringUtils.extractMethodName(example_method)

        assertEquals(methodName, result)
    }

    fun testAddImportsToExistingImports() {
        val existingCode = """
        import java.util.ArrayDeque;
        import java.util.Deque;
        
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
        """.trimIndent()

        val expectedCode = """
        import java.util.ArrayDeque;
        import java.util.Deque;
        import java.util.*;
        import java.util.Set;
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
        """.trimIndent()

        val newImports = listOf("import java.util.*;", "import java.util.Set;")
        val result = StringUtils.addImports(newImports, existingCode)

        assertEquals(expectedCode, result)
    }

    fun testAddImportsWithoutExistingImports() {
        val newImports = listOf("import java.util.*;", "import java.util.Set;")

        val expectedCode = """
        package com.example;
        import java.util.*;
        import java.util.Set;
        String greetUser(String name) {
            StringBuilder message = new StringBuilder();
            message.append("Hello ").append(name).append("!");
            return message.toString();
        }
        """.trimIndent()

        val result = StringUtils.addImports(newImports, sampleCodeWithoutImports)

        assertEquals(expectedCode, result)
    }

    fun testAddImportsToEmptyCode() {
        val emptyCode = ""

        val newImports = listOf("import java.util.*;", "import java.util.Set;")
        val expectedCode = """
            import java.util.*;
            import java.util.Set;
        """.trimIndent()

        val result = StringUtils.addImports(newImports, emptyCode)

        assertEquals(expectedCode, result)
    }

    fun testExtractClassName() {
        val clazz = """
            public class TestClass {
                public static int testMethod(int n) {
                    return n * n;
                }
            }
        """.trimIndent()

        val extractedClassName = StringUtils.extractClassName(clazz)

        assertEquals("TestClass", extractedClassName)
    }
}
