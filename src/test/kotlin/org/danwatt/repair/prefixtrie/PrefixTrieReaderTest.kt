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
    fun caseChangeNoSharedPrefix() {
        val words = "alpha Beta".split(" ")
        testDecode("alpha[05]Beta", words)
    }

    @Test
    fun kjvPartial2() {
        val words = ". And God In and beginning created earth heaven the was without".split(' ')
        testDecode(
            ".[01]And[1E][03]beginning[09]created[07]earth[05]God[03]heaven[06]In[02]the[03]was[02]ithout",
            words
        )
    }

    @Test
    fun issue1() {
        val words = listOf("signs", "sixth", "So", "so", "stars", "Spirit", "subdue")
        val out = PrefixTrieWriter().write(words, 0)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("signs[03]xth[1F][1E][04]o[1E][1E][01]pirit[1F][1E][05]tars[04]ubdue")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }

    @Test
    fun suffixBackspaceIssue() {
        val words = "herb herbs hymn hymns his In in is it itself kind kinds".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("[0D]s[1F]herb[0D][03]is[02]ymn[0D][04]In[1E][01]s[01]t[1F]self[06]kind[0D]")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }
    // Should not have a capital Sea
    //

    @Test
    fun capitalizationSuffixIssue() {
        val words = "saying sayings sea Seas seas seed seeds".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo(
            "[0D]s[1F]" +
                    "saying[0D][05]ea[0D][1E]s[1F][1E][02]ed[0D]"
        )
        assertThat(decoded).contains("Seas").doesNotContain("Sea")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }

    @Test
    fun anotherIssue() {
        val words = "give given God I In".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("[0D]n[1F]give[0D][1E][03]od[03]I[0D]")
        assertThat(decoded).contains("give").doesNotContain("Give").contains("given").doesNotContain("Given")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }

    @Test
    fun allCaps() {
        val words = "test Test TEST".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("TEST[03]est[1E]")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }

    @Test
    fun caseSwitchingAndSuffixes() {
        //hath have having head heads heard
        //he is missing
        val words = "He he Heth hundred hundredth live liveth".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("[0D]th[1F]He[0D][1E][1F][01]undred[0D][07]live[0D]")
        assertThat(decoded).contains("He").contains("he")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }

    @Test
    fun issue() {
        val words = "Be be Because because became been before Therefore the there therefore whe where wherefore herefore".split(' ')
        val out = PrefixTrieWriter().write(words, 16)
        println("============= READING =============")
        val decoded = PrefixTrieReader().read(out)
        assertThat(out.hexify3()).isEqualTo("[0E]refore[0F]cause[0D]fore[10]re[1F]" +
                "Be[0F]" + // Be, Because
                "[1E][1F]" + // be
                "[0F]" + // because
                "[0D]" + // before
                "came[04]en[04]herefore[08]the[0E][10][1E]refore[09]whe[0E][10]")
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(words)
    }


    @Test
    fun kjvFull() {
        LogManager.getLogManager().getLogger("").handlers.forEach { it.level = Level.FINEST }

        val tokens = BibleParser().readTranslation()
        val lexicon = tokens.filterNot { it in listOf("-VERSE-", "-BOOK-", "-CHAPTER-") }.toSortedSet()
        val out = PrefixTrieWriter().write(lexicon, 16)
        println(out.hexify3())

        val decoded = PrefixTrieReader().read(out)

        println("Input : ${lexicon.sorted().joinToString(" ")}")
        println("Output: ${decoded.sorted().joinToString(" ")}")

        // Actual: 13545
        // Expected: 13882
        assertThat(decoded).hasSameSizeAs(lexicon)
        assertThat(decoded).containsExactlyInAnyOrderElementsOf(lexicon)
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
