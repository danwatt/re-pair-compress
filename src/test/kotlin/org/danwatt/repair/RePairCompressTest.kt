package org.danwatt.repair

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertWith
import org.junit.jupiter.api.Test
import java.util.*

class RePairCompressTest {

    @Test
    fun `inner workings part1`() {
        val items = listOf("A", "B", "C", "A", "B", "C", "A", "B", "C")
        val (sequence, pairs) = RePairCompress().buildPairsAndSequence(items)

        assertThat(sequence.map { it.value.toString() }).isEqualTo(items)
        assertThat(sequence.map { it.nextValidItem }).isEqualTo(listOf(1, 2, 3, 4, 5, 6, 7, 8, -1))
        assertThat(sequence.map { it.previousValidItem }).isEqualTo(listOf(-1, 0, 1, 2, 3, 4, 5, 6, 7))

        assertThat(pairs.keys.toList().map { it.toString() }).isEqualTo(listOf("A|B", "B|C", "C|A"))
        assertWith(pairs.values, { p ->
            assertThat(p.map { it.v1 }).isEqualTo(listOf("A", "B", "C"))
            assertThat(p.map { it.v2 }).isEqualTo(listOf("B", "C", "A"))
            assertThat(p.map { it.freq() }).isEqualTo(listOf(3, 3, 2))
            assertThat(p.map { it.positions }).isEqualTo(listOf(listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5)))
        })
    }

//    @Test
//    fun `inner workings iterate`() {
//        val items = listOf("A", "B", "C", "A", "B", "C", "A", "B", "C")
//        val rePair = RePairCompress()
//        val (sequence, pairs) = rePair.buildPairsAndSequence(items)
//
//        rePair.iterate(sequence, pairs, pairs["A" to "B"]!!, "X", PriorityQueue())
//
//        assertThat(sequence.map { it.value }).isEqualTo(listOf("X", null, "C", "X", null, "C", "X", null, "C"))
//        assertThat(m(sequence) { it.nextValidItem }).isEqualTo(listOf(2, null, 3, 5, null, 6, 8, null, -1))
//        assertThat(m(sequence) { it.previousValidItem }).isEqualTo(listOf(-1, null, 0, 2, null, 3, 5, null, 6))
//
//        pairs.forEach { (t, u) ->
//            println("\t$t : $u")
//        }
//
//        assertThat(pairs.keys.toList()).isEqualTo(listOf("X" to "C"))
//        assertWith(pairs.values, { p ->
//            assertThat(p.map { it.v1 }).isEqualTo(listOf("X"))
//            assertThat(p.map { it.v2 }).isEqualTo(listOf("C"))
//            assertThat(p.map { it.freq() }).isEqualTo(listOf(3))
//            assertThat(p.map { it.positions }).isEqualTo(listOf(listOf(0, 3, 6)))
//        })
//    }

    @Test
    fun `inner workings build and iterate with overlaps`() {
        val items = listOf("T", "T", "T", "T", "T", "T")
        val rePair = RePairCompress()
        val (sequence, pairs) = rePair.buildPairsAndSequence(items)

        assertThat(sequence.map { it.value.toString() }).isEqualTo(listOf("T", "T", "T", "T", "T", "T"))
//        assertThat(m(sequence) { it.nextPair }).isEqualTo(listOf(2, -1, 4, -1, -1, -1))
//        assertThat(m(sequence) { it.previousPair }).isEqualTo(listOf(-1, -1, 0, -1, 2, -1))
//        assertThat(m(sequence) { it.nextValidItem }).isEqualTo(listOf(1, 2, 3, 4, 5, -1))
//        assertThat(m(sequence) { it.previousValidItem }).isEqualTo(listOf(-1, 0, 1, 2, 3, 4))
//
//        rePair.iterate(sequence, pairs, pairs["T" to "T"]!!, "X", PriorityQueue())
//
//        assertThat(sequence.map { it.value }).isEqualTo(listOf("X", null, "X", null, "X", null))
//        assertThat(m(sequence) { it.nextPair }).isEqualTo(listOf(3, null, 5, 6, null, -1, -1, null, -1))
//        assertThat(m(sequence) { it.previousPair }).isEqualTo(listOf(-1, null, -1, 0, null, 2, 3, null, -1))
//        assertThat(m(sequence) { it.nextValidItem }).isEqualTo(listOf(2, null, 3, 5, null, 6, 8, null, -1))
//        assertThat(m(sequence) { it.previousValidItem }).isEqualTo(listOf(-1, null, 0, 2, null, 3, 5, null, 6))
//
//        assertThat(pairs.keys.toList()).isEqualTo(listOf("X" to "C", "C" to "X"))
//        assertWith(pairs.values, { p ->
//            assertThat(p.map { it.v1 }).isEqualTo(listOf("X", "C"))
//            assertThat(p.map { it.v2 }).isEqualTo(listOf("C", "X"))
//            assertThat(p.map { it.freq }).isEqualTo(listOf(3, 2))
//            assertThat(p.map { it.firstPosition }).isEqualTo(listOf(0, 2))
//            assertThat(p.map { it.lastPosition }).isEqualTo(listOf(6, 5))
//        })
    }

    private fun m(sequence: MutableList<RSeq<String>>, supplier: (RSeq<String>) -> Int?) =
        sequence.map { if (it.value == null) null else supplier.invoke(it) }

    @Test
    fun `String variation with overlapping repetition`() {
        val result = RePairCompress().compress(
            listOf("test", "test", "test", "test", "test", "test")
        )
        assertThat(result.pairs).hasSize(1).contains(PairToken(Token("test") to Token("test")))
        assertThat(result.compressed).isEqualTo(listOf("[Pair 0]", "[Pair 0]", "[Pair 0]"))
    }

    @Test
    fun `String variation 2`() {
        val result = RePairCompress().compress(
            listOf("A", "B", "C", "A", "B", "C", "A", "B", "C")
        )
        val aToB = PairToken(Token("A") to Token("B"))
        val c = Token("C")
        val p1ToC = PairToken(aToB to c)
        assertThat(result.pairs).hasSize(2)
            .contains(aToB)
            .contains(p1ToC)
        assertThat(result.compressed).isEqualTo(listOf("[Pair 1]", "[Pair 1]", "[Pair 1]"))
    }

//    @Test
//    fun `bigger test`() {
//        val items = generateSequence { "A" }.take(12).toList()
//        val rePair = RePairCompress()
//        val (sequence, pairs) = rePair.buildPairsAndSequence(items)
//
//        val aToA = PairToken(Token("A") to Token("A"))
//        rePair.iterate(sequence, pairs, pairs[aToA]!!, "X", PriorityQueue())
//
//
//        assertThat(sequence.map { it.value }).isEqualTo(listOf("X", null, "X", null, "X", null, "X", null, "X", null, "X", null))
//        assertThat(m(sequence) { it.nextValidItem }).isEqualTo(listOf(2, null, 4, null, 6, null, 8, null, 10, null, -1, null))
//        assertThat(m(sequence) { it.previousValidItem }).isEqualTo(listOf(-1, null, 0, null, 2, null, 4, null, 6, null, 8, null))
//
//        val compressed = rePair.compress(items)
//        assertThat(compressed.compressed).isEqualTo(listOf("[Pair 1]","[Pair 1]","[Pair 1]"))
//        assertThat(compressed.pairs).hasSize(2)
////            .containsEntry("[Pair 0]", "A" to "A")
////            .containsEntry("[Pair 1]", "[Pair 0]" to "[Pair 0]")
//    }

    @Test
    fun `large test`() {
        val items = generateSequence { "A" }.take(100).toList()
        val rePair = RePairCompress()

        val compressed = rePair.compress(items)
        assertThat(compressed.compressed).isEqualTo(listOf("[Pair 4]", "[Pair 4]", "[Pair 4]", "[Pair 1]"))
        assertThat(compressed.pairs).hasSize(5)
//            .containsEntry("[Pair 0]", "A" to "A")
//            .containsEntry("[Pair 1]", "[Pair 0]" to "[Pair 0]")
//            .containsEntry("[Pair 2]", "[Pair 1]" to "[Pair 1]")
//            .containsEntry("[Pair 3]", "[Pair 2]" to "[Pair 2]")
//            .containsEntry("[Pair 4]", "[Pair 3]" to "[Pair 3]")
    }
}