package org.danwatt.repair.bible

import java.nio.charset.Charset
import java.util.*

class BibleParser {
    fun readTranslation(): List<String> {
        val stream = BibleParser::class.java.getResourceAsStream("/t_kjv.csv")
        stream.bufferedReader(Charset.forName("UTF-8")).use {
            return it.readLines().asSequence().drop(1).map { line ->
                val s = line.split(",", ignoreCase = true, limit = 5)
                val id = s[0].toInt()
                val book = s[1].toInt()
                val chapter = s[2].toInt()
                val verse = s[3].toInt()
                val specialTokens = mutableListOf<String>()
                if (chapter == 1 && verse == 1) {
                    specialTokens.add("-BOOK-")
                }
                if (verse == 1) {
                    specialTokens.add("-CHAPTER-")
                }
                specialTokens.add("-VERSE-")
                specialTokens + tokenize(s[4].trim('"'))
            }.flatten().toList()
        }
    }

    fun tokenize(text: String): List<String> =
        StringTokenizer(text, " .,;:?!()-", true)
            .asSequence()
            .filter { it != " " }
            .toList() as List<String>
}