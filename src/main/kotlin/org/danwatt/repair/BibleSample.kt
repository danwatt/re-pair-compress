package org.danwatt.repair

import kotlin.io.path.Path
import kotlin.io.path.writeLines

fun callback(
    iteration: Int,
    working: List<String?>,
    pairs: Map<String, Pair<String, String>>,
    currentPair: String,
    candidatePair: CandidatePair<String>,
) {
    if (iteration % 50 == 0 || iteration <= 128) {
        println("$iteration: Done with $currentPair ${decodePair(currentPair, pairs)} (${candidatePair.count} times)")
    }
}

fun decodePair(token: String, pairs: Map<String, Pair<String, String>>): String {
    return if (token.startsWith("-PAIR ")) {
        "(${decodePair(pairs[token]!!.first, pairs)}|${decodePair(pairs[token]!!.second, pairs)})"
    } else {
        token
    }
}

fun pairGeneratorDetailed(i: Int, p: Pair<String, String>): String = "<P $i (${p.first}|${p.second})>"

fun pairGeneratorMiddleGround(i: Int, p: Pair<String, String>): String {
    val first = p.first.substringAfter("(").substringBefore(")")
    val second = p.second.substringAfter("(").substringBefore(")")
    return "[$i ($first|$second)]"
}

fun pairGenerator(i: Int, p: Pair<String, String>): String = "<P $i>"

fun main() {
    val tokens = BibleParser().readTranslation()

    val sample = tokens
    sample.forEach { it.hashCode() }

    val distinct = tokens.distinct().size
    val maxIterations = 65536 - distinct
    println("Running for a max of $maxIterations")

    val results = RePairReferenceImplementation().compress(
        input = sample,
        generatePairMarker = ::pairGeneratorMiddleGround,
//        callback = ::callback,
        stopAfterIterations = maxIterations
    )


    //30k: 996826
    //40k: 921518
    /*
        val compressor = RePairReferenceImplementation()
        val results = compressor.compress(
            input = sample,
            generatePairMarker = ::pairGenerator,
            stopAfterIterations = maxIterations
    //        stopAfterIterations = maxIterations,
    //        callback = ::callback
        )

        val decompressed = RePairDecompress().decompress(results.compressed, results.pairs)
        if (decompressed.size != tokens.size) {
            throw RuntimeException("Decompressed size of ${decompressed.size} != original size of ${tokens.size}")
        }
        decompressed.forEachIndexed { index, s ->
            val original = tokens[index]
            if (original != s) {
                throw RuntimeException("Mis-matched decompression at $index")
            }
        }

    */

    println("Before: " + sample.take(500).joinToString(" "))
    println("${sample.size} to ${results.compressed.size} (ratio ${(sample.size.toDouble() / results.compressed.size)}:1)")
    println("After: " + results.compressed.take(500).joinToString(" "))

    val bytesNeededForTokens = results.compressed.size * 2
    val bytesNeededForPairs = results.pairs.size * 2
    println("Distinct input tokens: ${distinct}")
    println("Pairs generated: ${results.pairs.size}")
    println("Storage requirements: ${bytesNeededForPairs + bytesNeededForTokens}")

    val gr = results.compressed.groupBy { it }

    val tokenListing = results.pairs.map { p -> "# ${p.key}=${p.value.first}|${p.value.second}(${gr[p.key]?.size?:0})" }

    Path("/tmp/bible-repair.txt").writeLines(tokenListing + results.compressed)

    val stopTokens = listOf(
        "!",
        ",",
        "-VERSE-",
        ".",
        ":",
        ";",
        "a",
        "and",
        "as",
        "but",
        "for",
        "in",
        "of",
        "of",
        "or",
        "that",
        "the",
        "to",
        "upon",
        "with",
    ).map { it.lowercase() }

    val g = results.compressed.filter { it.startsWith("<P") }.groupBy { it }
    val top128 = g.values.sortedByDescending { it.size }.take(128).sumOf { it.size }
    println("Top 128 account for $top128")
}