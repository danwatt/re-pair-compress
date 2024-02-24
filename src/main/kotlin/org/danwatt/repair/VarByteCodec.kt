package org.danwatt.repair


fun UShort.upperUByte(): UByte = this.toUInt().shr(8).toUByte()
fun UShort.lowerUByte(): UByte = this.toUByte()
class VarByteCodec() {
    fun encode(ints: UIntArray) : UByteArray{

        //0x55 = 85
        val l = mutableListOf<UByte>()
        ints.forEach { i->
            if (i <= 127u) {
                l.add(i.toUByte())
            } else {

            }
        }
        return l.toUByteArray()
    }

    fun decode(ubytes: UByteArray) :UIntArray{
        return UIntArray(0)
    }
}