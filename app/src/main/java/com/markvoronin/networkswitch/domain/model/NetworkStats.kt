package com.markvoronin.networkswitch.domain.model

/**
 * Live network performance snapshot.
 *
 * @property signalDbm cell signal strength in dBm, or null if unknown
 * @property signalLevel cell signal level from 0 (none) to 4 (great), or null if unknown
 */
data class NetworkStats(
    val downloadBps: Long = 0,
    val uploadBps: Long = 0,
    val signalDbm: Int? = null,
    val signalLevel: Int? = null
)
