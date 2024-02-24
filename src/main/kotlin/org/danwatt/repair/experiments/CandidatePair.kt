package org.danwatt.repair.experiments

data class CandidatePair<T>(
    val v1: T,
    val v2: T,
    val firstOffset: Int,
    var lastOffset: Int,
    var count: Int = 0,
    var pairNumber: Int = -1,
) {
    fun sameAs(other: Pair<T, T>): Boolean = other.first == v1 && other.second == v2

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CandidatePair<*>
        if (v1 != other.v1) return false
        if (v2 != other.v2) return false
        return true
    }


    override fun toString(): String =
        "CandidatePair($v1,$v2, count=$count, first=$firstOffset, last=$lastOffset)"

    override fun hashCode(): Int {
        var result = v1?.hashCode() ?: 0
        result = 31 * result + (v2?.hashCode() ?: 0)
        return result
    }
}