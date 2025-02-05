package com.github.mrfapa.diffprompt

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BugCheckAction : AnAction("Check for Bug") {

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        val editor: Editor? = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
        val file: VirtualFile? = event.getData(PlatformDataKeys.VIRTUAL_FILE)

        if (project != null && editor != null) {
            val selectedText = editor.selectionModel.selectedText
            if (selectedText != null && file != null) {
                CoroutineScope(Dispatchers.Default).launch {
                    for (i in 1..8) {
                        val bugPrompter = BugPrompter()
                        bugPrompter.testForBug(selectedText, file.path, i)
                    }
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val editor = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
        event.presentation.isEnabled = editor?.selectionModel?.hasSelection() == true
    }
}
