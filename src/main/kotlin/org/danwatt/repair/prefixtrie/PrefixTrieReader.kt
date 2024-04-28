package org.danwatt.repair.prefixtrie

import org.danwatt.repair.prefixtrie.PrefixTrieWriter.Companion.FLIP_CASE_MARKER
import org.danwatt.repair.prefixtrie.PrefixTrieWriter.Companion.WORD_MARKER
import java.util.logging.Logger
import kotlin.math.max

@ExperimentalStdlibApi
class PrefixTrieReader {
    private val log : Logger = Logger.getLogger(PrefixTrieReader::class.java.name)
    fun read(trie: CharArray): List<String> {
        val words = mutableListOf<String>()
        if (trie.isEmpty()) {
            return words
        }
        var position = 0

        val suffixes = mutableMapOf<Char, String>()

        if (trie[0].code in 13..29) {
            var suffixWord = ""
            var suffixCode = trie[0]
            position++
            while (position < trie.size && trie[position] != WORD_MARKER) {
                val currentChar = trie[position]
                if (currentChar.code in 13..29) {
                    log.fine("Adding suffix ${suffixCode.code.toUByte().toHexString()} : ${suffixWord}")
                    suffixes[suffixCode] = suffixWord
                    suffixCode = currentChar
                    suffixWord = ""
                } else {
                    suffixWord += currentChar
                }
                position++
            }
            if (suffixWord.isNotBlank()) {
                log.fine("Adding suffix ${suffixCode.code.toUByte().toHexString()} : ${suffixWord}")
                suffixes[suffixCode] = suffixWord
            }
        }

        var workingWord = ""
        var previousChar = null as Char?
        var ignoreAdding = false
        while (position < trie.size) {
            var workingChar = trie[position]
            ignoreAdding =
                previousChar != null && (previousChar == WORD_MARKER || previousChar == FLIP_CASE_MARKER || previousChar.code < 16)
            when {
                workingChar == WORD_MARKER -> {
                    log.fine("Word marker encountered")
                    addWord(workingWord, words)
                }

                workingChar == FLIP_CASE_MARKER -> {
                    log.fine("Flip case encountered")
                    //This needs to be smarter. Need a state machine?
                    if (ignoreAdding) {
                        log.fine("Previous character was a word marker, NOT adding flipped case")
                    } else {
                        addWord(workingWord, words)
                    }

                    workingWord = workingWord.flipFirstLetterCase()
                    if (ignoreAdding) {
                        log.fine("Previous character was a word marker, NOT adding flipped case")
                    } else {
                        addWord(workingWord, words)
                    }
                }

                workingChar.code <= 12 -> {
                    log.fine("Backspace ${workingChar.code} characters from '$workingWord'")
                    if (ignoreAdding) {
                        log.fine("Skipping")
                    } else {
                        addWord(workingWord, words)
                    }
                    val length = max(0, workingWord.length - workingChar.code)
                    workingWord = workingWord.take(length)
                }

                workingChar > WORD_MARKER -> {
                    log.fine("Appending '${workingChar}'")
                    workingWord += workingChar
                }

                workingChar.code in 13..29 -> {
                    addWord(workingWord, words)
                    log.fine("Encountered suffix ${workingChar.code.toUByte().toHexString()}")
                    addWord(workingWord + suffixes[workingChar], words)
                }

                else -> {
                    log.fine("Unhandled!?")
                }
            }
            previousChar = workingChar
            position++
        }
        addWord(workingWord, words)
        return words
    }

    private fun addWord(workingWord: String, words: MutableList<String>) {
        if (workingWord.isNotEmpty() && !words.contains(workingWord)) {
            log.fine("Adding $workingWord")
            words.add(workingWord)
        }
    }
}