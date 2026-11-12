package dev.myvpn.personal

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * WireGuard/WARP experimental transport.
 *
 * IMPORTANT: GoBackend UP means the Android TUN was created, not that
 * Cloudflare performed a successful handshake. Verify real HTTPS traffic,
 * and tear down failed tunnels to restore ordinary Internet access.
 */
class WarpTunnel(context: Context) {
    private val backend = GoBackend(context.applicationContext)
    private val tunnel = object : Tunnel {
        override fun getName(): String = "MyVPN_WARP"
        override fun onStateChange(newState: Tunnel.State) {}
    }

    fun activate(configuration: String): String {
        val config = Config.parse(
            ByteArrayInputStream(configuration.toByteArray(StandardCharsets.UTF_8))
        )
        val state = backend.setState(tunnel, Tunnel.State.UP, config)
        if (state != Tunnel.State.UP) return "Не удалось включить WireGuard."
        try {
            // Attempt HTTPS inside the newly established tunnel. A working
            // registration plus an active interface do not prove data delivery.
            val checks = listOf(
                "https://www.cloudflare.com/cdn-cgi/trace",
                "https://one.one.one.one/cdn-cgi/trace"
            )
            var lastError = "Нет ответа от Cloudflare"
            for (url in checks) {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 4500
                    connection.readTimeout = 4500
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.useCaches = false
                    try {
                        if (connection.responseCode in 200..299) {
                            val body = connection.inputStream.bufferedReader().use { it.readText() }
                            if (body.lineSequence().any { it.trim() == "warp=on" }) {
                                return "WARP подключён: Cloudflare подтвердил warp=on."
                            }
                            lastError = "Cloudflare ответил, но warp=on не обнаружен"
                        } else lastError = "Cloudflare HTTP ${connection.responseCode}"
                    } finally {
                        connection.disconnect()
                    }
                } catch (e: Exception) {
                    lastError = "Не удалось получить данные через туннель (${e.javaClass.simpleName})"
                }
            }
            backend.setState(tunnel, Tunnel.State.DOWN, null)
            return "WARP отключён автоматически: $lastError. Интернет восстановлен."
        } catch (e: Exception) {
            // Fail closed with respect to the broken VPN, not to connectivity.
            runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            throw e
        }
    }

    fun deactivate(): String {
        backend.setState(tunnel, Tunnel.State.DOWN, null)
        return "WARP отключён"
    }

    fun isActive(): Boolean = backend.getState(tunnel) == Tunnel.State.UP
}
