package com.example.capsule

import android.os.SystemClock

// Navigation details read from the Google Maps screen (only when the user has turned navigation info on).
// The patterns match the text seen in Maps' navigation screen: "6 min", "16 km • 12:30 am", "50 m", and the street.
object MapsNav {
    @Volatile var active = false
    @Volatile var eta = ""            // e.g. "6 min"
    @Volatile var arrival = ""        // e.g. "12:07 am"
    @Volatile var distanceLeft = ""   // e.g. "400 m"
    @Volatile var turnDistance = ""   // e.g. "50 m"
    @Volatile var street = ""         // e.g. "Channasandra Main Rd"
    @Volatile var lastSeen = 0L

    private val etaRe = Regex("""\d+\s?(hr|h|min|mins)(\s\d+\s?min)?""", RegexOption.IGNORE_CASE)
    private val leftRe = Regex("""(\d+(\.\d+)?\s?(km|m|mi))\s*[•·]\s*(\d{1,2}:\d{2}\s?[ap]m)""", RegexOption.IGNORE_CASE)
    private val turnRe = Regex("""\d+(\.\d+)?\s?(km|m)""", RegexOption.IGNORE_CASE)
    private val ignore = setOf("Re-centre", "Report", "Search", "Start", "Done", "Overview")

    fun update(texts: List<String>) {
        val items = texts.map { it.trim() }.filter { it.isNotEmpty() }
        var e = ""
        var a = ""
        var d = ""
        var t = ""
        var turnIndex = -1
        for ((i, y) in items.withIndex()) {
            val lm = leftRe.matchEntire(y)
            if (lm != null && d.isEmpty()) {
                d = lm.groupValues[1]
                a = lm.groupValues[4]
                continue
            }
            if (e.isEmpty() && etaRe.matchEntire(y) != null) {
                e = y
                continue
            }
            if (t.isEmpty() && turnRe.matchEntire(y) != null) {
                t = y
                turnIndex = i
            }
        }
        // The street is the first readable text after the turn distance
        var s = ""
        if (turnIndex >= 0) {
            for (j in turnIndex + 1 until items.size) {
                val y = items[j]
                val looksLikeText = y.length > 3 && y.any { it.isLetter() } && y !in ignore
                if (looksLikeText && leftRe.matchEntire(y) == null && etaRe.matchEntire(y) == null) {
                    s = y
                    break
                }
            }
        }
        eta = e
        arrival = a
        distanceLeft = d
        turnDistance = t
        street = s
        active = e.isNotEmpty() || d.isNotEmpty()
        lastSeen = SystemClock.uptimeMillis()
    }
}
