package org.danwatt.repair.prefixtrie


@OptIn(ExperimentalStdlibApi::class)
fun CharArray.hexify(): List<String> = this.map { it ->
    if (it.code <= 31) {
        it.code.toUByte().toHexString(format = HexFormat.UpperCase)
    } else {
        it.toString()
    }
}

@OptIn(ExperimentalStdlibApi::class)
fun CharArray.hexify2(): List<String> = this.joinToString("") { c ->

    if (c.code <= 31) {
        " 0x" + c.code.toUByte().toHexString(format = HexFormat.UpperCase) + " "
    } else {
        c.toString()
    }
}.split(" ").filter { it.isNotBlank() }


@OptIn(ExperimentalStdlibApi::class)
fun CharArray.hexify3(): String = this.joinToString("") { c ->
    if (c.code <= 31) {
        "[" + c.code.toUByte().toHexString(format = HexFormat.UpperCase) + "]"
    } else {
        c.toString()
    }
}
