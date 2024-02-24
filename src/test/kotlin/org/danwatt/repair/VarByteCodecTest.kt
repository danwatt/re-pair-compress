package org.danwatt.repair

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class VarByteCodecTest {
    @Test
    fun test() {
        val codec = VarByteCodec()
        val ints = UIntArray(2)
        ints[0] = 0x127u
        ints[1] = 0x128u

        val bytes = codec.encode(ints)
        assertThat(bytes).hasSize(3)
        assertThat(bytes.toList()).containsExactly(65u)
    }
}