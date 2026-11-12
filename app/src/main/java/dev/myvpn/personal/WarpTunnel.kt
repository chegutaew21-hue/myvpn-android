package dev.myvpn.personal

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Official WireGuard-Go Android backend.
 * Supports an imported WireGuard configuration (including a valid WARP WG profile).
 * This is WARP-ONLY mode, not a chained VLESS connection.
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
        return if (state == Tunnel.State.UP) "WireGuard-туннель запущен. Проверь warp=on." else "Туннель не запущен."
    }

    fun deactivate(): String {
        backend.setState(tunnel, Tunnel.State.DOWN, null)
        return "WARP отключён"
    }

    fun isActive(): Boolean = backend.getState(tunnel) == Tunnel.State.UP
}
