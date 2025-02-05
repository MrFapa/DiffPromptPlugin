package com.github.mrfapa.diffprompt

import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

class ReferenceManager {

    private val logger: Logger = LoggerFactory.getLogger(ReferenceManager::class.java)

    suspend fun buildCompileProcessBuilder(filePath: String): ProcessBuilder {
        return ProcessBuilder("javac", "-cp", "E:/Downloads/QuixBugs-master/QuixBugs-master", filePath)
    }

    suspend fun buildRunProcessBuilder(runnableClass: String): ProcessBuilder {
        return ProcessBuilder("java", "-cp", "E:/Downloads/QuixBugs-master/QuixBugs-master", runnableClass)
    }

    suspend fun executeProcess(processBuilder: ProcessBuilder, timeoutSeconds: Long): ProcessOutput {
        logger.info("Starting process with a timeout of $timeoutSeconds seconds...")

        return withContext(Dispatchers.IO) {
            val process = processBuilder.start()
            try {
                val exitCode = withTimeout(timeoutSeconds * 1000L) {
                    process.awaitExit()
                }
                val outputStream = process.inputStream.bufferedReader().readText()
                val errorStream = process.errorStream.bufferedReader().readText()

                if (exitCode == 0) {
                    ProcessOutput(exitCode, outputStream)
                } else {
                    ProcessOutput(exitCode, errorStream)
                }
            } catch (e: TimeoutException) {
                logger.error("Process timed out after $timeoutSeconds seconds!")
                ProcessOutput(-1, "Timeout Error ($timeoutSeconds seconds): ${e.message}")
            } catch (e: Exception) {
                logger.error("An error occurred while executing the process: ${e.message}")
                ProcessOutput(-2, "Execution Error: ${e.message}")
            } finally {
                process.destroy()
            }
        }
    }

}
