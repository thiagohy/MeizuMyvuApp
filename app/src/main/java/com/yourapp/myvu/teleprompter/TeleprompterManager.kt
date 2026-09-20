package com.yourapp.myvu.teleprompter

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

class TeleprompterManager(
    private val context: Context
) {
    private var currentText = ""
    private var currentPage = 0
    private var linesPerPage = 10
    private var scrollSpeed = 5000L

    fun loadFromFile(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            currentText = reader.readText()
            reader.close()
            currentPage = 0
            currentText
        } catch (e: Exception) {
            null
        }
    }

    fun loadFromText(text: String) {
        currentText = text
        currentPage = 0
    }

    fun loadFromStringResource(resId: Int): String {
        currentText = context.getString(resId)
        currentPage = 0
        return currentText
    }

    fun getCurrentPage(): String {
        val lines = currentText.lines()
        val start = currentPage * linesPerPage
        val end = minOf(start + linesPerPage, lines.size)
        return lines.subList(start, end).joinToString("\n")
    }

    fun nextPage(): Boolean {
        val totalPages = getTotalPages()
        if (currentPage < totalPages - 1) {
            currentPage++
            return true
        }
        return false
    }

    fun previousPage(): Boolean {
        if (currentPage > 0) {
            currentPage--
            return true
        }
        return false
    }

    fun getTotalPages(): Int {
        val lines = currentText.lines().size
        return (lines + linesPerPage - 1) / linesPerPage
    }

    fun getCurrentPageNumber(): Int = currentPage + 1

    fun setLinesPerPage(lines: Int) {
        linesPerPage = lines
    }

    fun setScrollSpeed(speedMs: Long) {
        scrollSpeed = speedMs
    }

    fun getScrollSpeed(): Long = scrollSpeed

    fun search(query: String): List<Int> {
        val results = mutableListOf<Int>()
        val lines = currentText.lines()
        lines.forEachIndexed { index, line ->
            if (line.contains(query, ignoreCase = true)) {
                results.add(index)
            }
        }
        return results
    }

    fun goToLine(lineNumber: Int) {
        currentPage = lineNumber / linesPerPage
    }

    fun getFullText(): String = currentText

    fun clear() {
        currentText = ""
        currentPage = 0
    }

    fun getStatus(): String {
        return "Página ${getCurrentPageNumber()}/${getTotalPages()} - ${currentText.lines().size} linhas"
    }
}
