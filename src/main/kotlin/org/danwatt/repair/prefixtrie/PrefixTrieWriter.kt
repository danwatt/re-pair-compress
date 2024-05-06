package org.danwatt.repair.prefixtrie

import java.util.*
import java.util.logging.Logger
import kotlin.math.min

@ExperimentalStdlibApi
class PrefixTrieWriter {
    fun write(incomingWords: Collection<String>, maxSuffixCodes: Int = 0): CharArray {
        println("Words are: ${incomingWords.joinToString(" ")}")
        val suffixes = topSuffixes(incomingWords, min(maxSuffixCodes, 16))
        val suffixMapping: Map<String, Char> = suffixes.mapIndexed { index, s -> s to (Char(13 + index)) }.sortedByDescending { it.first.length }.toMap()
        suffixMapping.forEach { (suffix, c) ->
            println("Suffix '$suffix' will be represented by character 0x${c.code.toUByte().toHexString()}")
        }
        return writeWithSuffixes(incomingWords, suffixMapping)
    }

    fun writeWithSuffixes(
        incomingWords: Collection<String>,
        suffixMapping: Map<String, Char>,
    ): CharArray {
        val outputList = mutableListOf<Char>()
        val sortedIncoming = sortWords(incomingWords)
        val remainingWords = sortedIncoming.toMutableList()

        writeSuffixHeader(suffixMapping, outputList)
        var previousWord = ""

        while (remainingWords.isNotEmpty()) {
            val word = remainingWords.removeFirst()

            var workingWord = word
            println("Working word: '$workingWord'")

            // Capitals sort first
            if (workingWord.differsOnlyByFirstCapitalization(previousWord)) {
                val p = previousWord
                if (outputList.last() == WORD_MARKER) {
                    println("Removing unnecessary WORD_MARKER (2)")
                    outputList.removeLast()
                }
                previousWord = previousWord.flipFirstLetterCase()
                println("Adding FLIP_CASE_MARKER '$p' -> '$previousWord'")
                val addWordMarker = outputList.last() in suffixMapping.values
                outputList.add(FLIP_CASE_MARKER)

                if (addWordMarker) {
                    println("Adding explicit WORD_MARKER")
                    outputList.add(WORD_MARKER)
                    outputList.add(WORD_MARKER)
                }
                addSuffixes(suffixMapping, workingWord, sortedIncoming, remainingWords, outputList)
                continue
            } else if (previousWord.isNotBlank()
                && workingWord.first().isUpperCase() != previousWord.first().isUpperCase()
                && workingWord.first().isLetter()
                && previousWord.first().isLetter()
                && previousWord.first().equals(workingWord.first(), ignoreCase = true)
            ) {
                println("Prefixes differ by case, flipping case ('$previousWord' vs '$workingWord')")
                    outputList.add(FLIP_CASE_MARKER)
                previousWord = previousWord.flipFirstLetterCase()
            }

            val commonPrefix = commonPrefix(previousWord, workingWord)
            println("Checking for backspaces. Previous: '$previousWord', working: '$workingWord', common: '$commonPrefix' (${commonPrefix.length})")
            var backspaces = when {
                commonPrefix.isNotBlank() -> previousWord.length - commonPrefix.length
                previousWord.isNotBlank() -> previousWord.length
                else -> 0
            }

            if (backspaces > 0) {
                previousWord = commonPrefix

                if (outputList.isNotEmpty() && outputList.last() == WORD_MARKER) {
                    println("Removing redundant last WORD_MARKER")
                    outputList.removeLast()
                }

                println("Adding $backspaces backspaces, top word is '$previousWord'. Common prefix is '$commonPrefix'")

                while (backspaces > 0) {
                    if (backspaces > MAX_BACKSPACES) {
                        outputList.add(Char(MAX_BACKSPACES))
                    } else {
                        outputList.add(Char(backspaces))
                    }
                    backspaces -= MAX_BACKSPACES
                }
            }
            if (workingWord.startsWith(previousWord)) {
                println("'$workingWord' starts with '$previousWord', removing prefix")
                // this may be off...
                workingWord = workingWord.removePrefix(previousWord)
            }
                workingWord.toCharArray().forEach { char ->
                    previousWord += char
                    println("Adding '$char'")
                    outputList.add(char)
                }

                val suffixFound = addSuffixes(suffixMapping, word, sortedIncoming, remainingWords, outputList)
                if (suffixFound) {
                    println("Since a suffix was found, WORD_MARKER is skipped")
                } else {
                    println("Adding WORD_MARKER - $workingWord")
                    outputList.add(WORD_MARKER)
                }
        }
        if (outputList.last() == WORD_MARKER) {
            outputList.removeLast()
        }
        //This is dumb, but works
        var i = 1
        while (i < outputList.size) {
            if (outputList[i-1] == outputList[i] && outputList[i] == WORD_MARKER) {
                outputList.removeAt(i)
                println("Removing extra WORD_MARKER at $i")
            }
            i++
        }
        return outputList.toCharArray()
    }

    private fun commonPrefix(wordToKeepFrom: String, other: String): String {
        var s = ""
        for (i in 0 until minOf(wordToKeepFrom.length, other.length)) {
            if (wordToKeepFrom[i].equals(other[i], false)) {
                s += wordToKeepFrom[i]
            } else {
                break
            }
        }
        return s
    }

    private fun writeSuffixHeader(
        suffixMapping: Map<String, Char>,
        outputList: MutableList<Char>,
    ) {
        if (suffixMapping.isNotEmpty()) {
            suffixMapping.forEach { (suffix, c) ->
                println("Adding suffix '$suffix'")
                outputList.add(c)
                outputList.addAll(suffix.asSequence())
            }
            outputList.add(WORD_MARKER)
            outputList.add(WORD_MARKER)
        }
    }

    private fun addSuffixes(
        suffixMapping: Map<String, Char>,
        word: String,
        sortedIncoming: SortedSet<String>,
        remainingWords: MutableList<String>,
        outputList: MutableList<Char>,
    ): Boolean {
        var suffixFound = false
        for ((suffix, char) in suffixMapping) {
            val candidate = word + suffix
            if (sortedIncoming.contains(candidate)) {
                val foundAt = remainingWords.indexOf(candidate)
                if (foundAt >= 0) {
                    remainingWords.removeAt(foundAt)
                    println("Adding suffix 0x${char.code.toUByte().toHexString()}")
                    outputList.add(char)
                    suffixFound = true
                }
            }
        }
        return suffixFound
    }

    private fun sortWords(incomingWords: Collection<String>): SortedSet<String> {
        val sortedIncoming =
            incomingWords.toSortedSet { o1: String, o2: String ->
                val c1 = o1.lowercase().compareTo(o2.lowercase())
                if (c1 != 0) {
                    c1
                } else {
                    o1.compareTo(o2)
                }
            }

        println("Sorted: ${sortedIncoming.joinToString(" ")}")
        return sortedIncoming
    }

    companion object {
        val WORD_MARKER = Char(31)      // 0x1F 31 = UNIT SEPARATOR
        val FLIP_CASE_MARKER = Char(30) // 0x1E
        val MAX_BACKSPACES = 12

        fun topSuffixes(words: Collection<String>, maxSuffixCodes: Int = 0): List<String> {
            val wordsAsSet = words.toSet()
            val suffixCounts = mutableMapOf<String, Int>()
            wordsAsSet.forEach { str ->
                (1..str.length).forEach { i ->
                    val suffix = str.substring(str.length - i)
                    val prefix = str.substring(0, str.length - i)
                    if (wordsAsSet.contains(prefix)) {
                        suffixCounts[suffix] = suffixCounts.getOrDefault(suffix, 0) + 1
                    }
                }
            }
            return suffixCounts.entries
                .asSequence()
                .filter { it.value > 1 }
                .sortedByDescending { it.key.length * it.value }
                .take(maxSuffixCodes)
                .map { it.key }
                .toList()
        }
    }
}