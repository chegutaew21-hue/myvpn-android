package dev.myvpn.personal

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.VpnService
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors

/**
 * MyVPN 0.3: functional WireGuard tunnel for an imported WARP-compatible config.
 * VLESS diagnostic remains available, but chaining is not implemented.
 */
class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var warp: WarpTunnel
    private lateinit var status: TextView
    private lateinit var warpConfig: EditText
    private lateinit var warpButton: Button
    private var warpActive = false
    private val vpnRequestCode = 401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val scroller = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * density).toInt(), (24 * density).toInt(), (20 * density).toInt(), (32 * density).toInt())
            setBackgroundColor(Color.rgb(12,20,34))
        }
        scroller.addView(root)
        fun label(value: String, size: Float, color: Int = Color.WHITE): TextView =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        root.addView(label("MyVPN", 30f).apply { setTypeface(null, Typeface.BOLD) })
        root.addView(label("WARP — WireGuard", 18f, Color.LTGRAY))
        status = label("Отключено", 18f).apply {
            setPadding(0, (30*density).toInt(), 0, (16*density).toInt())
        }
        root.addView(status)
        root.addView(label("Вставь WireGuard-конфигурацию WARP в формате [Interface] / [Peer]", 14f, Color.LTGRAY))
        warpConfig = EditText(this).apply {
            hint = "[Interface]\nPrivateKey = ...\nAddress = ...\n\n[Peer]\nPublicKey = ...\nEndpoint = ..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            minLines = 5
            maxLines = 12
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        root.addView(warpConfig, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "Создать WARP автоматически (экспериментально)"
            setOnClickListener {
                android.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Регистрация WARP")
                    .setMessage(
                        "Будет создан новый ключ на телефоне и отправлен его публичный ключ " +
                        "в Cloudflare через неофициальный API. Перед продолжением ознакомься " +
                        "с условиями Cloudflare: https://www.cloudflare.com/application/terms/ . " +
                        "API может перестать работать. Продолжить?"
                    )
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Согласен") { _, _ ->
                        status.text = "Регистрирую устройство WARP..."
                        this.isEnabled = false
                        val button = this
                        executor.execute {
                            val result = runCatching { WarpRegistration.register() }
                            runOnUiThread {
                                button.isEnabled = true
                                result.onSuccess { config ->
                                    warpConfig.setText(config)
                                    status.text = "WARP-профиль создан. Нажми «Включить WARP»."
                                }.onFailure { e ->
                                    status.text = "Не удалось создать профиль: " +
                                        (e.message?.take(140) ?: e.javaClass.simpleName)
                                }
                            }
                        }
                    }
                    .show()
            }
        })
        warpButton = Button(this).apply {
            text = "Включить WARP"
            setOnClickListener {
                if (warpActive) {
                    runWarpAction(false)
                } else {
                    val configText = warpConfig.text.toString()
                    if (!configText.contains("[Interface]") || !configText.contains("[Peer]")) {
                        status.text = "Нужен полный WireGuard-профиль, а не токен аккаунта"
                    } else {
                        val request = VpnService.prepare(this@MainActivity)
                        if (request != null) {
                            startActivityForResult(request, vpnRequestCode)
                        } else {
                            runWarpAction(true)
                        }
                    }
                }
            }
        }
        root.addView(warpButton)
        root.addView(label("Внимание: текущий режим включает только WARP. Цепочка WARP → VLESS ещё не реализована.", 13f, Color.LTGRAY))
        root.addView(label("Диагностика VLESS", 18f).apply {
            setPadding(0,(35*density).toInt(),0,0)
        })
        val vlessLink = EditText(this).apply {
            hint = "vless://..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        root.addView(vlessLink)
        root.addView(Button(this).apply {
            text = "Проверить TCP до VPS"
            setOnClickListener {
                val profile = try { VlessProfile.parse(vlessLink.text.toString()) } catch (e: Exception) {
                    status.text = e.message ?: "Неверная VLESS-ссылка"
                    return@setOnClickListener
                }
                status.text = "Проверка TCP..."
                executor.execute {
                    val outcome = NetworkProbe.test(profile.host, profile.port)
                    runOnUiThread { status.text = outcome }
                }
            }
        })
        setContentView(scroller)
        try {
            warp = WarpTunnel(applicationContext)
        } catch (e: Exception) {
            status.text = "Не удалось запустить WireGuard: ${e.javaClass.simpleName}"
            warpButton.isEnabled = false
        }
    }

    @Deprecated("Compatibility callback for VpnService permission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == vpnRequestCode) {
            if (resultCode == RESULT_OK) runWarpAction(true)
            else status.text = "Разрешение VPN не предоставлено"
        }
    }

    private fun runWarpAction(enable: Boolean) {
        warpButton.isEnabled = false
        status.text = if (enable) "Запускаю WARP..." else "Отключаю WARP..."
        val configText = if (enable) warpConfig.text.toString() else ""
        executor.execute {
            val result = try {
                if (enable) warp.activate(configText) else warp.deactivate()
            } catch (e: Exception) {
                "Ошибка: ${e.javaClass.simpleName}: ${e.message?.take(150) ?: "нет деталей"}"
            }
            val running = try { warp.isActive() } catch (_: Exception) { false }
            runOnUiThread {
                warpActive = running
                warpButton.text = if (running) "Выключить WARP" else "Включить WARP"
                warpButton.isEnabled = true
                status.text = result
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
