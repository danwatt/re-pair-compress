@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

package org.danwatt.repair.bible

import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.danwatt.repair.*
import java.io.ByteArrayOutputStream
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.io.path.writeLines

val stopTokens = listOf(
    "-verse-",
    ",",
    ";",
    ":",
    "!",
    "?",
    ".",
    "a",
    "all",
    "also",
    "and",
    "are",
    "as",
    "be",
    "but",
    "by",
    "for",
    "from",
    "God",
    "had",
    "have",
    "he",
    "her",
    "him",
    "his",
    "I",
    "in",
    "is",
    "it",
    "me",
    "my",
    "not",
    "of",
    "or",
    "shall",
    "that",
    "the",
    "thee",
    "their",
    "them",
    "there",
    "thereof",
    "they",
    "this",
    "thou",
    "thy",
    "to",
    "unto",
    "upon",
    "was",
    "were",
    "which",
    "with",
    "ye",
    "you",
).map { it.lowercase() }

fun pairMarkerGenerator(i: Int, p: Pair<String, String>): String {
    val first = p.first.substringAfter("(").substringBefore(")")
    val second = p.second.substringAfter("(").substringBefore(")")
    return "[$i ($first|$second)]"
}

fun main() {
    val tokens = BibleParser().readTranslation()

    val lexicon = tokens.distinct().sorted()
    val numDistinctTokens = lexicon.size
    for (reserved in 103..103) {
        println("Trying $reserved")
        trial(reserved, numDistinctTokens, tokens, lexicon)
        println()
    }
}

private fun trial(
    reserved: Int,
    numDistinctTokens: Int,
    tokens: List<String>,
    lexicon: List<String>,
) {
    val reservationT = (0xFF - reserved).shl(8) + 0xFF
    println("RT: ${reservationT.toHexString(HexFormat.UpperCase)}")
    val maxIterations = (reservationT - numDistinctTokens) + 1

    val results = RePairCompress().compress(
        input = tokens,
        stopAfterIterations = maxIterations,
        progress = false
    )
//
//    val decompressed = RePairDecompress().decompress(results.compressed, results.pairs)
//    if (decompressed.size != tokens.size) {
//        throw RuntimeException("Decompressed size of ${decompressed.size} != original size of ${tokens.size}")
//    }
//    decompressed.forEachIndexed { index, s ->
//        val original = tokens[index]
//        if (original != s) {
//            throw RuntimeException("Mis-matched decompression at $index")
//        }
//    }


    println("${tokens.size} to ${results.compressed.size} (ratio ${(tokens.size.toDouble() / results.compressed.size)}:1)")

    println("Distinct input tokens: $numDistinctTokens")
    println("Pairs generated: ${results.pairs.size}")

    val gr = results.compressed.groupBy { it }

    val tokenListing =
        results.pairs.map { p -> "# $$p (${gr[p]?.size ?: 0})" }

    Path("/tmp/bible-repair.txt").writeLines(tokenListing + results.compressed.map { it.toString() })
    writeTest(results, lexicon, reserved)

//    stopWordTest(results)
}

/*
Trying 45: 969810 (VB: 91, PH: 159710, Body: 810009
    [0 (,|and)]| ,| and| the| to| of| ;| [11 (,|and|the)]| [3 (of|the)]| :| [1 (.|-VERSE-)]| that| in| with|
    [2 (.|-VERSE-|And)]| not| [8 (;|and)]| also| was| a| is| [6 (in|the)]| it| [9 (:|and)]| for| shall| be|
    his| me| unto| him| were| he| [2835 (,|the)]| them| [20 (.|-VERSE-|And|the)]| [4 (,|that)]| [17 (unto|the)]|
    [29 (.|-VERSE-|The)]| [10 (shall|be)]| from| her| ye| their| my

Switching to a minimum of 4:
50: 960838 (VB: 101, PH: 101790, Body: 858947
51: 960376 (VB: 103, PH: 101790, Body: 858483
52: 959918 (VB: 105, PH: 101790, Body: 858023
53: 959460 (VB: 107, PH: 101790, Body: 857563
54: 959004 (VB: 109, PH: 101790, Body: 857105 <--- GBA here
55: 958557 (VB: 111, PH: 101790, Body: 856656
60: 956472 (VB: 121, PH: 101790, Body: 854561
64: 954892 (VB: 129, PH: 101790, Body: 852973)
70: 952639 (VB: 141, PH: 101790, Body: 850708)
80: 949149 (VB: 161, PH: 101790, Body: 847198)
90: 946006 (VB: 181, PH: 101790, Body: 844035)
100:943244 (VB: 201, PH: 101790, Body: 841253)
103:943712 (VB: 207, PH: 101146, Body: 842359). It goes up after 103
RF: 942374
Just stop tokens: 942964 - Still less than GBA by ~5k
Encoded : 943063 (VB: 207, PH: 100966, Body: 841890) <--- uh.... why did it jump ever so slightly when changing toString()?

Compare to GBA: 955,290
: Question - does that include the lexicon and skip list? I think it does - need to circle back and check, because
the Lexicon itself is going to take 15kb or so.

 */

fun writeTest(
    results: RePairResult<String>,
    lexicon: List<String>,
    reserved: Int,
) {
    val stopWordTokensAndPairsGrouped: Map<String, List<String>> = results.compressed
        .filter { it.allTokens().all { t -> t.lowercase() in stopTokens } }
        .map { it.toString() }
        .groupBy { it }

    val singleByteTokensAndPairs: List<String> = stopWordTokensAndPairsGrouped.values
        .sortedByDescending { it.size }
        .take(reserved)
        .map { it[0] }

    println("== Top $reserved Tokens and Pairs ==")
    singleByteTokensAndPairs.forEach(::println)

    val doubleBytePairs: List<String> = results.pairs
        .map { it.toString() }
        .filterNot { it in singleByteTokensAndPairs }
        .sorted()

    val doubleByteTokens = lexicon.filterNot { it in singleByteTokensAndPairs }.sorted()

    /*
    Build a list of all output tokens, in a priority order:
    1) Single byte token/pairs - so these get the indexes 0-Reserved - keeping in mind that these numbers point to a lookup table
    2) Double byte pairs - Reserved - total number of remaining pairs (T)
    3) Double byte tokens
     */
    val tokensInIndexOrder: List<String> = singleByteTokensAndPairs +
            doubleBytePairs +
            doubleByteTokens

    println()
    println("Tokens in index order:")
    tokensInIndexOrder.take(1024).chunked(8).forEach { chunk->
        println(chunk.joinToString("\t"))
    }

    println()

    println("Final pairs in index order:")
    doubleBytePairs.takeLast(128).chunked(8).forEach { chunk->
        println(chunk.joinToString("\t"))
    }


    val m = mutableMapOf<String, UInt>()
    tokensInIndexOrder.forEachIndexed { index, s ->
        m[s] = index.toUInt()
    }
    val asIntegers = results.compressed
        .map { it.toString() }
        .map { m[it]!! }
        .toUIntArray()

    //TODO: There may be some simple compression gains to be made if we sort the pairs
    // and use a variable bit output

    val varByteHeader = UByteArray(1 + reserved * 2)
    varByteHeader[0] = reserved.toUByte()
    singleByteTokensAndPairs.forEachIndexed { index, token ->
        val us = tokensInIndexOrder.indexOf(token).toUShort()
        varByteHeader[1 + index * 2] = us.upperUByte()
        varByteHeader[1 + index * 2 + 1] = us.lowerUByte()
    }

    val pairHeader = UByteArray(2 + results.pairs.size * 4)
    varByteHeader[0] = results.pairs.size.toUShort().upperUByte()
    varByteHeader[1] = results.pairs.size.toUShort().lowerUByte()
    results.pairs.forEachIndexed { index, e ->
        val p1 = m[e.pair.first.toString()]!!.toUShort()
        val p2 = m[e.pair.second.toString()]!!.toUShort()
        pairHeader[2 + index * 4 + 0] = p1.upperUByte()
        pairHeader[2 + index * 4 + 1] = p1.lowerUByte()
        pairHeader[2 + index * 4 + 2] = p2.upperUByte()
        pairHeader[2 + index * 4 + 3] = p2.lowerUByte()
    }

    val codec = VarByteCodec(reserved.toUByte())
    val encoded = codec.encode(asIntegers)
    val complete = varByteHeader + pairHeader + encoded
    println("Encoded : ${complete.size} (VB: ${varByteHeader.size}, PH: ${pairHeader.size}, Body: ${encoded.size})")
    Path("/tmp/out.bin").writeBytes(complete.toByteArray())

    CompressorStreamFactory.getSingleton().compressorOutputStreamProviders.forEach { type, stream ->
        try {
            val b = ByteArrayOutputStream()
            val s = stream.createCompressorOutputStream(type, b)
            s.write(encoded.toByteArray())
            s.close()
            println("$type: ${b.toByteArray().size}")
        } catch (_: Throwable) {}
    }
}