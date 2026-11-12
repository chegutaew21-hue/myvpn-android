package dev.myvpn.personal

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

/**
 * Network diagnostics milestone.
 * Does NOT create a VPN tunnel, and never claims to be connected.
 */
class MainActivity : Activity() {
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (36 * density).toInt(), (24 * density).toInt(), 0)
            setBackgroundColor(Color.rgb(12, 20, 34))
        }
        fun label(value: String, size: Float, color: Int = Color.WHITE) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        root.addView(label("MyVPN", 30f).apply { setTypeface(null, Typeface.BOLD) })
        root.addView(label("Диагностика VLESS Reality", 16f, Color.LTGRAY))
        val status = label("VPN не подключён", 18f).apply {
            setPadding(0, (40*density).toInt(), 0, (18*density).toInt())
        }
        root.addView(status)
        val link = EditText(this).apply {
            hint = "Вставь vless:// ссылку из 3x-ui"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        root.addView(link, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        var profile: VlessProfile? = null
        val test = Button(this).apply {
            text = "Проверить доступность VPS"
            isEnabled = false
        }
        root.addView(Button(this).apply {
            text = "Проверить конфигурацию"
            setOnClickListener {
                try {
                    profile = VlessProfile.parse(link.text.toString())
                    val valid = profile!!
                    // Ensure config is well-formed without exposing credentials in UI.
                    valid.singBoxJson()
                    status.text = "Reality-профиль распознан. VPN пока выключен."
                    test.isEnabled = true
                } catch (e: Exception) {
                    profile = null
                    test.isEnabled = false
                    status.text = e.message ?: "Некорректная ссылка"
                }
            }
        })
        root.addView(test)
        test.setOnClickListener {
            val value = profile ?: return@setOnClickListener
            test.isEnabled = false
            status.text = "Проверка TCP до VPS..."
            backgroundExecutor.execute {
                val message = NetworkProbe.test(value.host, value.port)
                runOnUiThread {
                    status.text = message
                    test.isEnabled = true
                }
            }
        }
        root.addView(label(
            "Эта версия проверяет ссылку и доступность сервера. Реальное VPN-подключение появится после интеграции libbox.",
            14f, Color.LTGRAY).apply {
                setPadding(0, (26*density).toInt(), 0, 0)
            })
        setContentView(root)
    }
    override fun onDestroy() {
        backgroundExecutor.shutdownNow()
        super.onDestroy()
    }
}
