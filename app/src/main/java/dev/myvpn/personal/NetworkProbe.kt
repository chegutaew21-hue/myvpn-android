package dev.myvpn.personal

import java.net.InetSocketAddress
import java.net.Socket

/** Simple TCP availability check, NOT a Reality handshake. */
object NetworkProbe {
    fun test(host: String, port: Int, timeoutMs: Int = 5000): String =
        try {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            "TCP доступен: ${elapsed} мс. Reality ещё не проверен."
        } catch (e: Exception) {
            val type = e.javaClass.simpleName
            "TCP не подключился ($type). Проверь сеть и доступность VPS."
        }
}
