package org.danwatt.repair


fun UShort.upperUByte(): UByte = this.toUInt().shr(8).toUByte()
fun UShort.lowerUByte(): UByte = this.toUByte()

@ExperimentalUnsignedTypes
class VarByteCodec(val cutoff: UByte) {
    fun encode(ints: UIntArray): UByteArray {

        //0x55 = 85
        val l = mutableListOf<UByte>()
        ints.forEach { i ->
            if (i <= cutoff) {
                l.add(i.toUByte())
            } else {
                val u = i.toUShort().upperUByte() + cutoff
                if (u > 0xFFu) {
                    throw IllegalArgumentException("Too big to fit in a UByte: $u" )
                }
                val upper = u.toUByte()
                val lower = i.toUShort().lowerUByte()
                l.add(upper)
                l.add(lower)
            }
        }
        return l.toUByteArray()
    }

    fun decode(ubytes: UByteArray): UIntArray {
        val ints = mutableListOf<UInt>()
        var i = 0
        while (i < ubytes.size) {
            val b = ubytes[i]
            if (b < cutoff) {
                ints.add(b.toUInt())
                i++
            } else {
                ints.add((b - cutoff).shl(8) + ubytes[i + 1])
                i += 2
            }
        }
        return ints.toUIntArray()
    }
}