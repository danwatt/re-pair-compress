package org.danwatt.repair

fun <T> List<T>.indexOfPreviousNonMarker(
    startAt: Int,
    marker: T,
    lastPositionToCheck : Int = 0
): Int {
    if (startAt < 0) {
        return -1
    }
    var j = startAt
    val stopAt = maxOf(0,lastPositionToCheck)
    while (j > stopAt) {
        if (this[j] != marker) {
            return j
        }
        j--
    }
    return -1
}

fun <T> List<T>.indexOfNextNonMarker(
    startAt: Int,
    marker: T,
    lastPositionToCheck : Int = Integer.MAX_VALUE
): Int {
    if (startAt >= this.size) {
        return -1
    }
    var j = startAt
    val stopAt = minOf(this.size,lastPositionToCheck)
    while (j < stopAt) {
        if (this[j] != marker) {
            return j
        }
        j++
    }
    return -1
}