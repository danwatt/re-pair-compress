package org.danwatt.repair.prefixtrie

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.repair.bible.BibleParser
import org.junit.jupiter.api.Test
import java.util.logging.Level
import java.util.logging.LogManager


@ExperimentalStdlibApi
class PrefixTrieReaderTest {
    @Test
    fun singleLetters() {
        val words = "a[01]b[01]c".deHex()
        assertThat(PrefixTrieReader().read(words.toCharArray())).containsExactly(
            "a",
            "b",
            "c"
        )
    }

    @Test
    fun lowerAndUpper() {
        testDecode("A[1E][01]B", listOf("a", "A", "B"))
    }

    @Test
    fun moreMixedCase() {
        testDecode("As[1E]h[1F][1E][01]k[1E]", listOf("As", "as", "ash", "Ask", "ask"));
    }

    @Test
    fun mixedCaseAndBackspaces() {
        testDecode(
            "access[1F][1E][04]e[1F][1E][01]knowledge[1F][1E][09]tion[1F][1E][05]dvance[1F][1E][04]ise",
            listOf("access", "Ace", "acknowledge", "Action", "advance", "Advise")
        )
    }

    @Test
    fun someBuilding() {
        testDecode("a[1F]n[1F]d", listOf("a", "an", "and"))
    }

    @Test
    fun backspacing() {
        testDecode("and[01]t[02]sk", listOf("and", "ant", "ask"))
    }

    @Test
    fun longBackspace() {
        testDecode(
            "abcdefghijklmnopqrstuvwxyz[0C][0C][01]ce", listOf(
                "abcdefghijklmnopqrstuvwxyz",
                "ace"
            )
        )
    }

    @Test
    fun suffixes() {
        val words = listOf(
            "test",
            "testing",
            "tested",
            "toast",
            "toasting",
            "toasted"
        )
        testDecode("[0D]ing[0E]ed[1F]test[0D][0E][03]oast[0D][0E]", words)
    }

    @Test
    fun suffixesAndCapitalization() {
        val words = listOf(
            "test",
            "Test",
            "testing",
            "tested",
            "Tested",
            "Testing",
            "testy"
        )
        testDecode("[0D]ing[0E]ed[1F]Test[0D][0E][1E][0D][0E]y", words)
    }

    @Test
    fun kjvPartial1() {
        val list =
            "abusing Accad Accept accept acceptable acceptably acceptance acceptation accepted acceptest accepteth accepting access Accho accompanied accompany accompanying".split(
                " "
            )

        testDecode(
            "[0D]s[0E]ing[0F]eth[10]ed[11]est[1F]" +
                    "abusing[1F][1E][06]ccad[02]ept[1E][0E][0F][10][11]able[01]y[03]nce[03]tion[07]ss[1F][1E][03]ho[1F][1E][02]ompanied[03]y[0E]",
            list
        )
    }

    @Test
    fun kjvFull() {
        LogManager.getLogManager().getLogger("").handlers.forEach { it.level = Level.FINEST }

        val tokens = BibleParser().readTranslation().take(20)
        val lexicon = tokens.filterNot { it in listOf("-VERSE-", "-BOOK-", "-CHAPTER-") }.toSortedSet()
        val out = PrefixTrieWriter().write(lexicon, 16)
        println(out.hexify3())
        val decoded = PrefixTrieReader().read(out)
        // Actual: 13545
        // Expected: 13882
        assertThat(decoded).hasSameSizeAs(lexicon)
    }

    private fun testDecode(words: String, expected: List<String>) {
        assertThat(PrefixTrieReader().read(words.deHex().toCharArray())).containsExactlyInAnyOrderElementsOf(expected)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun String.deHex(): String {
    var i = 0
    var out = ""

    while (i < this.length) {
        if (this[i] == '[') {
            val nextChar = Char(this.substring(i + 1, i + 3).hexToInt())
            out += nextChar
            i += 4
        } else {
            out += this[i]
            i++
        }
    }
    return out
}
