package org.danwatt.repair.bible

import org.danwatt.repair.RePairDecompress
import org.danwatt.repair.RePairReferenceImplementation


fun pairMarkerGenerator(i: Int, p: Pair<String, String>): String {
    val first = p.first.substringAfter("(").substringBefore(")")
    val second = p.second.substringAfter("(").substringBefore(")")
    return "[$i ($first|$second)]"
}

fun main() {
    val tokens = BibleParser().readTranslation()

    tokens.forEach { it.hashCode() }

    val distinct = tokens.distinct().size
    val maxIterations = 65536 - distinct
    println("Running for a max of $maxIterations")

    val results = RePairReferenceImplementation().compress(
        input = tokens,
        generatePairMarker = ::pairMarkerGenerator,
        stopAfterIterations = maxIterations
    )


    //30k: 996826
    //40k: 921518

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


    println("Before: " + tokens.take(500).joinToString(" "))
    println("${tokens.size} to ${results.compressed.size} (ratio ${(tokens.size.toDouble() / results.compressed.size)}:1)")
    println("After: " + results.compressed.take(500).joinToString(" "))

    val bytesNeededForTokens = results.compressed.size * 2
    val bytesNeededForPairs = results.pairs.size * 2
    println("Distinct input tokens: $distinct")
    println("Pairs generated: ${results.pairs.size}")
    println("Storage requirements: ${bytesNeededForPairs + bytesNeededForTokens}")

    val gr = results.compressed.groupBy { it }

    val tokenListing =
        results.pairs.map { p -> "# ${p.key}=${p.value.first}|${p.value.second}(${gr[p.key]?.size ?: 0})" }

    //Path("/tmp/bible-repair.txt").writeLines(tokenListing + results.compressed)

    val stopTokens = listOf(
        "!",
        ",",
        "-VERSE-",
        ".",
        ":",
        ";",
        "a",
        "be",
        "and",
        "as",
        "but",
        "for",
        "in",
        "of",
        "or",
        "that",
        "the",
        "to",
        "shall",
        "upon",
        "with",
    ).map { it.lowercase() }

    val g = results.compressed.filter { it.startsWith("[") && it.endsWith("]") }.groupBy { it }
    val top128 = g.values.sortedByDescending { it.size }.take(128)
        val top128Size = top128.sumOf { it.size }
    println("Top 128 account for $top128Size out of ${results.compressed.size}")
    top128.forEach {
        println(it[0] +" : " + it.size)
    }
}