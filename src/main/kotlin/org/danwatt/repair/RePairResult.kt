package org.danwatt.repair

data class RePairResult<T>(
    val compressed: List<T>,
    val pairs: Map<T, Pair<T, T>>,
)