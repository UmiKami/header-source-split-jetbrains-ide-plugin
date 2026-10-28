// Copyright 2026 Ernesto Gonzalez. Licensed under the Apache License, Version 2.0.
// See the LICENSE and NOTICE files in the project root.

package dev.umikami.headersourcesplit

import com.intellij.openapi.vfs.VirtualFile

enum class CppFileKind { HEADER, SOURCE }

object CppFileKinds {
    // Ordered by preference when looking for a counterpart.
    val HEADER_EXTENSIONS = listOf("h", "hpp", "hh", "hxx", "h++")
    val SOURCE_EXTENSIONS = listOf("cpp", "cc", "cxx", "c", "c++")

    fun kindOf(file: VirtualFile): CppFileKind? {
        if (file.isDirectory) return null
        val ext = file.extension?.lowercase() ?: return null
        return when (ext) {
            in HEADER_EXTENSIONS -> CppFileKind.HEADER
            in SOURCE_EXTENSIONS -> CppFileKind.SOURCE
            else -> null
        }
    }

    fun counterpartExtensions(kind: CppFileKind): List<String> =
        if (kind == CppFileKind.HEADER) SOURCE_EXTENSIONS else HEADER_EXTENSIONS
}
