package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.api.ApiWrapper
import com.github.mrfapa.diffprompt.api.ChatGPTChat
import com.github.mrfapa.diffprompt.data.Message
import com.github.mrfapa.diffprompt.data.ReducedUsage
import com.github.mrfapa.diffprompt.data.Usage
import com.github.mrfapa.diffprompt.util.FileUtil
import com.github.mrfapa.diffprompt.util.MyBundle
import com.github.mrfapa.diffprompt.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class BugPrompter {

    private var chat: ChatGPTChat = ChatGPTChat()
    private val apiWrapper: ApiWrapper = ApiWrapper.create()
    private val logger: Logger = LoggerFactory.getLogger(BugPrompter::class.java)
    private var bugContext = BugContext()

    private var filesToTrack = mutableListOf<String>()
    private var processStreamOutput = mutableListOf<String>()
    private var iteration: Int? = null
    private var result: BugResult = BugResult.ERROR
    private var clock: Clock = Clock()

    suspend fun testForBug(put: String, filePath: String, iteration: Int?) {
        clock.start("all")
        this.chat = ChatGPTChat()
        initializeBugContext(put, filePath)
        filesToTrack.add(filePath)
        this.iteration = iteration
        logger.info("Starting to prompt ChatGPT")
        try {
            promptIntention()
        } catch (e: Exception) {
            saveResults()
        }
    }

    private fun initializeBugContext(put: String, filePath: String) {
        val methodName: String = StringUtils.extractMethodName(put).toString()
        bugContext = BugContext(
            put = put,
            filePath = filePath,
            imports = FileUtil.extractImportsFromFile(filePath),
            putClassContent = FileUtil.readContent(filePath),
            className = FileUtil.readContent(filePath)
                .let { StringUtils.extractClassName(it) }
                .orEmpty(),
            methodName = methodName,
            fullPackageName = FileUtil.readContent(filePath)
                .let { StringUtils.extractPackage(it, true) }
                .orEmpty(),
        )
    }


    private suspend fun promptIntention() {
        clock.start("intention")
        try {
            addUserMessage(MyBundle.message("prompts.intention", bugContext.put, bugContext.imports))
            logger.info("Step 1: Asking ChatGPT for the Intention of the given PUT")

            clock.start("intentionPrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("intentionPrompt")
            updateUsage(response.usage)
            chat.addMessage(response.choices[0].message)

            clock.stop("intention")
            promptReferences()
        } catch (e: Exception) {
            logger.error("Error during intention prompt", e)
            clock.stop("intention")
            abortAnalysis()
        }
    }

    private suspend fun promptReferences() {
        clock.start("references")
        try {
            addUserMessage(MyBundle.message("prompts.references"))
            logger.info("Step 2: Asking ChatGPT to generate references to the given PUT")
            clock.start("referencePrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("referencePrompt")
            updateUsage(response.usage)
            val lastMassage = response.choices[0].message
            chat.addMessage(lastMassage)
            bugContext.referenceList = StringUtils.parse(lastMassage.content)

            var cancel = false
            cancel = cancel || compileReference(bugContext.referenceList[0], "1")
            cancel = cancel || compileReference(bugContext.referenceList[1], "2")

            logger.info("Compilation of references finished.")
            clock.stop("references")
            if (cancel) {
                saveResults()
                cleanUp()
            } else {
                promptDiverseInput()
            }
        } catch (e: Exception) {
            logger.error("Error during reference generation", e)
            clock.stop("references")
            abortAnalysis()
        }
    }

    private suspend fun compileReference(referenceMethod: String, version: String): Boolean {
        var successful = false
        val referenceManager = ReferenceManager()
        val pathRef = createReferenceFiles(version, referenceMethod)
        filesToTrack.add(pathRef)
        val processOutput = referenceManager.executeProcess(
            referenceManager.buildCompileProcessBuilder(pathRef),
            60
        )
        processStreamOutput.add("Reference $version: ${processOutput.output}")
        if (processOutput.errorCode == 0
        ) {
            filesToTrack.add(getCompiledFilePath(pathRef))
        } else {
            logger.error("Compilation of reference $version failed. Cancel further analysis.")
            successful = true
        }

        return successful
    }


    private suspend fun promptDiverseInput() {
        clock.start("testInput")
        try {
            addUserMessage(MyBundle.message("prompts.diverseInput"))
            logger.info("Step 3: Asking ChatGPT to generate diverse Test Input")
            clock.start("testInputPrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("testInputPrompt")
            updateUsage(response.usage)
            chat.addMessage(response.choices[0].message)

            clock.stop("testInput")
            promptTestReferences()
        } catch (e: Exception) {
            logger.error("Error during prompting of diverse input", e)
            clock.stop("testInput")
            abortAnalysis()
        }
    }

    private suspend fun promptTestReferences() {
        clock.start("testReferences")
        try {
            addUserMessage(MyBundle.message("prompts.testReferences", bugContext.className))
            logger.info("Step 4: Asking ChatGPT to generate a test class that tests both references")
            clock.start("testReferencesPrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("testReferencesPrompt")
            updateUsage(response.usage)
            chat.addMessage(response.choices[0].message)
            val classPreperation = prepareTestClass("ReferenceTestClass")
            val compiled = compileTestClass(classPreperation.first)

            if (compiled) {
                val executionResult = executeTestClass(classPreperation.first, classPreperation.second)
                if (executionResult.errorCode == 0) {
                    logger.info("The reference test class executed without errors")
                    clock.stop("testReferences")
                    promptTestPUT(executionResult.output)
                } else {
                    logger.info("The reference test class executed with errors. Cancel bug detection..")
                    clock.stop("testReferences")
                    abortAnalysis()
                }
            } else {
                clock.stop("testReferences")
                abortAnalysis()
            }
        } catch (e: Exception) {
            logger.error("Error during testReferences", e)
            clock.stop("testReferences")
            abortAnalysis()
        }
    }

    private suspend fun promptTestPUT(testOutput: String) {
        clock.start("testPUT")
        try {
            addUserMessage(MyBundle.message("prompts.testPut", testOutput, bugContext.className, bugContext.className))
            logger.info("Step 5: Asking ChatGPT to generate test class for PUT")
            clock.start("testPUTPrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("testPUTPrompt")
            updateUsage(response.usage)
            chat.addMessage(response.choices[0].message)
            clock.stop("testPUT")
            promptVerify()
        } catch (e: Exception) {
            logger.error("Error during test generation", e)
            clock.stop("testPUT")
            abortAnalysis()
        }
    }

    private suspend fun promptVerify() {
        clock.start("verify")
        try {
            addUserMessage(MyBundle.message("prompt.verify"))
            logger.info("Step 6: Asking ChatGPT to verify the test class")
            clock.start("verifyPrompt")
            val response = apiWrapper.prompt(chat)
            clock.stop("verifyPrompt")
            updateUsage(response.usage)
            chat.addMessage(response.choices[0].message)
            val classPreperation = prepareTestClass("TestClass")
            val compiled = compileTestClass(classPreperation.first)

            if (compiled) {
                val executionResult = executeTestClass(classPreperation.first, classPreperation.second)
                if (executionResult.errorCode == 0) {
                    logger.info("The PUT seems bug free")
                    result = BugResult.NOT_FOUND
                } else {
                    logger.info("The PUT seems to have a bug")
                    result = BugResult.FOUND
                }
            }
        } catch (e: Exception) {
            logger.error("Error during verification of test class", e)
        } finally {
            clock.stop("verify")
            saveResults()
            cleanUp()
        }
    }

    private suspend fun abortAnalysis() {
        result = BugResult.ERROR
        saveResults()
        cleanUp()
    }

    private fun prepareTestClass(fileSuffix: String): Pair<String, String> {
        val putFile = File(bugContext.filePath)
        val className: String = putFile.nameWithoutExtension
        val testClassFileName = "$className$fileSuffix"
        val testClassFileNameWithExtension = "$className$fileSuffix.${putFile.extension}"
        val parent: String = putFile.parent
        val newPath = "$parent/$testClassFileNameWithExtension"

        var testClassResponse = StringUtils.parse(chat.getLastMessage().content)[0]

        testClassResponse = bugContext.fullPackageName + testClassResponse
        testClassResponse = StringUtils.addImport(bugContext.imports, testClassResponse)
        FileUtil.saveToFile(newPath, testClassResponse)
        filesToTrack.add(newPath)
        return Pair(newPath, testClassFileName)
    }

    private suspend fun compileTestClass(filePath: String): Boolean {
        val referenceManager = ReferenceManager()
        logger.info("Starting compilation of $filePath")

        val compileResult = referenceManager.executeProcess(referenceManager.buildCompileProcessBuilder(filePath), 60)
        processStreamOutput.add("PUT Test File: $compileResult")
        if (compileResult.errorCode != 0) {
            logger.error("Compilation of file $filePath failed with error: ${compileResult.output}")
            return false
        }

        logger.info("Compilation of file $filePath was successful")
        filesToTrack.add(getCompiledFilePath(filePath))
        return true
    }

    private suspend fun executeTestClass(filePath: String, className: String): ProcessOutput {
        val referenceManager = ReferenceManager()
        val justPackage = StringUtils.extractPackage(bugContext.fullPackageName, false)
        logger.info("Starting execution of file $filePath")
        val executionResult = referenceManager.executeProcess(
            referenceManager.buildRunProcessBuilder("${justPackage}.${className}"),
            60
        )
        processStreamOutput.add("PUT Test File: $executionResult")
        return if (executionResult.errorCode < 0) {
            logger.error("Execution of file $filePath failed with error: ${executionResult.output}")
            ProcessOutput(-1, executionResult.output)
        } else if (executionResult.errorCode > 0) {
            logger.error("The file $filePath exited with error code ${executionResult.errorCode}")
            ProcessOutput(-1, executionResult.output)
        } else {
            logger.info("Execution of file $filePath was successful")
            executionResult
        }
    }

    private fun getCompiledFilePath(filePath: String): String {
        return filePath.replace(".java", ".class")
    }

    private suspend fun saveResults() {
        clock.stop("all")
        val resultsSaver = ResultsSaver()
        resultsSaver.saveResults(
            iteration,
            result,
            chat.toString(),
            bugContext.methodName,
            bugContext.usage,
            filesToTrack,
            processStreamOutput,
            clock
        )
    }

    private fun updateUsage(fullUsage: Usage) {
        bugContext.usage.totalTokens += fullUsage.total_tokens
        bugContext.usage.promptTokens += fullUsage.prompt_tokens
        bugContext.usage.completionTokens += fullUsage.completion_tokens
        bugContext.usage.cachedTokens += fullUsage.prompt_tokens_details.cached_tokens
    }

    private suspend fun cleanUp() {
        for (filePath in filesToTrack) {
            if (filePath != bugContext.filePath) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(Path(filePath))
                }
            }
        }
    }

    private fun addUserMessage(message: String) {
        chat.addMessage(
            Message(
                RoleTypes.user,
                message,
                null
            )
        )
    }

    private fun createReferenceFiles(suffix: String, referenceMethod: String) : String {
        if (bugContext.filePath == null) {
            logger.error("File path is null. Cannot create reference files.")
            return ""
        }

        val originalFile = File(bugContext.filePath!!)
        if (!originalFile.exists()) {
            logger.error("Original file does not exist: $bugContext.filePath")
            return ""
        }

        val parentDir = originalFile.parent ?: throw IllegalStateException("File has no parent directory.")
        val baseFileName = originalFile.nameWithoutExtension
        val extension = originalFile.extension

        val newFileName = "$baseFileName$suffix.$extension"

        logger.info("Creating a new reference file: $newFileName")

        var fileContent = FileUtil.readContent(bugContext.filePath!!)
        val imports = StringUtils.extractImportList(referenceMethod)
        val cleanedReferenceMethod = StringUtils.remove(imports, referenceMethod)

        fileContent = fileContent
            .replace(bugContext.put, cleanedReferenceMethod)
            .replace(baseFileName, baseFileName + suffix)

        fileContent = StringUtils.addImports(imports, fileContent)

        val newFilePath = "$parentDir/$newFileName"
        FileUtil.saveToFile(newFilePath, fileContent)

        logger.info("Successfully created and updated reference file: $newFilePath")
        return newFilePath
    }

    private data class BugContext(
        var put: String = "",
        var filePath: String? = null,
        var imports: String = "",
        var className: String = "",
        var methodName: String = "",
        var putClassContent: String = "",
        var referenceList: List<String> = emptyList(),
        var fullPackageName: String = "",
        var usage: ReducedUsage = ReducedUsage(0, 0, 0, 0)
    )
}