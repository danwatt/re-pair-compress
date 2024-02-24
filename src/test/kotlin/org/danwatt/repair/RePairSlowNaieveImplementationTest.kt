package org.danwatt.repair

import org.assertj.core.api.Assertions.assertThat
import org.danwatt.repair.experiments.RePairSlowNaieveImplementation
import org.junit.jupiter.api.Test

class RePairSlowNaieveImplementationTest {
    val compressor = RePairSlowNaieveImplementation<Int>()

    fun pairMarkerGenerator(pairNumber: Int): Int {
        return 100 + pairNumber
    }

    fun pairMarkerGeneratorString(pairNumber: Int): String {
        return "[Pair $pairNumber]"
    }

    @Test
    fun `do not compress when there are fewer than 3 pairs`() {
        val result = compressor.compress(listOf(1, 1, 1, 1), -1, ::pairMarkerGenerator)
        assertThat(result.pairs).isEmpty()
        assertThat(result.compressed).isEqualTo(listOf(1, 1, 1, 1))
    }

    @Test
    fun `three repetitions of the same pair but no recursion`() {
        val result = compressor.compress(listOf(1, 1, 1, 1, 1, 1), -1, ::pairMarkerGenerator)
        assertThat(result.pairs).hasSize(1).containsEntry(100, 1 to 1)
        assertThat(result.compressed).isEqualTo(listOf(100, 100, 100))
    }

    @Test
    fun `String variation`() {
        val result = RePairSlowNaieveImplementation<String>().compress(
            listOf("test", "test", "test", "test", "test", "test"),
            "[marker]",
            ::pairMarkerGeneratorString
        )
        assertThat(result.pairs).hasSize(1).containsEntry("[Pair 0]", "test" to "test")
        assertThat(result.compressed).isEqualTo(listOf("[Pair 0]", "[Pair 0]", "[Pair 0]"))
    }

    @Test
    fun `two layers of recursion`() {
        /*
        1 1 1 1 1 1 1 1 1 1 1 1
        2   2   2   2   2   2
        3       3       3
         */
        val result = compressor.compress(
            listOf(
                1, 1,//1
                1, 1,
                1, 1,//2
                1, 1,
                1, 1,//3
                1, 1
            ), -1, ::pairMarkerGenerator
        )
        assertThat(result.compressed).isEqualTo(listOf(101, 101, 101))
        assertThat(result.pairs).hasSize(2)
            .containsEntry(100, 1 to 1)
            .containsEntry(101, 100 to 100)
    }
}