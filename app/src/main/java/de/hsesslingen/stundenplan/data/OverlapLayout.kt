package de.hsesslingen.stundenplan.data

const val DAY_START_MINUTES_DEFAULT = 8 * 60
const val DAY_END_MINUTES_DEFAULT = 19 * 60

/** Expands the default 08:00–19:00 grid window only when an event actually falls outside it (e.g.
 *  an evening block running 17:30–21:00) — otherwise such events used to be silently clipped off
 *  the grid entirely instead of just being rarer. */
fun dayWindowFor(events: List<TimetableEvent>): Pair<Int, Int> {
    if (events.isEmpty()) return DAY_START_MINUTES_DEFAULT to DAY_END_MINUTES_DEFAULT
    val earliest = events.minOf { it.startMinutes }
    val latest = events.maxOf { it.endMinutes }
    val start = minOf(DAY_START_MINUTES_DEFAULT, (earliest / 60) * 60)
    val end = maxOf(DAY_END_MINUTES_DEFAULT, ((latest + 59) / 60) * 60)
    return start to end
}

data class OverlapSlot(val event: TimetableEvent, val column: Int, val columnCount: Int)

/**
 * Assigns side-by-side columns to events that overlap in time — Google-Calendar-style — instead of
 * stacking them exactly on top of each other (which used to just show whichever card was drawn
 * last, with the one underneath bleeding through at half opacity).
 *
 * Events are grouped into clusters of mutually (possibly transitively) overlapping events by
 * tracking the running max end-time, then greedily packed into the fewest columns within each
 * cluster: each event goes into the first column whose previous event has already ended.
 */
fun layoutOverlaps(events: List<TimetableEvent>): List<OverlapSlot> {
    val sorted = events.sortedWith(compareBy({ it.startMinutes }, { it.endMinutes }))
    val result = mutableListOf<OverlapSlot>()
    var cluster = mutableListOf<TimetableEvent>()
    var clusterEnd = 0

    fun flushCluster() {
        if (cluster.isEmpty()) return
        val columnEnds = mutableListOf<Int>()
        val columnByEvent = HashMap<TimetableEvent, Int>()
        for (event in cluster) {
            val column = columnEnds.indexOfFirst { it <= event.startMinutes }
            if (column >= 0) {
                columnEnds[column] = event.endMinutes
                columnByEvent[event] = column
            } else {
                columnEnds.add(event.endMinutes)
                columnByEvent[event] = columnEnds.lastIndex
            }
        }
        val columnCount = columnEnds.size
        cluster.forEach { result.add(OverlapSlot(it, columnByEvent.getValue(it), columnCount)) }
        cluster = mutableListOf()
    }

    for (event in sorted) {
        when {
            cluster.isEmpty() -> clusterEnd = event.endMinutes
            event.startMinutes < clusterEnd -> clusterEnd = maxOf(clusterEnd, event.endMinutes)
            else -> {
                flushCluster()
                clusterEnd = event.endMinutes
            }
        }
        cluster.add(event)
    }
    flushCluster()
    return result
}
