package org.danwatt.repair

class RePairDecompress {

    fun <T> decompress(compressed: List<T>, pairs: Map<T, Pair<T, T>>): List<T> = compressed.flatMap { token ->
        decodeToken(pairs, token)
    }

    private fun <T> decodeToken(pairs: Map<T, Pair<T, T>>, token: T): List<T> {
        val p = pairs[token]
        return if (null == p) {
            listOf(token)
        } else {
            decodeToken(pairs, p.first) + decodeToken(pairs, p.second)
        }
    }
}