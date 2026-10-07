package com.myvpn.app.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * پینگ TCP ساده به آدرس:پورت سرور (زمان برقراری اتصال).
 */
object ServerPinger {

    suspend fun ping(server: String, port: Int, timeoutMs: Int = 3000): Long? =
        withContext(Dispatchers.IO) {
            try {
                val start = SystemClock.elapsedRealtime()
                val socket = Socket()
                socket.connect(InetSocketAddress(server, port), timeoutMs)
                val elapsed = SystemClock.elapsedRealtime() - start
                runCatching { socket.close() }
                elapsed
            } catch (e: Exception) {
                null
            }
        }
}
