@file:OptIn(ExperimentalStdlibApi::class)

package org.danwatt.repair.bible

import java.util.*

class PrefixTrieWriter {

    fun write(incomingWords: Collection<String>, maxSuffixCodes: Int = 0): CharArray {
        val suffixes = topSuffixes(incomingWords, Math.min(maxSuffixCodes, 16))
        val suffixMapping: Map<String, Char> = suffixes.mapIndexed { index, s -> s to (Char(13 + index)) }.toMap()
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

        var preserveFlipCaseForOneIteration = false

        while (remainingWords.isNotEmpty()) {
            val word = remainingWords.removeFirst()
            var workingWord = word

            // Capitals sort first
            if (workingWord.differsOnlyByFirstCapitalization(previousWord)) {
                val p = previousWord
                if (outputList.last() == WORD_MARKER) {
                    println("Removing unnecessary WORD_MARKER (2)")
                    outputList.removeLast()
                }
                previousWord = previousWord.flipFirstLetterCase()
                println("Adding FLIP_CASE_MARKER for '$workingWord' from '$p'")
                outputList.add(FLIP_CASE_MARKER)
                addSuffixes(suffixMapping, workingWord, sortedIncoming, remainingWords, outputList)
                continue
            } else if (previousWord.isNotBlank() && workingWord.first().isUpperCase() != previousWord.first().isUpperCase()) {
                println("Prefixes differ by case, flipping case")
                if (outputList.isEmpty() || outputList.last() != FLIP_CASE_MARKER) {
                    outputList.add(FLIP_CASE_MARKER)
                }
                previousWord = previousWord.flipFirstLetterCase()
                preserveFlipCaseForOneIteration= true
            }

            val commonPrefix = commonPrefix(previousWord, workingWord)
            val backspaces = when {
                commonPrefix.isNotEmpty() -> previousWord.length - commonPrefix.length
                previousWord.isNotEmpty() -> workingWord.length
                else -> 0
            }

            if (backspaces > 0) {
                if (backspaces > 28) {
                    throw IllegalArgumentException("Could not add $backspaces backspaces")
                }
                previousWord = commonPrefix

                if (outputList.isNotEmpty() && outputList.last() == WORD_MARKER) {
                    println("Removing redundant last WORD_MARKER")
                    outputList.removeLast()
                } else if (outputList.isNotEmpty() && outputList.last() == FLIP_CASE_MARKER && !preserveFlipCaseForOneIteration) {
                    println("Removing redundant last FLIP_CASE_MARKER")
                    outputList.removeLast()
                }
                preserveFlipCaseForOneIteration = false

                println("Adding $backspaces backspaces, top word is '$previousWord'. Common prefix is '$commonPrefix'")
                outputList.add(Char(backspaces))
            }
            if (workingWord.startsWith(previousWord)) {
                workingWord = workingWord.removePrefix(previousWord)
            }
            if (suffixMapping.contains(workingWord)) {
                println("Current working word '${workingWord}', part of '${word}', is a suffix")
                outputList.add(suffixMapping[workingWord]!!)
            } else {
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
        }
        if (outputList.last() == WORD_MARKER) {
            outputList.removeLast()
        }
        return outputList.toCharArray()
    }

    private fun commonPrefix(wordToKeepFrom: String, other: String): String {
        var s = ""
        for (i in 0 until minOf(wordToKeepFrom.length, other.length)) {
            if (wordToKeepFrom[i].equals(other[i], true)) {
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

        fun topSuffixes(words: Collection<String>, maxSuffixCodes: Int = 0): List<String> {
            val suffixCounts = mutableMapOf<String, Int>()
            words.forEach { str ->
                (1..str.length).forEach { i ->
                    val suffix = str.substring(str.length - i)
                    val prefix = str.substring(0, str.length - i)
                    if (words.contains(prefix)) {
                        suffixCounts[suffix] = suffixCounts.getOrDefault(suffix, 0) + 1
                    }
                }
            }
            return suffixCounts.entries
                .asSequence()
                .filter { it.value > 1 }
                .sortedByDescending { it.key.length * it.value }
                // top 64 just for debugging at the moment
                .take(64)
                .onEach {
                    println("Suffix '${it.key}' could be as much as  ${it.key.length * it.value} bytes")
                }
                .take(maxSuffixCodes)
                .map { it.key }
                .toList()
        }
    }
}

private fun String.flipFirstLetterCase(): String =
    if (this[0].isLowerCase()) {
        this.replaceFirstChar { it.uppercaseChar() }
    } else {
        this.replaceFirstChar { it.lowercaseChar() }
    }

private fun String.differsOnlyByFirstCapitalization(other: String): Boolean =
    this.equals(other, ignoreCase = true) && this.substring(1) == other.substring(1)

