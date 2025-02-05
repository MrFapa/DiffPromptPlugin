package com.github.mrfapa.diffprompt.util

class StringUtils {

    companion object {
        private const val SEPERATOR = "```";

        fun extractClassName(code: String): String? {
            val classRegex = Regex("public class\\s+([\\w.]+)")
            val matchResult = classRegex.find(code)
            return matchResult?.groupValues?.getOrNull(1)
        }

        fun extractMethodName(methodText: String): String? {
            val methodRegex = Regex("""\b\w+\s*\(""")
            val match = methodRegex.find(methodText)
            return match?.value?.removeSuffix("(")?.trim()
        }

        fun extractPackage(code: String, full: Boolean): String? {
            val packageRegex = Regex("package\\s+([\\w.*]+);")
            val matchResult = packageRegex.find(code)
            return if (full) matchResult?.value else matchResult?.groupValues?.getOrNull(1)
        }


        fun parse(referenceString: String): List<String> {
            val references = mutableListOf<String>()
            val parts = referenceString.split(SEPERATOR)
            for (i in parts.indices) {
                if (i % 2 != 0) {
                    val method = if (parts[i].startsWith("java")) {
                        parts[i].substring(4)
                    } else {
                        parts[i]
                    }
                    references.add(method.trim())
                }
            }

            return references
        }

        fun extractImportList(code: String): List<String> {
            val imports = mutableListOf<String>()
            val importRegex = Regex("import\\s+[\\w.*]+;")
            // For each import
            importRegex.findAll(code).forEach { matchResult ->
                imports.add(matchResult.groupValues[0])
            }
            return imports
        }

        fun addImports(imports: List<String>, code: String): String {
            // After last import
            val importRegex = Regex("^import\\s+[\\w.]+;\\s*$", RegexOption.MULTILINE)
            val newImports = imports.joinToString(separator = "\n") { it }

            val existingImports = importRegex.findAll(code).toList()

            return if (existingImports.isNotEmpty()) {
                val lastImportIndex = existingImports.last().range.last
                code.substring(0, lastImportIndex + 1) + newImports + code.substring(lastImportIndex + 1)
            } else {
                val packageRegex = Regex("package\\s+[\\w.*]+;")
                val pck = packageRegex.findAll(code).toList()

                code.substring(0, pck.last().range.last + 1) + newImports + code.substring(pck.last().range.last + 1)
            }
        }

        fun addImport(import: String, code: String): String {
            // After last import
            val importRegex = Regex("^import\\s+[\\w.]+;\\s*$", RegexOption.MULTILINE)

            val existingImports = importRegex.findAll(code).toList()

            return if (existingImports.isNotEmpty()) {
                val lastImportIndex = existingImports.last().range.last
                code.substring(0, lastImportIndex + 1) + import + code.substring(lastImportIndex + 1)
            } else {
                val packageRegex = Regex("package\\s+[\\w.*]+;")
                val pck = packageRegex.findAll(code).toList()

                code.substring(0, pck.last().range.last + 1) + import + code.substring(pck.last().range.last + 1)
            }
        }

        fun remove(list: List<String>, code: String): String {
            var cleaned = code
            for (i in list.indices) {
                cleaned = cleaned.replace(list[i], "")
            }
            return cleaned
        }
    }
}