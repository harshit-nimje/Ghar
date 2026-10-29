package com.example.capture.dedupe

import com.example.capture.parser.ParsedCapture
import java.util.concurrent.ConcurrentHashMap

object TransactionDeduplicator {

  private val recentHashes = ConcurrentHashMap<String, Long>()
  private const val DEDUPE_WINDOW_MS = 2 * 60 * 1000L // 2 minutes

  fun isDuplicate(capture: ParsedCapture, timestamp: Long = System.currentTimeMillis()): Boolean {
    // Purge expired entries older than 5 minutes
    val cutoff = timestamp - (5 * 60 * 1000L)
    recentHashes.entries.removeIf { it.value < cutoff }

    val key = "${capture.amountPaise}_${capture.direction}"
    val lastSeen = recentHashes[key]

    if (lastSeen != null && (timestamp - lastSeen) < DEDUPE_WINDOW_MS) {
      return true
    }

    recentHashes[key] = timestamp
    return false
  }
}
