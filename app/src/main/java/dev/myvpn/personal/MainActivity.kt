package dev.myvpn.personal

import android.app.Activity
import android.content.Context
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

/**
 * Offline configuration prototype. This version cannot connect to a VPN.
 * A real VPN service and tested transport will be added in a later milestone.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (36 * density).toInt(), (24 * density).toInt(), 0)
            setBackgroundColor(Color.rgb(12, 20, 34))
        }
        fun label(message: String, size: Float, color: Int = Color.WHITE): TextView {
            return TextView(this).apply {
                text = message
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        root.addView(label("MyVPN", 30f).apply { setTypeface(null, Typeface.BOLD) })
        root.addView(label("Личный VPN-клиент", 16f, Color.LTGRAY))
        val status = label("VPN не подключён", 20f).apply {
            setPadding(0, (48 * density).toInt(), 0, (24 * density).toInt())
        }
        root.addView(status)

        val link = EditText(this).apply {
            hint = "Вставь ссылку vless://"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
        }
        root.addView(
            link,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        link.setText(getPreferences(Context.MODE_PRIVATE).getString("vless", ""))

        val save = Button(this).apply {
            text = "Проверить и сохранить ссылку"
            setOnClickListener {
                val value = link.text.toString().trim()
                val parsed = try {
                    android.net.Uri.parse(value)
                } catch (_: Exception) {
                    null
                }
                if (parsed?.scheme == "vless" && !parsed.host.isNullOrBlank() && parsed.port in 1..65535) {
                    getPreferences(Context.MODE_PRIVATE).edit().putString("vless", value).apply()
                    status.text = "Профиль сохранён локально"
                } else {
                    status.text = "Проверь формат ссылки VLESS"
                }
            }
        }
        root.addView(save)
        root.addView(label(
            "Демо-версия: интерфейс и проверка VLESS-ссылки. VPN-движок ещё не подключён.",
            14f,
            Color.LTGRAY
        ).apply {
            setPadding(0, (32 * density).toInt(), 0, 0)
        })
        setContentView(root)
    }
}
