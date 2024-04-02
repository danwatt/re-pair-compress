package org.danwatt.repair

import java.util.Comparator
import java.util.PriorityQueue

val PRIORITY_COMPARATOR = Comparator.comparingInt(RPair<*>::freq)
    .reversed()
    .thenBy { it.v1.hashCode() }

data class RSeq<T>(
    var value: OutToken<T>?,
    //These next two effectively implement a double-linked-list for the input sequence.
    //When a pair is replaced, the second value in the pair is no longer needed
    var previousValidItem: Int = -1,
    var nextValidItem: Int = -1,
)

data class RPair<T>(
    val v1: OutToken<T>,
    val v2: OutToken<T>,
    val positions: MutableList<Int> = mutableListOf(),
) : Comparable<RPair<T>> {
    fun freq() = positions.size
    override fun compareTo(other: RPair<T>): Int = PRIORITY_COMPARATOR.compare(this, other)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RPair<*>
        return when {
            v1 != other.v1 -> false
            v2 != other.v2 -> false
            else -> true
        }
    }

    override fun hashCode(): Int = (31 * v1.hashCode() + v2.hashCode())
}

val minPairs = 4

class RePairCompress {

    fun <T> compress(
        input: List<T>,
        stopAfterIterations: Int = Integer.MAX_VALUE,
        progress: Boolean = true,
    ): RePairResult<T> {
        val outputPairs = mutableMapOf<PairToken<T>, PairToken<T>>()

        val (sequence, pairs) = buildPairsAndSequence(input)

        val queue: PriorityQueue<RPair<T>> = PriorityQueue(PRIORITY_COMPARATOR)
        queue.addAll(pairs.values.filter { it.positions.size >= minPairs })

        var iterationNumber = 0
        while (queue.isNotEmpty()) {
            val highestPriority = queue.poll()
            if (highestPriority.positions.size < minPairs) {
                continue
            }
            val newPairIdentifier =
                PairToken(highestPriority.v1 to highestPriority.v2)//generatePairMarker.invoke(outputPairs.size, p)

            outputPairs[newPairIdentifier] = newPairIdentifier

            iterate(sequence, pairs, highestPriority, newPairIdentifier, queue)

            iterationNumber++

            if (progress && iterationNumber % 100 == 0) {
                println("Done with $iterationNumber")
            }
            if (iterationNumber == stopAfterIterations) {
                break
            }
        }

        return RePairResult(sequence.mapNotNull { it.value }.toList(), outputPairs.keys.toList())
    }

    fun <T> buildPairsAndSequence(items: List<T>): Pair<MutableList<RSeq<T>>, MutableMap<PairToken<T>, RPair<T>>> {
        val sequence = mutableListOf<RSeq<T>>()
        val pairs = mutableMapOf<PairToken<T>, RPair<T>>()

        var sequencePosition = 0
        while (sequencePosition < (items.size - 1)) {
            val p = PairToken(Token(items[sequencePosition]) to Token(items[sequencePosition + 1]))
            var existingPair = pairs[p]
            val repeatSpecialCase = existingPair != null && existingPair.positions.last() == sequencePosition - 1
            if (existingPair == null) {
                existingPair = RPair(Token(items[sequencePosition]), Token(items[sequencePosition + 1]))
                existingPair.positions.add(sequencePosition)
                pairs[p] = existingPair
            } else if (!repeatSpecialCase) {
                existingPair.positions.add(sequencePosition)
            }

            val currentSequence = RSeq(Token(items[sequencePosition]))

            if (sequencePosition > 0) {
                currentSequence.previousValidItem = sequencePosition - 1
            }
            if (sequencePosition < items.size) {
                currentSequence.nextValidItem = sequencePosition + 1
            }
            sequence.add(currentSequence)

            sequencePosition++
        }
        //Special case for the last item
        val lastIndex = items.size - 1
        sequence.add(RSeq(Token(items[lastIndex]), previousValidItem = lastIndex - 1))
        return Pair(sequence, pairs)
    }

    fun <T> iterate(
        sequence: MutableList<RSeq<T>>,
        pairs: MutableMap<PairToken<T>, RPair<T>>,
        pairToReplace: RPair<T>,
        newPairIdentifier: PairToken<T>,
        queue: PriorityQueue<RPair<T>>,
    ) {
        val pairsToAddToQueue = mutableListOf<RPair<T>>()
        pairToReplace.positions.forEach { indexFirstPartOfPair ->
            val pairPart1: RSeq<T> = sequence[indexFirstPartOfPair]
            val indexSecondPartOfPair = pairPart1.nextValidItem

            val pairPart2: RSeq<T> = sequence[indexSecondPartOfPair]

            val currentPair: PairToken<T> = PairToken(pairPart1.value!! to pairPart2.value!!)

            pairPart1.value = newPairIdentifier
            pairPart2.value = null

            val indexBeforeCurrentPair = pairPart1.previousValidItem
            val indexAfterCurrentPair = pairPart2.nextValidItem

            val oneBeforePair = if (indexBeforeCurrentPair == -1) null else sequence[indexBeforeCurrentPair]
            val oneAfterPair = if (indexAfterCurrentPair == -1) null else sequence[indexAfterCurrentPair]

            removeFromSeqLinkedList(pairPart1, pairPart2, oneAfterPair, indexFirstPartOfPair)

            val firstValue = oneBeforePair?.value

            if (firstValue != null) {
                addRemove(
                    pairToDecrement = PairToken(firstValue to pairToReplace.v1),
                    pairToAdd = PairToken(firstValue to newPairIdentifier),
                    currentPair = currentPair,
                    pairs = pairs,
                    position1 = indexBeforeCurrentPair,
                    position2 = indexBeforeCurrentPair,
                    sequence = sequence,
                    pairsToAddToQueue = pairsToAddToQueue
                )
            }


            if (oneAfterPair != null) {
                addRemove(
                    pairToDecrement = PairToken(pairToReplace.v2 to oneAfterPair.value!!),
                    pairToAdd = PairToken(newPairIdentifier to oneAfterPair.value!!),
                    currentPair = currentPair,
                    pairs = pairs,
                    position1 = indexSecondPartOfPair,
                    position2 = indexFirstPartOfPair,
                    sequence = sequence,
                    pairsToAddToQueue = pairsToAddToQueue
                )
            }
        }

        queue.addAll(pairsToAddToQueue.filter { it.positions.size >= minPairs })
    }

    private fun <T> addRemove(
        pairToDecrement: PairToken<T>,
        pairToAdd: PairToken<T>,
        currentPair: PairToken<T>,
        pairs: MutableMap<PairToken<T>, RPair<T>>,
        position1: Int,
        position2: Int,
        sequence: MutableList<RSeq<T>>,
        pairsToAddToQueue: MutableList<RPair<T>>,
    ) {
        if (pairToDecrement != currentPair) {
            decrementPair(pairs, pairToDecrement, position1)
        }

        if (pairToAdd != currentPair) {
            val p = incrementPair(
                pairs = pairs,
                pairToAdd = pairToAdd,
                currentPosition = position2,
                sequence = sequence
            )

            if (p != null) {
                pairsToAddToQueue.add(p)
            }
        }
    }


    private fun <T> incrementPair(
        pairs: MutableMap<PairToken<T>, RPair<T>>,
        pairToAdd: PairToken<T>,
        currentPosition: Int,
        sequence: MutableList<RSeq<T>>,
    ): RPair<T>? {
        var rp = pairs[pairToAdd]
        if (rp == null) {
            rp = RPair(pairToAdd.pair.first, pairToAdd.pair.second, positions = mutableListOf(currentPosition))
            pairs[pairToAdd] = rp
            return rp
        } else if (rp.positions.isEmpty()) {
            rp.positions.add(currentPosition)
        }
        //Don't allow overlapping pairs
        else if (sequence[rp.positions.last()].nextValidItem != currentPosition) {
            rp.positions.add(currentPosition)
        }
        return null

    }

    private fun <T> decrementPair(
        pairs: MutableMap<PairToken<T>, RPair<T>>,
        p: PairToken<T>,
        offsetForThisPair: Int,
    ) {
        pairs[p]?.apply {
            this.positions.remove(offsetForThisPair)
        }
    }

    private fun <T> removeFromSeqLinkedList(
        current: RSeq<T>,
        toRemove: RSeq<T>,
        twoAhead: RSeq<T>?,
        currentPosition: Int,
    ) {
        if (null != twoAhead) {
            current.nextValidItem = toRemove.nextValidItem
            twoAhead.previousValidItem = currentPosition
        } else {
            current.nextValidItem = -1
        }
        toRemove.value = null
        toRemove.nextValidItem = -1
        toRemove.previousValidItem = -1
    }
}