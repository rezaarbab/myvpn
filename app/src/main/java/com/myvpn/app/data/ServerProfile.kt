package com.myvpn.app.data

import kotlinx.serialization.Serializable

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val type: String, // vless, vmess, trojan, shadowsocks, hysteria2, tuic, custom
    val server: String = "",
    val serverPort: Int = 0,
    val uuid: String = "",
    val password: String = "",
    val method: String = "",
    val alterId: Int = 0,
    val flow: String = "",
    val tls: Boolean = true,
    val serverName: String = "",
    val realityEnabled: Boolean = false,
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val utlsFingerprint: String = "",
    val insecure: Boolean = false,
    val transport: String = "",
    val transportHost: String = "",
    val transportPath: String = "",
    val serviceName: String = "",
    val obfsPassword: String = "",
    val congestionControl: String = "",
    val rawConfig: String = "",
    // مصرف تجمعی این سرور (بایت)
    val usedDown: Long = 0,
    val usedUp: Long = 0,
) {
    val displayAddress: String
        get() = if (type == "custom") "کانفیگ سفارشی" else "$server:$serverPort"
}
