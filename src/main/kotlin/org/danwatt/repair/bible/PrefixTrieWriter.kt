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
        val stack = Stack<Char>()


        val outputList = mutableListOf<Char>()
        val sortedIncoming = sortWords(incomingWords)
        val remainingWords = sortedIncoming.toMutableList()

        writeSuffixHeader(suffixMapping, outputList)

        while (remainingWords.isNotEmpty()) {
            val word = remainingWords.removeFirst()
            var workingWord = word
            var previousWord = stack.joinToString("")

            // Capitals sort first
            if (workingWord.equals(
                    previousWord,
                    ignoreCase = true
                ) && workingWord.substring(1) == previousWord.substring(1)
            ) {
                if (outputList.last() == WORD_MARKER) {
                    println("Removing unnecessary WORD_MARKER (2)")
                    outputList.removeLast()
                }
                stack[0] = if (stack[0].isLowerCase()) {
                    stack[0].uppercaseChar()
                } else {
                    stack[0].lowercaseChar()
                }
                println("Adding FLIP_CASE_MARKER for '$workingWord' from '$previousWord'")
                outputList.add(FLIP_CASE_MARKER)
                addSuffixes(suffixMapping, workingWord, sortedIncoming, remainingWords, outputList)
                continue
            }

            var backspaces = 0
            while (!workingWord.startsWith(previousWord) && previousWord.isNotEmpty()) {
                stack.pop()
                previousWord = previousWord.substring(0, previousWord.length - 1)
                if (outputList.isNotEmpty() && outputList.last() == WORD_MARKER) {
                    println("Removing redundant WORD_MARKER (3)")
                    outputList.removeLast()
                } else if (outputList.isNotEmpty() && outputList.last() == FLIP_CASE_MARKER) {
                    println("Removing redundant FLIP_CASE_MARKER")
                    outputList.removeLast()
                }
                backspaces++
            }
            if (backspaces > 0) {
                if (backspaces > 28) {
                    throw IllegalArgumentException("Could not add $backspaces backspaces")
                }
                println("Adding $backspaces backspaces, top word is '${stack.joinToString("")}'")
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
                    stack.push(char)
                    println("Adding '$char'")
                    outputList.add(char)
                }

                val suffixFound = addSuffixes(suffixMapping, word, sortedIncoming, remainingWords, outputList)
                if (suffixFound) {
                    println("Since a suffix was found, WORD_MARKER is skipped")
                } else {
                    println("Adding word marker (31) - $workingWord")
                    outputList.add(WORD_MARKER)
                }
            }
        }
        if (outputList.last() == WORD_MARKER) {
            outputList.removeLast()
        }
        return outputList.toCharArray()
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
        val WORD_MARKER = Char(31)//31 = UNIT SEPARATOR
        val FLIP_CASE_MARKER = Char(30)

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