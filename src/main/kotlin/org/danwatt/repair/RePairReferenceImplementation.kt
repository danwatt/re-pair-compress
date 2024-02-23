package org.danwatt.repair

import java.util.Comparator
import java.util.PriorityQueue

val PRIORITY_COMPARATOR = Comparator.comparingInt(RPair<*>::freq)
    .reversed()
    .thenBy { it.v1.hashCode() }

data class RSeq<T>(
    var value: T?,
    //These next two effectively implement a double-linked-list for the input sequence.
    //When a pair is replaced, the second value in the pair is no longer needed
    var previousValidItem: Int = -1,
    var nextValidItem: Int = -1,
) {

}

data class RPair<T>(
    val v1: T,
    val v2: T,
    val positions: MutableList<Int> = mutableListOf(),
) : Comparable<RPair<T>> {
    fun freq() = positions.size
    override fun compareTo(other: RPair<T>): Int = PRIORITY_COMPARATOR.compare(this, other)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RPair<*>

        if (v1 != other.v1) return false
        if (v2 != other.v2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = v1?.hashCode() ?: 0
        result = 31 * result + (v2?.hashCode() ?: 0)
        return result
    }
}

class RePairReferenceImplementation {

    fun compress(
        input: List<String>,
        generatePairMarker: (Int, Pair<String, String>) -> String,
        stopAfterIterations: Int = Integer.MAX_VALUE,
    ): RePairResult<String> {
        val outputPairs = mutableMapOf<String, Pair<String, String>>()

        val (sequence, pairs) = buildPairsAndSequence(input)

        val queue: PriorityQueue<RPair<String>> = PriorityQueue(PRIORITY_COMPARATOR)
        queue.addAll(pairs.values.filter { it.positions.size >= 3 })

        var iterationNumber = 0
        while (queue.isNotEmpty()) {
            val highestPriority = queue.poll()
            if (highestPriority.positions.size < 3) {
                continue
            }
            val p = highestPriority.v1 to highestPriority.v2
            val newPairIdentifier = generatePairMarker.invoke(outputPairs.size, p)

            outputPairs[newPairIdentifier] = p

            iterate(sequence, pairs, highestPriority, newPairIdentifier, queue)

            iterationNumber++

            if (iterationNumber % 100 == 0) {
                println("Done with $iterationNumber")
            }
            if (iterationNumber == stopAfterIterations) {
                break
            }
        }


        return RePairResult(sequence.mapNotNull { it.value }.toList(), outputPairs)
    }

    private fun dumpSequence(sequence: MutableList<RSeq<String>>) {
        println("IX\tV\tPI\tNI")
        sequence.forEachIndexed { idx, item ->
            println("$idx\t${item.value ?: "❌"}\t${item.previousValidItem}\t${item.nextValidItem}")
        }
    }

    fun buildPairsAndSequence(items: List<String>): Pair<MutableList<RSeq<String>>, MutableMap<Pair<String, String>, RPair<String>>> {
        val sequence = mutableListOf<RSeq<String>>()
        val pairs = mutableMapOf<Pair<String, String>, RPair<String>>()

        var sequencePosition = 0
        while (sequencePosition < (items.size - 1)) {
            val p = items[sequencePosition] to items[sequencePosition + 1]
            var existingPair = pairs[p]
            val repeatSpecialCase = existingPair != null && existingPair.positions.last() == sequencePosition - 1
            if (existingPair == null) {
                existingPair = RPair(items[sequencePosition], items[sequencePosition + 1])
                existingPair.positions.add(sequencePosition)
                pairs[p] = existingPair
            } else if (!repeatSpecialCase) {
                existingPair.positions.add(sequencePosition)
            }

            val currentSequence = RSeq(items[sequencePosition])

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
        sequence.add(RSeq(items[lastIndex], previousValidItem = lastIndex - 1))
        return Pair(sequence, pairs)
    }

    fun iterate(
        sequence: MutableList<RSeq<String>>,
        pairs: MutableMap<Pair<String, String>, RPair<String>>,
        pairToReplace: RPair<String>,
        newPairIdentifier: String,
        queue: PriorityQueue<RPair<String>>,
    ) {

        val cp = pairToReplace.v1 to pairToReplace.v2

        val pairsToAddToQueue = mutableListOf<RPair<String>>()
        pairToReplace.positions.forEach { indexFirstPartOfPair ->
            val pairPart1 = sequence[indexFirstPartOfPair]
            val indexSecondPartOfPair = pairPart1.nextValidItem

            val pairPart2 = sequence[indexSecondPartOfPair]

            val currentPair = pairPart1.value to pairPart2.value
            assert(currentPair == cp)

            pairPart1.value = newPairIdentifier
            pairPart2.value = null

            val indexBeforeCurrentPair = pairPart1.previousValidItem
            val indexAfterCurrentPair = pairPart2.nextValidItem

            val oneBeforePair = if (indexBeforeCurrentPair == -1) null else sequence[indexBeforeCurrentPair]
            val oneAfterPair = if (indexAfterCurrentPair == -1) null else sequence[indexAfterCurrentPair]

            removeFromSeqLinkedList(pairPart1, pairPart2, oneAfterPair, indexFirstPartOfPair)

            // TODO: Working on eliminating the duplication here
            val secondValue = pairToReplace.v1
            val ix = indexBeforeCurrentPair
            val firstValue = oneBeforePair?.value

            if (firstValue != null) {
                val pairToDecrement = firstValue to secondValue
                if (pairToDecrement != currentPair) {
                    decrementPair(pairs, pairToDecrement, ix)
                }
                val pairToAdd = firstValue to newPairIdentifier
                if (pairToAdd != currentPair) {
                    val p = incrementPair(
                        pairs = pairs,
                        pairToAdd = pairToAdd,
                        currentPosition = ix,
                        sequence = sequence
                    )

                    if (p != null) {
                        pairsToAddToQueue.add(p)
                    }
                }
            }


            if (oneAfterPair != null) {
                val pairToDecrement = pairToReplace.v2 to oneAfterPair.value!!
                if (pairToDecrement != currentPair) {
                    decrementPair(pairs, pairToDecrement, indexSecondPartOfPair)
                }
                val pairToAdd = newPairIdentifier to oneAfterPair.value!!
                if (pairToAdd != currentPair) {
                    val p = incrementPair(
                        pairs = pairs,
                        pairToAdd = pairToAdd,
                        currentPosition = indexFirstPartOfPair,
                        sequence = sequence
                    )
                    if (p != null) {
                        pairsToAddToQueue.add(p)
                    }
                }
            }
        }

        queue.addAll(pairsToAddToQueue.filter { it.positions.size >= 3 })
    }


    private fun incrementPair(
        pairs: MutableMap<Pair<String, String>, RPair<String>>,
        pairToAdd: Pair<String, String>,
        currentPosition: Int,
        sequence: MutableList<RSeq<String>>,
    ): RPair<String>? {
        var rp = pairs[pairToAdd]
        var created = false
        if (rp == null) {
            rp = RPair(pairToAdd.first, pairToAdd.second, positions = mutableListOf(currentPosition))
            pairs[pairToAdd] = rp
            created = true
        } else {
            if (rp.positions.isEmpty()) {
                rp.positions.add(currentPosition)
            } else {
                //Don't allow overlapping pairs
                val lastSequencePosition = rp.positions.last()
                val lastItem = sequence[lastSequencePosition]
                if (lastItem.nextValidItem != currentPosition) {
                    rp.positions.add(currentPosition)
                }
            }
        }
        return if (created) {
            rp
        } else {
            null
        }
    }

    private fun decrementPair(
        pairs: MutableMap<Pair<String, String>, RPair<String>>,
        p: Pair<String, String>,
        offsetForThisPair: Int,
    ) {
        pairs[p]?.apply {
            this.positions.remove(offsetForThisPair)
        }
    }

    private fun removeFromSeqLinkedList(
        current: RSeq<String>,
        toRemove: RSeq<String>,
        twoAhead: RSeq<String>?,
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