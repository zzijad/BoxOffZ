package rs.zzijad.boxoffz

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class SettingsActivity : Activity() {
    private lateinit var etIp: EditText
    private lateinit var etSecs: EditText
    private lateinit var cbSleep: CheckBox
    private lateinit var tvAdmin: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnAdmin: android.widget.Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = column().apply { gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(48), dp(28), dp(48), dp(28)) }
        page.add(text("BoxOffZ – podešavanja", 28f, bold = true))

        page.add(label("IP adresa TV-a (Hisense)"), w = MATCH_PARENT, top = 20)
        etIp = field(Prefs.tvIp(this), InputType.TYPE_CLASS_TEXT)
        page.add(etIp, w = MATCH_PARENT, h = dp(52), top = 6)

        page.add(label("Odbrojavanje pre gašenja (sekunde, 0 = odmah)"), w = MATCH_PARENT, top = 16)
        etSecs = field(Prefs.countdown(this).toString(), InputType.TYPE_CLASS_NUMBER)
        page.add(etSecs, w = MATCH_PARENT, h = dp(52), top = 6)

        cbSleep = CheckBox(this).apply {
            text = "Ugasi i box posle TV-a"
            isChecked = Prefs.sleepBox(this@SettingsActivity)
            setTextColor(col(R.color.text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            buttonTintList = android.content.res.ColorStateList.valueOf(col(R.color.accent))
        }
        page.add(cbSleep, w = MATCH_PARENT, top = 12)

        tvAdmin = label("")
        page.add(tvAdmin, w = MATCH_PARENT, top = 8)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.add(button("Sačuvaj") { save(); finish() }, h = dp(52), side = 6)
        row.add(button("Test: ugasi TV") { save(); testTv() }, h = dp(52), side = 6)
        btnAdmin = button("") { toggleAdmin() }
        row.add(btnAdmin, h = dp(52), side = 6)
        page.add(row, top = 20)

        tvStatus = label("")
        page.add(tvStatus, w = MATCH_PARENT, top = 14)

        setContentView(ScrollView(this).apply { setBackgroundColor(col(R.color.bg)); addView(page) })
    }

    override fun onResume() {
        super.onResume()
        val on = AdminReceiver.isActive(this)
        tvAdmin.text = if (on) "Gašenje boxa: uključeno ✓"
        else "Gašenje boxa: nije uključeno. Ako dugme ne radi, preko ADB-a:\n" +
            "adb shell dpm set-active-admin rs.zzijad.boxoffz/.AdminReceiver"
        tvAdmin.setTextColor(col(if (on) R.color.ok else R.color.muted))
        btnAdmin.text = if (on) "Isključi gašenje boxa" else "Uključi gašenje boxa"
    }

    private fun label(s: String) = text(s, 16f, R.color.muted).apply { gravity = Gravity.START }

    private fun field(v: String, type: Int) = EditText(this).apply {
        setText(v)
        inputType = type
        setSingleLine(true)
        setTextColor(col(R.color.text))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        setBackgroundResource(R.drawable.bg_field)
        setPadding(dp(16), 0, dp(16), 0)
    }

    private fun save() {
        val secs = etSecs.text.toString().toIntOrNull()?.coerceIn(0, 60) ?: 5
        Prefs.save(this, etIp.text.toString().trim(), secs, cbSleep.isChecked)
    }

    private fun testTv() {
        tvStatus.setTextColor(col(R.color.muted))
        tvStatus.text = "Šaljem KEY_POWER na ${Prefs.tvIp(this)}…"
        val ip = Prefs.tvIp(this)
        Thread {
            val err = HisenseMqtt.sendKey(ip, "KEY_POWER")
            runOnUiThread {
                tvStatus.setTextColor(col(if (err == null) R.color.ok else R.color.error))
                tvStatus.text = if (err == null) "Poslato ✓" else "Greška: $err"
            }
        }.start()
    }

    private fun toggleAdmin() {
        val c = AdminReceiver.component(this)
        if (AdminReceiver.isActive(this)) {
            AdminReceiver.dpm(this).removeActiveAdmin(c)
            tvAdmin.postDelayed({ onResume() }, 500)
            return
        }
        val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, c)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.admin_desc))
        runCatching { startActivity(i) }.onFailure {
            tvStatus.setTextColor(col(R.color.error))
            tvStatus.text = "Box ne nudi ovaj ekran – uključi preko ADB komande iznad."
        }
    }
}
