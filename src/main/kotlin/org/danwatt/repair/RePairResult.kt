package org.danwatt.repair



sealed class OutToken<T> {
    abstract fun allTokens() : List<T>
}
class Token<T>(val token: T) : OutToken<T>() {
    override fun allTokens(): List<T> = listOf(token)
    override fun toString(): String = token.toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token<*>

        return token == other.token
    }

    override fun hashCode(): Int {
        return token?.hashCode() ?: 0
    }


}

class PairToken<T>(val pair: Pair<OutToken<T>,OutToken<T>>) : OutToken<T>() {
    override fun allTokens(): List<T> = pair.first.allTokens() + pair.second.allTokens()
    override fun toString(): String = allTokens().joinToString("|")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PairToken<*>

        return pair == other.pair
    }

    override fun hashCode(): Int = pair.hashCode()


}

data class RePairResult<T>(
    val compressed: List<OutToken<T>>,
    val pairs: List<PairToken<T>>,
)