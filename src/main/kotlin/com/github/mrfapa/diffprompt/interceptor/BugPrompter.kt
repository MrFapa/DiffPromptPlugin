package com.github.mrfapa.diffprompt

import com.github.mrfapa.diffprompt.api.ApiWrapper
import com.github.mrfapa.diffprompt.api.ChatGPTChat
import com.github.mrfapa.diffprompt.data.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class BugPrompter {

    private var chat: ChatGPTChat = ChatGPTChat()
    private val apiWrapper: ApiWrapper = ApiWrapper.create()
    private val logger: Logger = LoggerFactory.getLogger(BugPrompter::class.java)
    private var bugContext = BugContext();
    private var lastCallback: ChatGPTCallback? = null

    fun testForBug(put: String, filePath: String) {
        this.chat = ChatGPTChat()
        initializeBugContext(put, filePath)
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


    private fun promptIntention() {
        addUserMessage(MyBundle.message("prompts.intention", bugContext.put, bugContext.imports))
        logger.info("Asking ChatGPT for the Intention of the given PUT")
        apiWrapper.prompt(chat, ChatGPTCallback(chat) {
            promptReferences()
        })
    }


    private fun promptReferences() {
        addUserMessage(MyBundle.message("prompts.references"))
        logger.info("Asking ChatGPT to generate references to the given PUT")
        apiWrapper.prompt(chat, ChatGPTCallback(chat) {
            bugContext.referenceList = StringUtils.parse(chat.getLastMessage().content)
            val referenceManager = ReferenceManager()
            val pathRefOne = createReferenceFiles("1", bugContext.referenceList[0])
            val pathRefTwo = createReferenceFiles("2", bugContext.referenceList[1])
            referenceManager.executeProcess(referenceManager.buildCompileProcessBuilder(pathRefOne))
            referenceManager.executeProcess(referenceManager.buildCompileProcessBuilder(pathRefTwo))
            promptDiverseInput()
        })
    }

    private fun promptDiverseInput(){
        addUserMessage(MyBundle.message("prompts.diverseInput"))
        logger.info("Asking ChatGPT to generate diverse Test Input")
        apiWrapper.prompt(chat, ChatGPTCallback(chat) {
            promptTestReferences()
        })
    }

    private fun promptTestReferences() {
        addUserMessage(MyBundle.message("prompts.testReferences", bugContext.className))
        logger.info("Asking ChatGPT to generate a test class, that tests both references")
        apiWrapper.prompt(chat, ChatGPTCallback(chat) {
            promptTestAll()
        })
    }

    private fun promptTestAll() {
        addUserMessage(MyBundle.message("prompts.testAll", bugContext.className))
        logger.info("Asking ChatGPT to generate test class")
        apiWrapper.prompt(chat, ChatGPTCallback(chat) {
            promptVerify()
        })
    }

    private fun promptVerify(){
        addUserMessage(MyBundle.message("prompt.verify"))
        logger.info("Asking ChatGPT to verify the test class")
        lastCallback = ChatGPTCallback(chat) {
            val putFile = File(bugContext.filePath)
            val className: String = putFile.nameWithoutExtension
            val testClassFileName = "${className}TestClass.${putFile.extension}"
            val parent: String = putFile.parent
            val newPath = "$parent/$testClassFileName"
            var testClassResponse = StringUtils.parse(chat.getLastMessage().content)[0]
            testClassResponse = bugContext.fullPackageName + testClassResponse

            FileUtil.saveToFile(newPath, testClassResponse)
            val referenceManager = ReferenceManager()
            referenceManager.executeProcess(referenceManager.buildCompileProcessBuilder(newPath))

            val justPackage = StringUtils.extractPackage(bugContext.fullPackageName, false)
            referenceManager.executeProcess(referenceManager.buildRunProcessBuilder("${justPackage}.${bugContext.className}TestClass"))
            saveResults()
        }
        apiWrapper.prompt(chat, lastCallback!!)
    }

    private fun saveResults() {
        lastCallback?.getUsage()?.let { chat.writeResultsToFile(bugContext.methodName, it) }
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
        var fullPackageName: String = ""
    )


}