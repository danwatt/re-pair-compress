package org.danwatt.repair

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class VarByteCodecTest {
    @Test
    fun test() {
        val codec = VarByteCodec(0x55u)
        /*
        68AF/13AF : Standard lookup  : Genesis
        C564/7064 : Two byte compressed  : % | In | the (3 words)
        5A1D/051D:  Standard lookup : beginning
        BC44/6744 : Two byte compressed  : God | created (2 words)
        03 : Single byte lookup : the
        C961/7461 : Two byte compressed : heaven | and | the | earth (4 words)
        ECFA/97FA : Two byte compressed : . | @ | And | the | earth (5 words)
        BF5E/6A5E : Two byte compressed  : was | without (2 words)
        67A5/12A5 : Standard lookup : form
        EAE4/95E4 : Two byte compressed  : , | and | void (3 words)
        0C : Single byte lookup : ; | and
        DD3F/883F : Two byte compressed : darkness | was (2 words)
        A4F7/4FF7 : Two byte compressed  : upon | the | face | of | the (5 words)
         */
        val ints = uintArrayOf(
            0x13AFu, 0x7064u, 0x051Du, 0x6744u,
            0x03u,
            0x7461u, 0x97FAu, 0x6A5Eu, 0x12A5u, 0x95E4u,
            0x0Cu,
            0x883Fu, 0x4FF7u
        )

        val bytes = codec.encode(ints)
        assertThat(bytes.toList()).containsExactly(
            0x68u, 0xAFu, 0xC5u, 0x64u, 0x5Au, 0x1Du, 0xBCu, 0x44u,
            0x03u, 0xC9u,
            0x61u, 0xECu, 0xFAu, 0xBFu, 0x5Eu, 0x67u, 0xA5u, 0xEAu, 0xE4u,
            0x0Cu,
            0xDDu, 0x3Fu, 0xA4u, 0xF7u,
        )

        val decoded = codec.decode(bytes)
        assertThat(decoded).containsAll(ints)
    }
}