package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.data.ReducedUsage
import com.github.mrfapa.diffprompt.util.FileUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ResultsSaver {

    private val logger: Logger = LoggerFactory.getLogger(ResultsSaver::class.java)
    private val destinationRoot: String = "E:/Log/Eval"

    suspend fun saveResults(
        iteration: Int?,
        result: BugResult,
        chatContent: String,
        methodName: String,
        usage: ReducedUsage,
        filesToCopy: List<String>,
        processStreamOutput: List<String>,
        clock: Clock
    ) {
        val time = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(time))

        val baseDirectoryPath = if (iteration == null) {
            "$destinationRoot/$methodName-$formattedDate"
        } else {
            "$destinationRoot/$methodName/$iteration"
        }
        val chatFilePath = "$baseDirectoryPath/chat.txt"
        val resultFilePath = "$baseDirectoryPath/result-$result.txt"
        val usageFilePath = "$baseDirectoryPath/usage.txt"
        val timeMeasuresPath = "$baseDirectoryPath/time-measurements.txt"
        val processStreamFilePath = "$baseDirectoryPath/process-stream-output.txt"

        try {
            val baseDirectory = File(baseDirectoryPath)
            if (!baseDirectory.exists()) {
                baseDirectory.mkdirs()
            }
            copySourceFiles(baseDirectoryPath, filesToCopy)

            val resultFile = File(resultFilePath)
            resultFile.writeText(result.toString())

            val chatFile = File(chatFilePath)
            chatFile.writeText(chatContent)

            val usageFile = File(usageFilePath)
            usageFile.writeText(usage.toString())

            val timeMeasuresFile = File(timeMeasuresPath)
            timeMeasuresFile.writeText(clock.getMeasurements())

            val processStreamFile = File(processStreamFilePath)
            processStreamFile.writeText("Process Stream Output:\n\n")
            processStreamOutput.forEach { output ->
                processStreamFile.appendText("$output\n")
            }

            logger.info("Messages written to file successfully: ${chatFile.absolutePath}")
            logger.info("Usage written to file successfully: ${usageFile.absolutePath}")
            logger.info("Process stream output written to file successfully: ${processStreamFile.absolutePath}")
        } catch (e: IOException) {
            logger.error("An error occurred while writing to the file: ${e.message}", e)
        }
    }

    private fun copySourceFiles(destinationPath: String, filesToCopy: List<String>) {
        for (filePath in filesToCopy) {
            try {
                val originalFile = File(filePath)
                if (originalFile.exists()) {
                    val newFileName = originalFile.name
                    FileUtil.copyFileToNewDestination(filePath, newFileName, destinationPath)
                    logger.info("File copied successfully: $filePath to $destinationPath\\$newFileName")
                } else {
                    logger.warn("File not found: $filePath")
                }
            } catch (e: IOException) {
                logger.error("Error copying file $filePath: ${e.message}", e)
            }
        }
    }
}
