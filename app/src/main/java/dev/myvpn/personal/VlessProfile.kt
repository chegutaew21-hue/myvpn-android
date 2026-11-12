package dev.myvpn.personal

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Strict VLESS Reality URL parser.
 * Does not log tokens, identifiers or keys.
 */
data class VlessProfile(
    val host: String,
    val port: Int,
    val uuid: String,
    val serverName: String,
    val publicKey: String,
    val shortId: String,
    val fingerprint: String,
    val flow: String
) {
    companion object {
        fun parse(link: String): VlessProfile {
            val uri = Uri.parse(link.trim())
            require(uri.scheme.equals("vless", ignoreCase = true)) { "Нужна VLESS-ссылка" }
            val host = uri.host?.takeIf { it.isNotBlank() } ?: error("Не указан сервер")
            require(uri.port in 1..65535) { "Неверный порт" }
            val uuid = uri.userInfo ?: error("Не указан UUID")
            require(runCatching { UUID.fromString(uuid) }.isSuccess) { "Неверный UUID" }
            val security = uri.getQueryParameter("security") ?: ""
            require(security == "reality") { "Пока поддерживается только Reality" }
            val serverName = uri.getQueryParameter("sni")?.takeIf { it.isNotBlank() }
                ?: error("В ссылке нет sni")
            val publicKey = uri.getQueryParameter("pbk")?.takeIf { it.isNotBlank() }
                ?: error("В ссылке нет pbk")
            val transport = uri.getQueryParameter("type") ?: "tcp"
            require(transport == "tcp") { "Пока поддерживается только TCP" }
            return VlessProfile(
                host = host,
                port = uri.port,
                uuid = uuid,
                serverName = serverName,
                publicKey = publicKey,
                shortId = uri.getQueryParameter("sid") ?: "",
                fingerprint = uri.getQueryParameter("fp") ?: "chrome",
                flow = uri.getQueryParameter("flow") ?: ""
            )
        }
    }

    /**
     * Prepare sing-box configuration; no VPN is started by this method.
     * Integration with libbox/VpnService is the next milestone.
     */
    fun singBoxJson(): String {
        val tls = JSONObject()
            .put("enabled", true)
            .put("server_name", serverName)
            .put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint))
            .put("reality", JSONObject().put("enabled", true).put("public_key", publicKey).put("short_id", shortId))
        val outbound = JSONObject()
            .put("type", "vless")
            .put("tag", "proxy")
            .put("server", host)
            .put("server_port", port)
            .put("uuid", uuid)
            .put("tls", tls)
        if (flow.isNotBlank()) outbound.put("flow", flow)
        return JSONObject()
            .put("log", JSONObject().put("level", "warn"))
            .put("inbounds", JSONArray().put(JSONObject()
                .put("type", "tun")
                .put("tag", "tun-in")
                .put("address", JSONArray().put("172.19.0.1/30"))
                .put("auto_route", true)))
            .put("outbounds", JSONArray().put(outbound)
                .put(JSONObject().put("type", "direct").put("tag", "direct")))
            .put("route", JSONObject().put("final", "proxy"))
            .toString()
    }
}
