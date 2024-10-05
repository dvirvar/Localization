package com.localization.offline.extension

import org.jetbrains.skiko.hostOs
import java.awt.Desktop
import java.io.File
import java.io.IOException

fun Desktop.tryBrowse(folder: File): Boolean {
    if (hostOs.isWindows) {
        return runCommand(arrayOf("explorer", folder.absolutePath))
    } else if (hostOs.isMacOS) {
        return runCommand(arrayOf("open", folder.absolutePath))
    } else if (hostOs.isLinux) {
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
    if (hostOs.isWindows) {
        return runCommand(arrayOf("explorer", "/select,", "\"${file.absolutePath}\""))
    } else if (hostOs.isMacOS) {
        return runCommand(arrayOf("open", "-R", file.absolutePath))
    } else if (hostOs.isLinux) {
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