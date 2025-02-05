package com.github.mrfapa.diffprompt.util

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileUtil {
    companion object {
        fun extractImportsFromFile(filePath: String): String {
            var imports = ""
            File(filePath).forEachLine { line ->
                if (line.startsWith("import")) {
                    imports += line + "\n"
                }
            }
            return imports
        }

        fun extractPackageFromFile(filePath: String): String {
            var pck = ""
            File(filePath).forEachLine { line ->
                if (line.startsWith("package")) {
                    pck = line
                }
            }
            return pck
        }

        fun saveToFile(filePath: String, content: String): File {
            File(filePath).bufferedWriter().use { out -> out.write(content) }
            return File(filePath)
        }

        fun readContent(filePath: String): String {
            return File(filePath).readText()
        }

        fun copyFileToNewDestination(originalFilePath: String, newFileName: String, destinationPath: String): File {
            val sourceFile = File(originalFilePath)
            val destinationDir = File(destinationPath)

            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val newFile = File(destinationDir, newFileName)
            Files.copy(sourceFile.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            return newFile
        }
    }
}