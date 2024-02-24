package org.danwatt.repair

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

class BitInputStream(val input: InputStream) {
    private var nextBits = 0
    private var bitsRemaining: Int
    private var isEndOfStream: Boolean


    init {
        bitsRemaining = 0
        isEndOfStream = false
    }

    @Throws(IOException::class)
    fun read(): Int {
        if (isEndOfStream) return -1
        if (bitsRemaining == 0) {
            nextBits = input.read()
            if (nextBits == -1) {
                isEndOfStream = true
                return -1
            }
            bitsRemaining = 8
        }
        bitsRemaining--
        return (nextBits ushr bitsRemaining) and 1
    }


    @Throws(IOException::class)
    fun readNoEof(): Int {
        val result = read()
        if (result != -1) return result
        else throw EOFException("End of stream reached")
    }

    @Throws(IOException::class)
    fun close() {
        input.close()
    }
}