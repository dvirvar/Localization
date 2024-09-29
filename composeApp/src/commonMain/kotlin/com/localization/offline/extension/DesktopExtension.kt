package com.localization.offline.extension

import java.awt.Desktop
import java.io.File
import java.io.IOException

fun Desktop.tryBrowse(folder: File): Boolean {
    val osName = System.getProperty("os.name").lowercase()
    if (osName.startsWith("win")) {
        return runCommand(arrayOf("explorer", folder.absolutePath))
    } else if (osName.startsWith("mac")) {
        return runCommand(arrayOf("open", folder.absolutePath))
    } else if (osName.contains("solaris") || osName.contains("sunos") || osName.contains("linux") || osName.contains("unix")) {
        if (runCommand(arrayOf("kde-open", folder.absolutePath))) return true
        if (runCommand(arrayOf("gnome-open", folder.absolutePath))) return true
        return runCommand(arrayOf("xdg-open", folder.absolutePath))
    }
    if (isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
        try {
            browseFileDirectory(folder)
            return true
        } catch (e: Exception) {
            return false
        }
    }
    return false
}

fun Desktop.tryBrowseAndHighlight(file: File): Boolean {
    val osName = System.getProperty("os.name").lowercase()
    if (osName.startsWith("win")) {
        return runCommand(arrayOf("explorer", "/select,", "\"${file.absolutePath}\""))
    } else if (osName.startsWith("mac")) {
        return runCommand(arrayOf("open", "-R", file.absolutePath))
    } else if (osName.contains("solaris") || osName.contains("sunos") || osName.contains("linux") || osName.contains("unix")) {
        //TODO: Highlight in supported versions
        if (runCommand(arrayOf("kde-open", file.absolutePath))) return true
        if (runCommand(arrayOf("gnome-open", file.absolutePath))) return true
        return runCommand(arrayOf("xdg-open", file.absolutePath))
    }
    if (isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
        try {
            browseFileDirectory(file)
            return true
        } catch (e: Exception) {
            return false
        }
    }
    return false
}

private fun runCommand(cmdArray: Array<String>): Boolean {
    try {
        val p = Runtime.getRuntime().exec(cmdArray) ?: return false;
        return p.isAlive
    } catch (e: IOException) {
        return false;
    }
}