package dev.myvpn.personal

import com.wireguard.crypto.KeyPair
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Experimental Cloudflare consumer WARP registration.
 *
 * Uses an unofficial, undocumented client API. This may stop working without notice.
 * Must ONLY run after the user explicitly accepts Cloudflare Terms and clicks Register.
 * Keypair is generated on device and private key is never sent to the registration endpoint.
 * Only the public key is transmitted; resulting config is held in memory only.
 */
object WarpRegistration {
    fun register(): String {
        val keys = KeyPair()
        val payload = JSONObject()
            .put("key", keys.publicKey.toBase64())
            .put("tos", Instant.now().toString())
            .put("model", "MyVPN Android")
            .put("locale", "en_US")

        val connection = URL("https://api.cloudflareclient.com/v0a2158/reg")
            .openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("CF-Client-Version", "a-6.10-2158")
        connection.setRequestProperty("User-Agent", "MyVPN/0.4")
        try {
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                error("Cloudflare отклонил регистрацию (HTTP $status). API неофициальный.")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val response = JSONObject(body)
            val config = response.optJSONObject("config")
                ?: error("Cloudflare не предоставил WireGuard-конфигурацию")
            val address = config.optJSONObject("interface")?.optJSONObject("addresses")
                ?: error("В ответе нет адресов интерфейса")
            val v4 = address.optString("v4").takeIf { it.isNotBlank() }
                ?: error("Не получен IPv4-адрес")
            val v6 = address.optString("v6")
            val peers = config.optJSONArray("peers")
                ?: error("В ответе нет WireGuard-серверов")
            require(peers.length() > 0) { "Список WireGuard-серверов пуст" }
            val peer = peers.getJSONObject(0)
            val publicKey = peer.optString("public_key")
                .takeIf { it.isNotBlank() } ?: error("Нет публичного ключа Cloudflare")
            val endpointInfo = peer.optJSONObject("endpoint")
            val endpoint = endpointInfo?.optString("host")
                ?.takeIf { it.isNotBlank() }
                ?: "engage.cloudflareclient.com:2408"
            val addresses = buildList {
                add("$v4/32")
                if (v6.isNotBlank()) add("$v6/128")
            }.joinToString(", ")
            return """
                [Interface]
                PrivateKey = ${keys.privateKey.toBase64()}
                Address = $addresses
                DNS = 1.1.1.1, 1.0.0.1
                MTU = 1280

                [Peer]
                PublicKey = $publicKey
                AllowedIPs = 0.0.0.0/0, ::/0
                Endpoint = $endpoint
                PersistentKeepalive = 25
            """.trimIndent()
        } finally {
            connection.disconnect()
        }
    }
}
