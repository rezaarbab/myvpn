package com.myvpn.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * ساخت کانفیگ JSON سازگار با sing-box 1.12 از پروفایل سرور.
 */
object ConfigBuilder {

    fun build(profile: ServerProfile): String {
        if (profile.type == "custom") return prepareCustomConfig(profile.rawConfig)
        val proxy = buildProxyOutbound(profile)
        return baseConfig(proxy).toString()
    }

    private fun baseConfig(proxy: JsonObject): JsonObject = buildJsonObject {
        putJsonObject("log") {
            put("level", "info")
            put("timestamp", true)
        }
        putJsonObject("dns") {
            putJsonArray("servers") {
                add(buildJsonObject {
                    put("type", "udp")
                    put("tag", "dns-remote")
                    put("server", "1.1.1.1")
                    put("detour", "proxy")
                })
                add(buildJsonObject {
                    put("type", "udp")
                    put("tag", "dns-direct")
                    put("server", "8.8.8.8")
                    // بدون detour: رفتار پیش‌فرض یعنی dial مستقیم؛
                    // اعتبارسنجِ sing-box «detour به direct خالی» را رد می‌کند
                })
            }
            put("final", "dns-remote")
        }
        putJsonArray("inbounds") {
            add(buildJsonObject {
                put("type", "tun")
                put("tag", "tun-in")
                putJsonArray("address") {
                    add(JsonPrimitive("172.19.0.1/30"))
                    add(JsonPrimitive("fdfe:dcba:9876::1/126"))
                }
                put("mtu", 9000)
                put("auto_route", true)
                put("strict_route", true)
                put("stack", "system")
            })
        }
        putJsonArray("outbounds") {
            add(proxy)
            add(buildJsonObject {
                put("type", "direct")
                put("tag", "direct")
            })
        }
        putJsonObject("route") {
            put("final", "proxy")
            put("default_domain_resolver", "dns-direct")
        }
        // libbox v1.12 در SetService به clash_server نیاز دارد
        putJsonObject("experimental") {
            putJsonObject("clash_api") {
                put("external_controller", "127.0.0.1:9090")
            }
        }
    }

    private fun buildProxyOutbound(p: ServerProfile): JsonObject = buildJsonObject {
        put("type", p.type)
        put("tag", "proxy")
        put("server", p.server)
        put("server_port", p.serverPort)
        when (p.type) {
            "vless" -> {
                put("uuid", p.uuid)
                if (p.flow.isNotBlank()) put("flow", p.flow)
                tlsBlock(p)?.let { put("tls", it) }
                transportBlock(p)?.let { put("transport", it) }
            }
            "vmess" -> {
                put("uuid", p.uuid)
                put("security", "auto")
                put("alter_id", p.alterId)
                tlsBlock(p)?.let { put("tls", it) }
                transportBlock(p)?.let { put("transport", it) }
            }
            "trojan" -> {
                put("password", p.password)
                tlsBlock(p)?.let { put("tls", it) }
                transportBlock(p)?.let { put("transport", it) }
            }
            "shadowsocks" -> {
                put("method", p.method)
                put("password", p.password)
            }
            "hysteria2" -> {
                put("password", p.password)
                if (p.obfsPassword.isNotBlank()) {
                    putJsonObject("obfs") {
                        put("type", "salamander")
                        put("password", p.obfsPassword)
                    }
                }
                tlsBlock(p)?.let { put("tls", it) }
            }
            "tuic" -> {
                put("uuid", p.uuid)
                put("password", p.password)
                put("congestion_control", p.congestionControl.ifBlank { "bbr" })
                tlsBlock(p)?.let { put("tls", it) }
            }
        }
    }

    private fun tlsBlock(p: ServerProfile): JsonObject? {
        if (!p.tls && !p.realityEnabled) return null
        return buildJsonObject {
            put("enabled", true)
            if (p.serverName.isNotBlank()) put("server_name", p.serverName)
            put("insecure", p.insecure)
            if (p.realityEnabled) {
                putJsonObject("reality") {
                    put("enabled", true)
                    put("public_key", p.realityPublicKey)
                    if (p.realityShortId.isNotBlank()) put("short_id", p.realityShortId)
                }
            }
            val fingerprint = p.utlsFingerprint.ifBlank { if (p.realityEnabled) "chrome" else "" }
            if (fingerprint.isNotBlank()) {
                putJsonObject("utls") {
                    put("enabled", true)
                    put("fingerprint", fingerprint)
                }
            }
        }
    }

    private fun transportBlock(p: ServerProfile): JsonObject? {
        if (p.transport.isBlank()) return null
        return buildJsonObject {
            put("type", p.transport)
            when (p.transport) {
                "ws" -> {
                    if (p.transportPath.isNotBlank()) put("path", p.transportPath)
                    if (p.transportHost.isNotBlank()) {
                        putJsonObject("headers") { put("Host", p.transportHost) }
                    }
                }
                "http", "httpupgrade" -> {
                    if (p.transportPath.isNotBlank()) put("path", p.transportPath)
                    if (p.transportHost.isNotBlank()) {
                        putJsonArray("host") { add(JsonPrimitive(p.transportHost)) }
                    }
                }
                "grpc" -> {
                    if (p.serviceName.isNotBlank()) put("service_name", p.serviceName)
                }
            }
        }
    }

    /**
     * کانفیگ خام کاربر: اگر inbound tun نداشت اضافه می‌شود و مسیر نهایی تنظیم می‌گردد.
     */
    private fun prepareCustomConfig(raw: String): String {
        val json = Json { ignoreUnknownKeys = true }
        val obj = json.parseToJsonElement(raw).jsonObject
        val inbounds = obj["inbounds"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
        val hasTun = inbounds.any { (it as? JsonObject)?.get("type")?.let { t -> t is JsonPrimitive && t.content == "tun" } == true }
        val outbounds = obj["outbounds"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
        val directTags = outbounds.mapNotNull { o ->
            (o as? JsonObject)?.let {
                val type = (it["type"] as? JsonPrimitive)?.content
                val tag = (it["tag"] as? JsonPrimitive)?.content
                if (type in listOf("direct", "block", "dns", "socks", "http")) tag else null
            }
        }.toSet()
        val firstProxyTag = outbounds.mapNotNull { o ->
            (o as? JsonObject)?.let {
                val type = (it["type"] as? JsonPrimitive)?.content
                val tag = (it["tag"] as? JsonPrimitive)?.content
                if (type != null && tag != null && type !in listOf("direct", "block", "dns")) tag else null
            }
        }.firstOrNull()

        return buildJsonObject {
            obj.forEach { (key, value) -> put(key, value) }
            if (!hasTun) {
                putJsonArray("inbounds") {
                    add(buildJsonObject {
                        put("type", "tun")
                        put("tag", "tun-in")
                        putJsonArray("address") {
                            add(JsonPrimitive("172.19.0.1/30"))
                            add(JsonPrimitive("fdfe:dcba:9876::1/126"))
                        }
                        put("mtu", 9000)
                        put("auto_route", true)
                        put("strict_route", true)
                        put("stack", "system")
                    })
                }
            }
            if (obj["route"] == null && firstProxyTag != null) {
                putJsonObject("route") {
                    put("final", firstProxyTag)
                    // resolver دایرکت فقط وقتی معتبر است که DNS خودمان هم تزریق شده باشد
                    if (directTags.isNotEmpty() && obj["dns"] == null) {
                        put("default_domain_resolver", "dns-direct")
                    }
                }
            }
            // بدون clash_api کتابخانه در SetService پنیک می‌کند
            val experimental = obj["experimental"] as? JsonObject
            if (experimental?.get("clash_api") == null) {
                put("experimental", buildJsonObject {
                    experimental?.forEach { (key, value) -> put(key, value) }
                    putJsonObject("clash_api") {
                        put("external_controller", "127.0.0.1:9090")
                    }
                })
            }
        }.toString()
    }
}
