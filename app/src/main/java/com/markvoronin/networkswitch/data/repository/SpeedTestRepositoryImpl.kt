package com.markvoronin.networkswitch.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.markvoronin.networkswitch.di.IoDispatcher
import com.markvoronin.networkswitch.domain.repository.SpeedTestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Measures download speed over the cellular network, even when Wi-Fi is the default
 * network, so results for different network modes can be compared.
 */
@Singleton
class SpeedTestRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SpeedTestRepository {

    override suspend fun runDownloadTest(): Result<Long> {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
            ?: return Result.failure(IOException("No connectivity service"))
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Completing more than once is a no-op, so late callbacks are harmless
        val cellularNetwork = CompletableDeferred<Network?>()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                cellularNetwork.complete(network)
            }

            override fun onUnavailable() {
                cellularNetwork.complete(null)
            }
        }
        connectivityManager.requestNetwork(request, callback, NETWORK_TIMEOUT_MS)
        return try {
            val network = cellularNetwork.await()
                ?: return Result.failure(IOException("No cellular network available"))
            // The network must stay requested until the download is done
            Result.success(withContext(ioDispatcher) { download(network) })
        } catch (e: IOException) {
            Log.w(TAG, "Speed test failed", e)
            Result.failure(e)
        } finally {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    /** Average bits per second, measured from the first received byte. */
    private suspend fun download(network: Network): Long {
        val connection = network.openConnection(URL(TEST_URL)) as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected response ${connection.responseCode}")
            }
            val buffer = ByteArray(BUFFER_SIZE)
            var totalBytes = 0L
            var startNs = 0L
            var elapsedNs = 0L
            connection.inputStream.use { input ->
                while (elapsedNs < TEST_DURATION_NS) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    val now = System.nanoTime()
                    if (startNs == 0L) {
                        startNs = now
                    } else {
                        totalBytes += read
                        elapsedNs = now - startNs
                    }
                }
            }
            if (totalBytes == 0L || elapsedNs <= 0L) throw IOException("No data received")
            return totalBytes * 8 * NANOS_PER_SECOND / elapsedNs
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "SpeedTest"
        // Capped so a fast connection can't burn through much mobile data. Cloudflare answers
        // 403 for 100000000 bytes or more.
        const val TEST_URL = "https://speed.cloudflare.com/__down?bytes=90000000"
        const val TEST_DURATION_NS = 10_000_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000L
        const val NETWORK_TIMEOUT_MS = 5_000
        const val TIMEOUT_MS = 5_000
        const val BUFFER_SIZE = 64 * 1024
    }
}
