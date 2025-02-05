package com.github.mrfapa.diffprompt

class Clock {
    private val startTimes: MutableMap<String, Long> = HashMap()
    private val endTimes: MutableMap<String, Long> = HashMap()

    fun start(key: String) {
        startTimes[key] = System.currentTimeMillis()
        endTimes.remove(key)
    }

    fun stop(key: String) {
        endTimes[key] = System.currentTimeMillis()
    }

    fun getTime(key: String): Long {
        val start = startTimes[key]
        val end = endTimes[key]
        if (start != null && end != null) {
            return end - start
        }
        throw RuntimeException("No time for key $key")

    }

    fun getTimeInSeconds(key: String): Long {
        return getTime(key) / 1000
    }

    fun getMeasurements(): String {
        var measurements: String = "Measurements\n"
        for (i in startTimes) {
            measurements += "${i.key}: ${getTime(i.key)}\n"
        }
        return measurements
    }
}