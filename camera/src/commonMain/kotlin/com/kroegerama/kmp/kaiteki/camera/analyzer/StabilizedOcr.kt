package com.kroegerama.kmp.kaiteki.camera.analyzer

import com.kroegerama.kmp.kaiteki.camera.ExperimentalKaitekiCameraApi
import com.kroegerama.kmp.kaiteki.camera.model.OCRResult
import com.kroegerama.kmp.kaiteki.camera.model.OCRResultBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MAX_OVERLAP_RATIO = 0.6f

/**
 * Suppresses OCR flicker: emits only lines seen in at least [minHits] of the last [windowSize]
 * frames, merged by majority text vote with averaged boxes and the stronger of two overlapping
 * readings kept. Emitted lines persist down to [persistHits] and are dropped after
 * [maxConsecutiveMisses] misses. Lines match across frames when their box centers lie within
 * [maxCenterDistance] and their texts within [maxTextDistanceRatio] edits per character.
 * The window counts frames, so the look-back depends on the analysis frame rate;
 * a [windowSize] of `1` disables stabilization.
 */
@ExperimentalKaitekiCameraApi
public fun Flow<OCRResult>.stabilized(
    windowSize: Int = 5,
    minHits: Int = 3,
    persistHits: Int = (minHits - 1).coerceAtLeast(1),
    maxConsecutiveMisses: Int = 1.coerceAtMost(windowSize - 1),
    maxCenterDistance: Float = 0.05f,
    maxTextDistanceRatio: Float = 0.2f,
): Flow<OCRResult> {
    require(windowSize >= 1) { "windowSize must be at least 1" }
    require(minHits in 1..windowSize) { "minHits must be in 1..windowSize" }
    require(persistHits in 1..minHits) { "persistHits must be in 1..minHits" }
    require(maxConsecutiveMisses in 0 until windowSize) { "maxConsecutiveMisses must be in 0 until windowSize" }
    if (windowSize == 1) return this

    return flow {
        val window = ArrayDeque<List<OCRResultBlock>>()
        var lastEmitted = emptyList<OCRResultBlock>()
        collect { result ->
            window.addLast(result.blocks)
            if (window.size > windowSize) window.removeFirst()

            val clusters = mutableListOf<MutableList<IndexedValue<OCRResultBlock>>>()
            window.forEachIndexed { frameIndex, frame ->
                frame.forEach { block ->
                    val cluster = clusters.firstOrNull { candidate ->
                        candidate.last().value.matches(block, maxCenterDistance, maxTextDistanceRatio)
                    }
                    if (cluster != null) {
                        cluster += IndexedValue(frameIndex, block)
                    } else {
                        clusters += mutableListOf(IndexedValue(frameIndex, block))
                    }
                }
            }

            val candidates = clusters.mapNotNull { cluster ->
                if (window.lastIndex - cluster.last().index > maxConsecutiveMisses) return@mapNotNull null
                val hits = cluster.distinctBy { it.index }.size
                if (hits < persistHits) return@mapNotNull null
                val blocks = cluster.map { it.value }
                Candidate(
                    block = blocks.merged(),
                    hits = hits,
                    totalConfidence = blocks.map { it.confidence }.sum(),
                )
            }.filter { candidate ->
                candidate.hits >= minHits || lastEmitted.any { emitted ->
                    emitted.matches(candidate.block, maxCenterDistance, maxTextDistanceRatio)
                }
            }

            val stableBlocks = suppressOverlaps(candidates)
            lastEmitted = stableBlocks
            emit(OCRResult(stableBlocks))
        }
    }
}

@ExperimentalKaitekiCameraApi
private class Candidate(
    val block: OCRResultBlock,
    val hits: Int,
    val totalConfidence: Float,
)

/** Merges a cluster into one block: majority text (confidence as tie-break), averaged box. */
@ExperimentalKaitekiCameraApi
private fun List<OCRResultBlock>.merged(): OCRResultBlock {
    val winner = groupBy { it.text }.entries.maxWith(
        compareBy({ it.value.size }, { it.value.maxOf { block -> block.confidence } })
    ).value
    return winner.maxBy { it.confidence }.copy(
        relativeX = map { it.relativeX }.average().toFloat(),
        relativeY = map { it.relativeY }.average().toFloat(),
        relativeWidth = map { it.relativeWidth }.average().toFloat(),
        relativeHeight = map { it.relativeHeight }.average().toFloat(),
    )
}

/** Drops candidates whose box overlaps a stronger candidate, keeping one reading per location. */
@ExperimentalKaitekiCameraApi
private fun suppressOverlaps(candidates: List<Candidate>): List<OCRResultBlock> {
    val kept = mutableListOf<Candidate>()
    candidates
        .sortedWith(compareByDescending<Candidate> { it.hits }.thenByDescending { it.totalConfidence })
        .forEach { candidate ->
            if (kept.none { it.block.overlapRatio(candidate.block) > MAX_OVERLAP_RATIO }) {
                kept += candidate
            }
        }
    return kept.map { it.block }
}

/** Intersection area relative to the smaller of the two boxes. */
@ExperimentalKaitekiCameraApi
private fun OCRResultBlock.overlapRatio(other: OCRResultBlock): Float {
    val overlapWidth = minOf(relativeX + relativeWidth, other.relativeX + other.relativeWidth)
        .minus(maxOf(relativeX, other.relativeX))
        .coerceAtLeast(0f)
    val overlapHeight = minOf(relativeY + relativeHeight, other.relativeY + other.relativeHeight)
        .minus(maxOf(relativeY, other.relativeY))
        .coerceAtLeast(0f)
    val minArea = minOf(relativeWidth * relativeHeight, other.relativeWidth * other.relativeHeight)
    if (minArea <= 0f) return 0f
    return overlapWidth * overlapHeight / minArea
}

@ExperimentalKaitekiCameraApi
private fun OCRResultBlock.matches(
    other: OCRResultBlock,
    maxCenterDistance: Float,
    maxTextDistanceRatio: Float,
): Boolean {
    val dx = (relativeX + relativeWidth / 2f) - (other.relativeX + other.relativeWidth / 2f)
    val dy = (relativeY + relativeHeight / 2f) - (other.relativeY + other.relativeHeight / 2f)
    if (dx * dx + dy * dy > maxCenterDistance * maxCenterDistance) return false
    val maxLength = maxOf(text.length, other.text.length)
    val allowedEdits = (maxLength * maxTextDistanceRatio).toInt()
    return levenshtein(text, other.text) <= allowedEdits
}

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    var previousRow = IntArray(b.length + 1) { it }
    var currentRow = IntArray(b.length + 1)
    for (i in 1..a.length) {
        currentRow[0] = i
        for (j in 1..b.length) {
            val substitutionCost = if (a[i - 1] == b[j - 1]) 0 else 1
            currentRow[j] = minOf(
                currentRow[j - 1] + 1,
                previousRow[j] + 1,
                previousRow[j - 1] + substitutionCost
            )
        }
        val swap = previousRow
        previousRow = currentRow
        currentRow = swap
    }
    return previousRow[b.length]
}
