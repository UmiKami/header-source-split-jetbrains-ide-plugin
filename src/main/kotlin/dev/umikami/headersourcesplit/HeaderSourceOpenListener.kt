// Copyright 2026 Ernesto Gonzalez. Licensed under the Apache License, Version 2.0.
// See the LICENSE and NOTICE files in the project root.

package dev.umikami.headersourcesplit

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Point

class HeaderSourceOpenListener(private val project: Project) : FileEditorManagerListener {
    // Set while we open/move files ourselves, so those opens don't start another pairing.
    private var arranging = false

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (arranging || !HeaderSourceSplitSettings.getInstance().state.enabled) return
        if (CppFileKinds.kindOf(file) == null) return

        ReadAction.nonBlocking<VirtualFile?> { CounterpartFinder.find(project, file) }
            .expireWith(project)
            .finishOnUiThread(ModalityState.nonModal()) { counterpart ->
                if (counterpart != null) arrange(file, counterpart)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun arrange(file: VirtualFile, counterpart: VirtualFile) {
        if (project.isDisposed || !file.isValid || !counterpart.isValid) return
        val settings = HeaderSourceSplitSettings.getInstance().state
        if (!settings.enabled) return

        val manager = FileEditorManagerEx.getInstanceEx(project)
        // Only pair the file the user is looking at (skips tabs restored in the background on startup).
        if (file !in manager.selectedFiles) return

        val windows = manager.windows.toList()
        val fileWindow = manager.currentWindow?.takeIf { file in it.fileList }
            ?: windows.firstOrNull { file in it.fileList }
            ?: return
        // Counterpart is already visible in another split: nothing to do.
        if (windows.any { it != fileWindow && counterpart in it.fileList }) return

        val fileIsHeader = CppFileKinds.kindOf(file) == CppFileKind.HEADER
        val fileGoesFirst = fileIsHeader == settings.headerFirst

        arranging = true
        try {
            if (windows.size <= 1) {
                // Every argument is passed so this binds to split(...) itself, not split$default, whose
                // signature changes whenever the platform adds a parameter (binary incompatibility).
                fileWindow.split(
                    orientation = settings.layout.splitOrientation,
                    forceSplit = true,
                    virtualFile = counterpart,
                    focusNew = false,
                    fileIsSecondaryComponent = fileGoesFirst,
                    forceFocus = false,
                )
            }
            else {
                arrangeInExistingSplits(manager, windows, fileWindow, file, counterpart, fileGoesFirst, settings.layout)
            }
        }
        finally {
            arranging = false
        }
    }

    /** Editor is already split: put the pair into the outermost two windows along the configured axis. */
    private fun arrangeInExistingSplits(
        manager: FileEditorManagerEx,
        windows: List<EditorWindow>,
        fileWindow: EditorWindow,
        file: VirtualFile,
        counterpart: VirtualFile,
        fileGoesFirst: Boolean,
        layout: SplitLayout,
    ) {
        val positions = windows.associateWith { position(it) }
        val sorted = if (layout == SplitLayout.SIDE_BY_SIDE) {
            windows.sortedWith(compareBy({ positions.getValue(it).x }, { positions.getValue(it).y }))
        }
        else {
            windows.sortedWith(compareBy({ positions.getValue(it).y }, { positions.getValue(it).x }))
        }
        val fileTarget = if (fileGoesFirst) sorted.first() else sorted.last()
        val counterpartTarget = if (fileGoesFirst) sorted.last() else sorted.first()

        openIn(manager, counterpartTarget, counterpart, focus = false)
        openIn(manager, fileTarget, file, focus = true)
        if (fileTarget != fileWindow) {
            // fileOpened only fires for a file that wasn't open anywhere, so this closes nothing the user had before.
            fileWindow.closeFile(file, disposeIfNeeded = true)
        }
    }

    // The platform has no stable "open in this window" API (FileEditorOpenOptions is internal, FileEditorOpenRequest
    // was experimental and removed), so make the window current and open there with the plain public openFile.
    private fun openIn(manager: FileEditorManagerEx, window: EditorWindow, file: VirtualFile, focus: Boolean) {
        window.setAsCurrentWindow(focus)
        // searchForOpen = false: open in the current window even if the file already has a tab elsewhere.
        manager.openFile(file, focus, false)
    }

    private fun position(window: EditorWindow): Point {
        val component = window.selectedComposite?.component ?: return Point(0, 0)
        return if (component.isShowing) component.locationOnScreen else Point(0, 0)
    }
}
