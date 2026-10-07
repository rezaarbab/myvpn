package com.myvpn.app.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocketFactory

/**
 * پینگ واقعی: اتصال + دست‌دادن TLS (با SNI درست) برای سرورهای TLSدار،
 * و در غیر این صورت فقط اتصال TCP.
 */
object ServerPinger {

    suspend fun ping(profile: ServerProfile, timeoutMs: Int = 5000): Long? =
        withContext(Dispatchers.IO) {
            val host = profile.server
            val port = profile.serverPort
            if (host.isBlank() || port <= 0) return@withContext null

            if (profile.tls || profile.realityEnabled) {
                pingTls(host, port, profile.serverName.ifBlank { host }, timeoutMs)?.let { return@withContext it }
            }
            pingTcp(host, port, timeoutMs)
        }

    private fun pingTls(host: String, port: Int, sni: String, timeoutMs: Int): Long? = try {
        val start = SystemClock.elapsedRealtime()
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val socket = factory.createSocket() as javax.net.ssl.SSLSocket
        try {
            socket.soTimeout = timeoutMs
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            val parameters = socket.sslParameters
            parameters.serverNames = listOf(SNIHostName(sni))
            socket.sslParameters = parameters
            socket.startHandshake()
            SystemClock.elapsedRealtime() - start
        } finally {
            runCatching { socket.close() }
        }
    } catch (e: Exception) {
        null
    }

    private fun pingTcp(host: String, port: Int, timeoutMs: Int): Long? = try {
        val start = SystemClock.elapsedRealtime()
        val socket = Socket()
        socket.connect(InetSocketAddress(host, port), timeoutMs)
        val elapsed = SystemClock.elapsedRealtime() - start
        runCatching { socket.close() }
        elapsed
    } catch (e: Exception) {
        null
    }
}
