package org.danwatt.repair

import java.io.IOException
import java.io.OutputStream

class BitOutputStream(val output: OutputStream) {
    private var currentByte: Int
    private var bitsInCurrentByte: Int

    init {
        currentByte = 0
        bitsInCurrentByte = 0
    }

    @Throws(IOException::class)
    fun write(b: Boolean) {
        val t = if (b) 1 else 0
        currentByte = currentByte shl 1 or t
        bitsInCurrentByte++
        if (bitsInCurrentByte == 8) {
            output.write(currentByte)
            bitsInCurrentByte = 0
        }
    }

    fun write(ub: UByte) {
        (0..7).forEach { i->
            val mask = 1 shl i
            write(ub.and(mask.toUByte()) > 0u)
        }
    }

    @Throws(IOException::class)
    fun close() {
        while (bitsInCurrentByte != 0) write(false)
        output.close()
    }
}