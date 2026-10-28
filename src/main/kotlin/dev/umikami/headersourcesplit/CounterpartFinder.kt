// Copyright 2026 Ernesto Gonzalez. Licensed under the Apache License, Version 2.0.
// See the LICENSE and NOTICE files in the project root.

package dev.umikami.headersourcesplit

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/** Finds the source file for a header or the header for a source file. Must be called inside a read action. */
object CounterpartFinder {
    // Folder names that conventionally mirror each other (Unreal Public/Private, classic include/src).
    private val MIRRORED_FOLDERS: Map<String, List<String>> = mapOf(
        "public" to listOf("Private"),
        "private" to listOf("Public", "Classes"),
        "classes" to listOf("Private"),
        "include" to listOf("src", "source", "Private"),
        "inc" to listOf("src", "source"),
        "src" to listOf("include", "inc", "Public"),
        "source" to listOf("include", "inc"),
    )

    private const val MAX_MIRROR_DEPTH = 8

    fun find(project: Project, file: VirtualFile): VirtualFile? {
        val kind = CppFileKinds.kindOf(file) ?: return null
        val names = CppFileKinds.counterpartExtensions(kind).map { "${file.nameWithoutExtension}.$it" }
        val dir = file.parent ?: return null

        findInDir(dir, names)?.let { return it }
        findInMirroredFolders(dir, names)?.let { return it }
        return findInIndex(project, file, names)
    }

    private fun findInDir(dir: VirtualFile, names: List<String>): VirtualFile? {
        for (name in names) {
            val child = dir.findChild(name)
            if (child != null && !child.isDirectory) return child
        }
        return null
    }

    /** Walks up from [startDir]; at each mirrored folder (e.g. Public) tries the same sub-path under its twin (Private). */
    private fun findInMirroredFolders(startDir: VirtualFile, names: List<String>): VirtualFile? {
        val relative = ArrayDeque<String>()
        var current: VirtualFile? = startDir
        var depth = 0
        while (current != null && depth < MAX_MIRROR_DEPTH) {
            val parent = current.parent ?: break
            val mirrors = MIRRORED_FOLDERS[current.name.lowercase()]
            if (mirrors != null) {
                for (mirrorName in mirrors) {
                    val mirror = findChildDirIgnoreCase(parent, mirrorName) ?: continue
                    if (relative.isNotEmpty()) {
                        val sameSubPath = mirror.findFileByRelativePath(relative.joinToString("/"))
                        if (sameSubPath != null && sameSubPath.isDirectory) {
                            findInDir(sameSubPath, names)?.let { return it }
                        }
                    }
                    findInDir(mirror, names)?.let { return it }
                }
            }
            relative.addFirst(current.name)
            current = parent
            depth++
        }
        return null
    }

    private fun findChildDirIgnoreCase(dir: VirtualFile, name: String): VirtualFile? =
        dir.findChild(name)?.takeIf { it.isDirectory }
            ?: dir.children.firstOrNull { it.isDirectory && it.name.equals(name, ignoreCase = true) }

    /** Last resort: any same-named counterpart in the project, preferring the one closest to [file] on disk. */
    private fun findInIndex(project: Project, file: VirtualFile, names: List<String>): VirtualFile? {
        if (DumbService.isDumb(project)) return null
        return try {
            val scope = GlobalSearchScope.projectScope(project)
            names.asSequence()
                .flatMap { FilenameIndex.getVirtualFilesByName(it, scope).asSequence() }
                .filter { !it.isDirectory }
                .maxByOrNull { commonPrefixLength(it.path, file.path) }
        }
        catch (_: IndexNotReadyException) {
            null
        }
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val max = minOf(a.length, b.length)
        var i = 0
        while (i < max && a[i].equals(b[i], ignoreCase = true)) i++
        return i
    }
}
