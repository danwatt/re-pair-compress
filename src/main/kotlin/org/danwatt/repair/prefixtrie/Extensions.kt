package org.danwatt.repair.prefixtrie

fun String.flipFirstLetterCase(): String =
    if (this[0].isLowerCase()) {
        this.replaceFirstChar { it.uppercaseChar() }
    } else {
        this.replaceFirstChar { it.lowercaseChar() }
    }

fun String.differsOnlyByFirstCapitalization(other: String): Boolean =
    this.equals(other, ignoreCase = true) && this.substring(1) == other.substring(1)

