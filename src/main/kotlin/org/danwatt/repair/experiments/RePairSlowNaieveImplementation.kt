package org.danwatt.repair.experiments

import org.danwatt.repair.RePairResult
import java.util.*

class RePairSlowNaieveImplementation<T> {
    @Suppress("UNUSED_PARAMETER")
    fun doNothingCallback(
        iteration: Int,
        working: List<T>,
        pairs: Map<T, Pair<T, T>>,
        currentPairToken: T,
        currentPair: CandidatePair<T>,
    ) {

    }

    fun compress(
        input: Collection<T>,
        marker: T,
        pairMarkerGenerator: (Int) -> T,
        stopAfterIterations: Int = Integer.MAX_VALUE,
        callback: (Int, List<T>, Map<T, Pair<T, T>>, T, CandidatePair<T>) -> Unit = ::doNothingCallback,
    ): RePairResult<T> {
        assert(!input.any { it == marker })
        val working = LinkedList(input)
        val pairs = buildPairs(working)
        val queue = PriorityQueue(
            Comparator.comparingInt(CandidatePair<T>::count).reversed().thenBy { it.v1.hashCode() }
        )
        queue.addAll(pairs.keys.filter { it.count >= 3 })

        var pairNumber = 0
        val outputPairs = mutableMapOf<T, Pair<T, T>>()
        var passNumber = 0
        while (queue.isNotEmpty()) {
            passNumber++
            val currentPairToken = pairMarkerGenerator(pairNumber)
            pairs.clear()
            val current = queue.poll()
            if (current.pairNumber == -1) {
                pairNumber++
                current.pairNumber = pairNumber
                outputPairs[currentPairToken] = current.v1 to current.v2
            }
            var offset = current.firstOffset
            val offsetsToAddPairs = mutableListOf<Int>()

            while (offset <= current.lastOffset) {
                val v1 = working[offset]
                val next = working.indexOfNextNonMarker(offset + 1, marker)
                if (next < 0) {
                    break
                }
                val v2 = working[next]
                if (v1 == current.v1 && v2 == current.v2) {
                    offsetsToAddPairs.add(offset)
                    working[offset] = currentPairToken
                    working[next] = marker
                    offset += 2
                } else {
                    offset++
                    continue
                }
            }

            offsetsToAddPairs.forEach { pairOffset ->
                working.indexOfPreviousNonMarker(pairOffset - 1, marker)
                    .takeIf { it > -1 }?.let { addPair(working[it], currentPairToken, it, pairs) }
                working.indexOfNextNonMarker(pairOffset + 2, marker)
                    .takeIf { it > -1 }?.let { addPair(currentPairToken, working[it], pairOffset, pairs) }
            }

            queue.addAll(pairs.keys.filter { it.count >= 3 })
            callback(passNumber, working, outputPairs, currentPairToken, current)
        }

        val markersRemoved = working.filter { it != marker }
        return RePairResult(LinkedList<T>(markersRemoved), outputPairs)
    }

    private fun buildPairs(working: LinkedList<T>): MutableMap<CandidatePair<T>, CandidatePair<T>> {
        val pairs = mutableMapOf<CandidatePair<T>, CandidatePair<T>>()
        var i = 0
        var previousCandidate: CandidatePair<T>? = null
        while (i < working.size - 1) {
            if (previousCandidate != null) {
                if (previousCandidate.lastOffset == i - 1 && previousCandidate.sameAs(working[i] to working[i + 1])) {
                    i++
                    continue
                }
            }
            previousCandidate = addPair(working[i], working[i + 1], i, pairs)
            i++
        }
        return pairs
    }

    private fun addPair(
        v1: T,
        v2: T,
        offset: Int,
        pairs: MutableMap<CandidatePair<T>, CandidatePair<T>>,
    ): CandidatePair<T> {
        val p: CandidatePair<T> = CandidatePair(v1, v2, offset, offset, 1)
        if (pairs.containsKey(p)) {
            val existing = pairs[p]!!
            if (existing.lastOffset == offset) {
                return existing
            }
            existing.lastOffset = offset
            existing.count++
            return existing
        } else {
            pairs[p] = p
            return p
        }

    }
}

