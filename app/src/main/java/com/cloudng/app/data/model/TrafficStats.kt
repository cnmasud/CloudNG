package com.cloudng.app.data.model

data class TrafficStats(
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val uploadSpeed: Long = 0L,
    val downloadSpeed: Long = 0L
) {
    companion object {
        val EMPTY = TrafficStats()
    }
}
