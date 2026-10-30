package com.markvoronin.networkswitch.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.net.TrafficStats
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.markvoronin.networkswitch.domain.model.NetworkStats
import com.markvoronin.networkswitch.domain.repository.NetworkStatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkStatsRepository {

    private data class Throughput(val downloadBps: Long, val uploadBps: Long)

    override fun observeNetworkStats(): Flow<NetworkStats> =
        combine(observeThroughput(), observeSignalStrength()) { throughput, signal ->
            NetworkStats(
                downloadBps = throughput.downloadBps,
                uploadBps = throughput.uploadBps,
                signalDbm = signal?.cellSignalStrengths?.firstOrNull { it.dbm != Int.MAX_VALUE }?.dbm,
                signalLevel = signal?.level
            )
        }

    /** Total device traffic (Wi-Fi + mobile), sampled once per second. */
    private fun observeThroughput(): Flow<Throughput> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastTime = SystemClock.elapsedRealtime()
        emit(Throughput(0, 0))
        while (true) {
            delay(SAMPLE_INTERVAL_MS)
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            val now = SystemClock.elapsedRealtime()
            val elapsedMs = (now - lastTime).coerceAtLeast(1)
            emit(
                Throughput(
                    downloadBps = bitsPerSecond(rx - lastRx, elapsedMs),
                    uploadBps = bitsPerSecond(tx - lastTx, elapsedMs)
                )
            )
            lastRx = rx
            lastTx = tx
            lastTime = now
        }
    }

    // Counters can be UNSUPPORTED (-1) or reset, which makes the delta negative
    private fun bitsPerSecond(deltaBytes: Long, elapsedMs: Long): Long =
        if (deltaBytes <= 0) 0 else deltaBytes * 8 * 1000 / elapsedMs

    /**
     * Cell signal strength, or null while unknown. The listener is registered when the flow
     * is collected and unregistered when collection stops. Needs no permission.
     */
    @Suppress("DEPRECATION")
    private fun observeSignalStrength(): Flow<SignalStrength?> {
        val telephonyManager = context.getSystemService(TelephonyManager::class.java)
        if (telephonyManager == null ||
            !context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        ) {
            return flowOf(null)
        }
        val executor = context.mainExecutor
        return callbackFlow<SignalStrength?> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        trySend(signalStrength)
                    }
                }
                telephonyManager.registerTelephonyCallback(executor, callback)
                awaitClose { telephonyManager.unregisterTelephonyCallback(callback) }
            } else {
                val listener = object : PhoneStateListener(executor) {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        trySend(signalStrength)
                    }
                }
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                awaitClose { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        }.onStart { emit(null) }
    }

    private companion object {
        const val SAMPLE_INTERVAL_MS = 1000L
    }
}
