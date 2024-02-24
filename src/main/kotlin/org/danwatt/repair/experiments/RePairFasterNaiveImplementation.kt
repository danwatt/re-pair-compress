package org.danwatt.repair.experiments

import org.danwatt.repair.RePairResult
import java.util.*
import kotlin.collections.ArrayList

class RePairFasterNaiveImplementation<T> {
    @Suppress("UNUSED_PARAMETER")
    private fun doNothingCallback(
        iteration: Int,
        working: List<T?>,
        pairs: Map<T, Pair<T, T>>,
        currentPairToken: T,
        currentPair: CandidatePair<T>,
    ) {

    }

    fun compress(
        input: Collection<T>,
        generatePairMarker: (Int, Pair<T, T>) -> T,
        stopAfterIterations: Int = Integer.MAX_VALUE,
        callback: (Int, List<T?>, Map<T, Pair<T, T>>, T, CandidatePair<T>) -> Unit = ::doNothingCallback,
    ): RePairResult<T> {
        val working = ArrayList<T?>(input)
        val removedItems = BitSet(input.size)

        val queue = PriorityQueue(
            Comparator.comparingInt(CandidatePair<T>::count).reversed().thenBy { it.v1.hashCode() }
        )

        val initialPairs = buildInitialPairs(working)
        queue.addAll(initialPairs.keys.filter { it.count >= 3 })

        var pairNumber = 0
        val outputPairs = mutableMapOf<T, Pair<T, T>>()
        var passNumber = 0

        val offsetsThatChanged = IntArray(working.size) { -1 }

        while (queue.isNotEmpty() && passNumber < stopAfterIterations) {
            val currentPair = queue.poll()
            val currentPairAsPair = currentPair.v1 to currentPair.v2
            passNumber++
            val currentPairToken = generatePairMarker(pairNumber, currentPairAsPair)//Pass number or pair number?

            if (currentPair.pairNumber == -1) {
                pairNumber++
                currentPair.pairNumber = pairNumber
                outputPairs[currentPairToken] = currentPairAsPair
            }

            innerLoop(removedItems, working, currentPair, offsetsThatChanged, currentPairToken)

            addNewPairs(offsetsThatChanged, working, currentPairToken, queue, removedItems)
            callback(passNumber, working, outputPairs, currentPairToken, currentPair)
        }

        return RePairResult(working.mapNotNull { it }, outputPairs)
    }

    private fun innerLoop(
        removedItems: BitSet,
        working: ArrayList<T?>,
        current: CandidatePair<T>,
        offsetsThatChanged: IntArray,
        currentPairToken: T,
    ): Int {
        var offset1 = current.firstOffset
        var numChanged = 0
        val currentHash = current.v1.hashCode() + 31 * current.v2.hashCode()

        while (true) {
            val next = removedItems.nextClearBit(offset1 + 1)
            if (next < 0 || next >= working.size || next > current.lastOffset) {
                break
            }

            val v1: T? = working[offset1]
            val v2: T? = working[next]
            val newHash = (v1?.hashCode() ?: 0) + (31 * (v2?.hashCode() ?: 0))
            val pairsMatch = currentHash == newHash
                    && v1 == current.v1
                    && v2 == current.v2
            if (pairsMatch) {
                offsetsThatChanged[numChanged++] = offset1
                working[offset1] = currentPairToken
                working[next] = null
                removedItems.set(next)
                offset1 += 2
            } else {
                offset1++
            }
        }
        return offset1
    }

    private fun addNewPairs(
        offsetsThatChanged: IntArray,
        working: ArrayList<T?>,
        currentPairToken: T,
        queue: PriorityQueue<CandidatePair<T>>,
        removedItems: BitSet,
    ) {
        val newPairs = mutableMapOf<CandidatePair<T>, CandidatePair<T>>()
        var t = 0
        offsetsThatChanged.takeWhile { it >= 0 }.forEach { pairOffset ->
            removedItems.previousClearBit(pairOffset - 1)
                .takeIf { it > -1 }?.let {
                    addPair(working[it], currentPairToken, it, newPairs)
                }
            removedItems.nextClearBit(pairOffset + 2)
                .takeIf { it > -1 && it < working.size }?.let {
                    addPair(currentPairToken, working[it], pairOffset, newPairs)
                }
            offsetsThatChanged[t++] = -1
        }
        queue.addAll(newPairs.keys.filter { it.count >= 3 })
    }

    private fun buildInitialPairs(working: List<T?>): MutableMap<CandidatePair<T>, CandidatePair<T>> {
        val pairs = mutableMapOf<CandidatePair<T>, CandidatePair<T>>()
        var i = 0
        var previousCandidate: CandidatePair<T>? = null
        while (i < working.size - 1) {
            if (previousCandidate != null) {
                //Don't allow candidates to overlap. Ie: [A A] A and A [A A] are not allowed. The second is rejected
                if (previousCandidate.lastOffset == i - 1 && previousCandidate.sameAs(working[i]!! to working[i + 1]!!)) {
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
        v1: T?,
        v2: T?,
        offset: Int,
        pairs: MutableMap<CandidatePair<T>, CandidatePair<T>>,
    ): CandidatePair<T> {
        assert(v1 != null)
        assert(v2 != null)
        val p: CandidatePair<T> = CandidatePair(v1!!, v2!!, offset, offset, 1)
        val existing = pairs[p]
        return if (existing == null) {
            pairs[p] = p
            p
        } else {
            if (existing.lastOffset == offset) {
                return existing
            }
            existing.also {
                it.lastOffset = offset
                it.count++
            }
        }

    }
}