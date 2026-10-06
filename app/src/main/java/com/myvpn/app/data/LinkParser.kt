package com.myvpn.app.data

import android.net.Uri
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.Base64

/**
 * پارس لینک‌های اشتراکی رایج و کانفیگ JSON sing-box.
 * پشتیبانی: vless:// vmess:// trojan:// ss:// hysteria2:// (hy2://) tuic:// و JSON
 */
object LinkParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<ServerProfile> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) error("متن خالی است")
        if (trimmed.startsWith("{")) {
            return listOf(parseRawConfig(trimmed))
        }
        val results = trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { parseLink(it) }
            .toList()
        if (results.isEmpty()) error("هیچ لینک قابل‌فهمی پیدا نشد")
        return results
    }

    private fun parseLink(line: String): ServerProfile? = try {
        when {
            line.startsWith("vless://") -> parseUriBased(line, "vless")
            line.startsWith("trojan://") -> parseUriBased(line, "trojan")
            line.startsWith("hysteria2://") || line.startsWith("hy2://") -> parseHysteria2(line)
            line.startsWith("tuic://") -> parseTuic(line)
            line.startsWith("vmess://") -> parseVmess(line)
            line.startsWith("ss://") -> parseShadowsocks(line)
            else -> null
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("خطا در پارس: ${e.message}", e)
    }

    private fun parseUriBased(link: String, type: String): ServerProfile {
        val uri = Uri.parse(link)
        val params = queryParameters(uri)
        val security = params["security"] ?: "tls"
        val netType = params["type"] ?: "tcp"
        return ServerProfile(
            id = newId(),
            name = uri.fragment?.takeIf { it.isNotBlank() } ?: "${type}-${uri.host}",
            type = type,
            server = uri.host ?: error("آدرس سرور پیدا نشد"),
            serverPort = uri.port.takeIf { it > 0 } ?: error("پورت نامعتبر است"),
            uuid = if (type == "vless") uri.userInfo.orEmpty() else "",
            password = if (type == "trojan") uri.userInfo.orEmpty() else "",
            flow = params["flow"] ?: "",
            tls = security != "none",
            serverName = params["sni"] ?: params["host"] ?: "",
            realityEnabled = security == "reality",
            realityPublicKey = params["pbk"] ?: "",
            realityShortId = params["sid"] ?: "",
            utlsFingerprint = params["fp"] ?: "",
            insecure = params["allowInsecure"] == "1" || params["insecure"] == "1",
            transport = when (netType) {
                "tcp", "raw" -> ""
                "ws" -> "ws"
                "grpc" -> "grpc"
                "http", "h2" -> "http"
                "httpupgrade" -> "httpupgrade"
                else -> ""
            },
            transportHost = params["host"] ?: "",
            transportPath = params["path"] ?: "",
            serviceName = params["serviceName"] ?: "",
        )
    }

    private fun parseHysteria2(link: String): ServerProfile {
        val uri = Uri.parse(link)
        val params = queryParameters(uri)
        val password = uri.userInfo?.substringAfterLast(':').orEmpty()
        return ServerProfile(
            id = newId(),
            name = uri.fragment?.takeIf { it.isNotBlank() } ?: "hy2-${uri.host}",
            type = "hysteria2",
            server = uri.host ?: error("آدرس سرور پیدا نشد"),
            serverPort = uri.port.takeIf { it > 0 } ?: 443,
            password = password,
            tls = true,
            serverName = params["sni"] ?: params["peer"] ?: "",
            insecure = params["insecure"] == "1" || params["allowInsecure"] == "1",
            obfsPassword = params["obfs-password"] ?: params["obfsParam"] ?: "",
        )
    }

    private fun parseTuic(link: String): ServerProfile {
        val uri = Uri.parse(link)
        val params = queryParameters(uri)
        val userInfo = uri.userInfo.orEmpty()
        return ServerProfile(
            id = newId(),
            name = uri.fragment?.takeIf { it.isNotBlank() } ?: "tuic-${uri.host}",
            type = "tuic",
            server = uri.host ?: error("آدرس سرور پیدا نشد"),
            serverPort = uri.port.takeIf { it > 0 } ?: 443,
            uuid = userInfo.substringBefore(':'),
            password = userInfo.substringAfter(':', missingDelimiterValue = ""),
            tls = true,
            serverName = params["sni"] ?: "",
            insecure = params["allow_insecure"] == "1" || params["insecure"] == "1",
            congestionControl = params["congestion_control"] ?: "bbr",
        )
    }

    private fun parseVmess(link: String): ServerProfile {
        val payload = decodeBase64(link.removePrefix("vmess://"))
        val obj = json.parseToJsonElement(payload).jsonObject
        fun str(key: String) = obj[key]?.toString()?.removeSurrounding("\"") ?: ""
        val net = str("net")
        return ServerProfile(
            id = newId(),
            name = str("ps").ifBlank { "vmess-${str("add")}" },
            type = "vmess",
            server = str("add"),
            serverPort = str("port").toIntOrNull() ?: error("پورت نامعتبر است"),
            uuid = str("id"),
            alterId = str("aid").toIntOrNull() ?: str("alterId").toIntOrNull() ?: 0,
            tls = str("tls") == "tls",
            serverName = str("sni").ifBlank { str("host") },
            utlsFingerprint = str("fp"),
            insecure = str("allowInsecure") == "1",
            transport = when (net) {
                "tcp" -> ""
                "ws" -> "ws"
                "grpc" -> "grpc"
                "h2" -> "http"
                "httpupgrade" -> "httpupgrade"
                else -> ""
            },
            transportHost = str("host"),
            transportPath = str("path"),
            serviceName = str("path").takeIf { net == "grpc" } ?: "",
        )
    }

    private fun parseShadowsocks(link: String): ServerProfile {
        val body = link.removePrefix("ss://")
        val fragment = body.substringAfter("#", "")
        val main = body.substringBefore("#").substringBefore("?")
        val decoded = runCatching { decodeBase64(main) }.getOrDefault(main)
        val name = java.net.URLDecoder.decode(fragment.ifBlank { "ss" }, "UTF-8")

        // حالت SIP002: base64(method:password)@host:port
        val atIndex = decoded.lastIndexOf('@')
        if (atIndex > 0) {
            val userInfo = decoded.substring(0, atIndex)
            val hostPart = decoded.substring(atIndex + 1).substringBefore('/')
            val method = userInfo.substringBefore(':')
            val password = userInfo.substringAfter(':', missingDelimiterValue = "")
            val host = hostPart.substringBeforeLast(':')
            val port = hostPart.substringAfterLast(':').toIntOrNull() ?: error("پورت نامعتبر است")
            return ServerProfile(
                id = newId(), name = name, type = "shadowsocks",
                server = host.trim('[', ']'), serverPort = port,
                method = method, password = password,
            )
        }
        // حالت قدیمی: base64(method:password@host:port)
        val userinfo = decoded.substringBeforeLast('@')
        val hostPart = decoded.substringAfterLast('@')
        return ServerProfile(
            id = newId(), name = name, type = "shadowsocks",
            server = hostPart.substringBeforeLast(':').trim('[', ']'),
            serverPort = hostPart.substringAfterLast(':').toIntOrNull() ?: error("پورت نامعتبر است"),
            method = userinfo.substringBefore(':'),
            password = userinfo.substringAfter(':', missingDelimiterValue = ""),
        )
    }

    private fun parseRawConfig(text: String): ServerProfile {
        val obj = json.parseToJsonElement(text).jsonObject
        obj["outbounds"] ?: error("کانفیگ JSON فاقد outbounds است")
        return ServerProfile(
            id = newId(),
            name = "کانفیگ سفارشی",
            type = "custom",
            rawConfig = text,
        )
    }

    private fun queryParameters(uri: Uri): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val raw = uri.encodedQuery ?: return result
        raw.split('&').forEach { pair ->
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun decodeBase64(input: String): String {
        val normalized = input.trim().replace('-', '+').replace('_', '/').replace("\n", "")
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return String(Base64.getDecoder().decode(padded))
    }

    fun newId(): String = java.util.UUID.randomUUID().toString()
}
