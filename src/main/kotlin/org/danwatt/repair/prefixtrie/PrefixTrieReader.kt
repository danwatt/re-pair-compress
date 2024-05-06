package org.danwatt.repair.prefixtrie

import org.danwatt.repair.prefixtrie.PrefixTrieWriter.Companion.FLIP_CASE_MARKER
import org.danwatt.repair.prefixtrie.PrefixTrieWriter.Companion.WORD_MARKER
import kotlin.math.max

@ExperimentalStdlibApi
class PrefixTrieReader {

    fun read(trie: CharArray): List<String> {
        val words = mutableListOf<String>()
        if (trie.isEmpty()) {
            return words
        }
        val pair = decodeSuffixes(trie)
        var position = pair.first
        val suffixes = pair.second

        var workingWord = ""
        var previousChar = null as Char?
        var ignoreAdding = false
        while (position < trie.size) {
            val workingChar = trie[position]
            when {
                workingChar > WORD_MARKER -> {
                    println("Appending '${workingChar}' : $workingWord$workingChar")
                    workingWord += workingChar
                    println("Enable adding")
                    ignoreAdding = false
                }

                workingChar == WORD_MARKER -> {
                    println("Word marker encountered")
                    addWord(workingWord, words, false)

                    if (!ignoreAdding && trie[position-1] != FLIP_CASE_MARKER) {
                        println("Disabling adding (WORD_MARKER)")
                        ignoreAdding = true
                    }

                }

                workingChar == FLIP_CASE_MARKER -> {
                    println("Flip case encountered, working word is currently '${workingWord}'")
                    addWord(workingWord, words, ignoreAdding)
                    workingWord = workingWord.flipFirstLetterCase()
                    println("Working word is now '${workingWord}'")
                    if ((previousChar == null) || previousChar.code !in SUFFIX_RANGE) {
                        addWord(workingWord, words, ignoreAdding)
                    }
                }

                workingChar.code in SUFFIX_RANGE -> {
                    addWord(workingWord, words, ignoreAdding)
                    println("Encountered suffix ${workingChar.code.toUByte().toHexString()} (${suffixes[workingChar]})")
                    addWord(workingWord + suffixes[workingChar], words, ignoreAdding)
                    println("Enabling adding")
                    ignoreAdding = false
                }

                workingChar.code in BACKSPACE_RANGE -> {
                    println("Backspace ${workingChar.code} characters from '$workingWord' (prev: ${previousChar!!.code})")
                    if (previousChar > WORD_MARKER) {
                        addWord(workingWord, words, ignoreAdding)
                    }
                    val length = max(0, workingWord.length - workingChar.code)
                    workingWord = workingWord.take(length)
                }


                else -> {
                    println("Unhandled!?")
                }
            }
            previousChar = workingChar
            position++
        }
        addWord(workingWord, words, ignoreAdding)
        return words
    }

    private fun decodeSuffixes(trie: CharArray): Pair<Int, MutableMap<Char, String>> {
        var position = 0

        val suffixes = mutableMapOf<Char, String>()

        if (trie[0].code in SUFFIX_RANGE) {
            var suffixWord = ""
            var suffixCode = trie[0]
            position++
            while (position < trie.size && trie[position] != WORD_MARKER) {
                val currentChar = trie[position]
                if (currentChar.code in SUFFIX_RANGE) {
                    println("Adding suffix ${suffixCode.code.toUByte().toHexString()} : ${suffixWord}")
                    suffixes[suffixCode] = suffixWord
                    suffixCode = currentChar
                    suffixWord = ""
                } else {
                    suffixWord += currentChar
                }
                position++
            }
            if (suffixWord.isNotBlank()) {
                println("Adding suffix ${suffixCode.code.toUByte().toHexString()} : ${suffixWord}")
                suffixes[suffixCode] = suffixWord
            }
        }
        return Pair(position, suffixes)
    }

    private fun addWord(workingWord: String, words: MutableList<String>, ignoreAdding: Boolean) {
        if (ignoreAdding) {
            println("Skipping adding '$workingWord'")
            return
        }
        if (workingWord.isNotEmpty() && !words.contains(workingWord)) {
            println("Adding word '$workingWord'")
            words.add(workingWord)
        } else {
            println("Did not add '$workingWord'")
        }
    }

    companion object {
        val BACKSPACE_RANGE = 0..12
        val SUFFIX_RANGE = 13..29
    }
}