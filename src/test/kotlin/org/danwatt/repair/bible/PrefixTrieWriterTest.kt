package org.danwatt.repair.bible

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

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
        assertThat(PrefixTrieWriter().write(words)).containsExactly(
            'a', C01,
            'b', C01,
            'c'
        )
    }

    @Test
    fun lowerAndUpper() {
        val words = listOf("a", "A", "B")
        assertThat(PrefixTrieWriter().write(words).hexify2()).containsExactly(
            "A", "0x1E", "0x01", "B"
        )
    }

    @Test
    fun moreMixedCase() {
        val words = listOf("As", "as", "ash", "Ask", "ask")
        // Sorted: As as ash Ask ask
        assertThat(PrefixTrieWriter().write(words).hexify2()).containsExactly(
            "As",
            "0x1E", // - adds both As and as, start lower case mode
            "h", // ash
            "0x1F",
            "0x1E", // end of lower case, stack reverts to "As" - this should be a 1E
            "0x01",
            "k",
            "0x1E"// 1E/30 = Ask & ask
            // final 31 not needed
        )
    }

    @Test
    fun mixedCaseAndBackspaces() {
        val words = listOf("access", "Ace", "acknowledge", "Action", "advance", "Advise")
        assertThat(PrefixTrieWriter().write(words).hexify2()).containsExactly(
            "access",
            "0x1F",
            "0x1E",
            "0x04",
            "e",
            "0x1F",
            "0x1E",
            "0x01",
            "knowledge",
            "0x1F",
            "0x1E",
            "0x09",
            "tion",
            "0x1F",
            "0x1E",
            "0x05",
            "dvance",
            "0x1F",
            "0x1E",
            "0x04",
            "ise"
            /* OLD
            "access",
            "0x06",//Should be 0x04, followed by cap
            "Ace",
            "0x03",//Should be 0x01, followed by flip case
            "acknowledge",
            "0x0B",
            "Action",
            "0x06",
            "advance",
            "0x07",
            "Advise"
            */
        )
    }

    @Test
    fun someBuilding() {
        val words = listOf("a", "an", "and")
        assertThat(PrefixTrieWriter().write(words)).containsExactly(
            'a', C31,
            'n', C31,
            'd'
        )
    }

    @Test
    fun backspacing() {
        val words = listOf("and", "ant", "ask")
        assertThat(PrefixTrieWriter().write(words)).containsExactly(
            'a', 'n', 'd', C01,
            't', C02,
            's', 'k'
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
        assertThat(PrefixTrieWriter().write(words, maxSuffixCodes = 2).hexify()).containsExactly(
            "0D", "i", "n", "g",
            "0E", "e", "d",
            "1F",
            "t", "e", "s", "t", "0D", "0E",
            "03", "o", "a", "s", "t", "0D", "0E"
        )
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
        assertThat(PrefixTrieWriter().write(words, maxSuffixCodes = 2).hexify2()).containsExactly(
            "0x0D", "ing",
            "0x0E", "ed",
            "0x1F",
            "Test",
            "0x0D",// Testing
            "0x0E",// Tested
            "0x1E", // flip case
            "0x0D", //testing
            "0x0E", // tested
            "y"
        )
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
        // 45_813 - flip case fix 2
        // tbd: vowel rule?
        assertThat(out.size).isEqualTo(45_813)
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

            /*
            // Suffix header
            "0x0D", "s",
            "0x0E", "ing",
            "0x0F", "eth",
            "0x10", "ed",
            "0x11", "est",
            // Words
            "0x1F", "abusing", // should be backspace 6, and flip : +1
            "0x07", "Accad", // ccad
            "0x02", "ept", // Accept
            "0x1E", // accept
            "0x0E", // accepting
            "0x0F", // accepteth
            "0x10", // accepted
            "0x11", // acceptest
            "able", // acceptable
            "0x01", // acceptabl...
            "y",
            "0x03", "nce", // acceptance
            "0x03", "tion", // acception
            "0x07", "ss", // access
            "0x06", // this should just be a backspace 3 with a capitalization +1 byte
            "Accho", // - 3 bytes
            "0x05", // backspace 2, go to lower-case - + 1 byte
            "accompanied", // -3 bytes
            "0x03", "y",
            "0x0E"*
             */
        )
        // savings : +1 -3 +1 -3 = -4 bytes saved
    }
}


@OptIn(ExperimentalStdlibApi::class)
private fun CharArray.hexify(): List<String> = this.map { it ->
    if (it.code <= 31) {
        it.code.toUByte().toHexString(format = HexFormat.UpperCase)
    } else {
        it.toString()
    }
}

@OptIn(ExperimentalStdlibApi::class)
private fun CharArray.hexify2(): List<String> = this.joinToString("") { c ->

    if (c.code <= 31) {
        " 0x" + c.code.toUByte().toHexString(format = HexFormat.UpperCase) + " "
    } else {
        c.toString()
    }
}.split(" ").filter { it.isNotBlank() }

