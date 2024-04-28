package org.danwatt.repair.prefixtrie

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.danwatt.repair.bible.BibleParser
import org.junit.jupiter.api.Test

@ExperimentalStdlibApi
class PrefixTrieWriterTest {
    @Test
    fun topSuffixes() {
        assertThat(
            PrefixTrieWriter.topSuffixes(
                listOf(
                    "test",
                    "testing",
                    "tested",
                    "toast",
                    "toasting",
                    "toasted"
                ), 2
            )
        ).containsExactly("ing", "ed")
    }

    val C31 = Char(31)
    val C01 = Char(1)
    val C02 = Char(2)

    @Test
    fun singleLetters() {
        val words = listOf("a", "b", "c")
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("a[01]b[01]c")
    }

    @Test
    fun lowerAndUpper() {
        val words = listOf("a", "A", "B")
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("A[1E][01]B")
    }

    @Test
    fun moreMixedCase() {
        val words = listOf("As", "as", "ash", "Ask", "ask")
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("As[1E]h[1F][1E][01]k[1E]")
    }

    @Test
    fun mixedCaseAndBackspaces() {
        val words = listOf("access", "Ace", "acknowledge", "Action", "advance", "Advise")
        assertThat(
            PrefixTrieWriter().write(words).hexify3()
        ).isEqualTo("access[1F][1E][04]e[1F][1E][01]knowledge[1F][1E][09]tion[1F][1E][05]dvance[1F][1E][04]ise")
    }

    @Test
    fun someBuilding() {
        val words = listOf("a", "an", "and")
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("a[1F]n[1F]d")
    }

    @Test
    fun backspacing() {
        val words = listOf("and", "ant", "ask")
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("and[01]t[02]sk")
    }

    @Test
    fun longBackspace() {
        val words = listOf(
            "abcdefghijklmnopqrstuvwxyz",
            "ace"
        )
        assertThat(PrefixTrieWriter().write(words).hexify3()).isEqualTo("abcdefghijklmnopqrstuvwxyz[0C][0C][01]ce")
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
        assertThat(
            PrefixTrieWriter().write(words, maxSuffixCodes = 2).hexify3()
        ).isEqualTo("[0D]ing[0E]ed[1F]test[0D][0E][03]oast[0D][0E]")
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
        assertThat(
            PrefixTrieWriter().write(words, maxSuffixCodes = 2).hexify3()
        ).isEqualTo("[0D]ing[0E]ed[1F]Test[0D][0E][1E][0D][0E]y")
    }

    @Test
    fun kjvTest() {
        /*
Suffix 's' could be as much as  1191 bytes
Suffix 'ing' could be as much as  1176 bytes
Suffix 'eth' could be as much as  1089 bytes
Suffix 'ed' could be as much as  1048 bytes
Suffix 'est' could be as much as  576 bytes
Suffix ''s' could be as much as  458 bytes
Suffix 'ness' could be as much as  420 bytes
Suffix 'th' could be as much as  414 bytes
Suffix 'ly' could be as much as  384 bytes
Suffix 'ites' could be as much as  332 bytes
Suffix 'st' could be as much as  306 bytes
Suffix 'd' could be as much as  304 bytes
Suffix 'er' could be as much as  296 bytes
Suffix 'ers' could be as much as  231 bytes
Suffix 'ings' could be as much as  208 bytes
Suffix 'edst' could be as much as  140 bytes

Suffix 'es' could be as much as  118 bytes
Suffix 'ite' could be as much as  114 bytes
Suffix 'ah' could be as much as  98 bytes
Suffix 's'' could be as much as  96 bytes
-- 426 bytes

Suffix 'n' could be as much as  90 bytes
Suffix 'ful' could be as much as  90 bytes
Suffix 'soever' could be as much as  84 bytes
Suffix 'h' could be as much as  82 bytes // Another 772 bytes

         */
        val tokens = BibleParser().readTranslation()
        val lexicon = tokens.distinct().sorted()
        val out = PrefixTrieWriter().write(lexicon, 16)
        // Target is 45kb
        // 58_043 - with 0 suffixes
        // 52_689 - with 16 suffixes
        // 49_763 with suffix capitalization rules
        // 49_377 with suffix optimiation 1
        // 48_249 - flip case fix
        // 45823 - flip case fix 2
        // tbd: vowel rule?
        assertThat(out.size).isEqualTo(45823)
    }

    @Test
    fun kjvPartial1() {
        val list =
            "abusing Accad Accept accept acceptable acceptably acceptance acceptation accepted acceptest accepteth accepting access Accho accompanied accompany accompanying".split(
                " "
            )

        val suffixes = mapOf(
            "s" to Char(13),
            "ing" to Char(14),
            "eth" to Char(15),
            "ed" to Char(16),
            "est" to Char(17),
        )

        val out = PrefixTrieWriter().writeWithSuffixes(list, suffixes)
        assertThat(out.hexify3()).isEqualTo("[0D]s[0E]ing[0F]eth[10]ed[11]est[1F]abusing[1F][1E][06]ccad[02]ept[1E][0E][0F][10][11]able[01]y[03]nce[03]tion[07]ss[1F][1E][03]ho[1F][1E][02]ompanied[03]y[0E]")
        assertThat(out.hexify2()).containsExactly(
            "0x0D", "s",
            "0x0E", "ing",
            "0x0F", "eth",
            "0x10", "ed",
            "0x11", "est",
            "0x1F", // End of suffixes
            "abusing", "0x1F", "0x1E", "0x06", // End the word, flip case, backspace 6
            "ccad", // Accad
            "0x02", // Back 2
            "ept", "0x1E", // Accept and accept, working is now "accept"
            "0x0E", "0x0F", "0x10", "0x11", // Suffixes
            "able", // acceptable
            "0x01", "y", // acceptably
            "0x03", "nce", // acceptance
            "0x03", "tion", // acceptation
            "0x07", "ss", "0x1F", "0x1E",
            "0x03", "ho", "0x1F", "0x1E",
            "0x02", "ompanied",
            "0x03", "y",
            "0x0E"
        )
    }

    @Test
    fun kjvPartial2() {
        val words = ". And God In and beginning created earth heaven the was without".split(' ')
        val out = PrefixTrieWriter().write(words)
        // This is incorrect
        assertThat(out.hexify3()).isEqualTo(".[01]And[09]beginning[07]created[05]earth[1F][1E][03]God[1F][1E][06]heaven[1F][1E][02]In[1F][1E][03]the[03]was[02]ithout")
    }
}