package rs.zzijad.boxoffz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Pokretanje aplikacije = gašenje.
 * Kratko odbrojavanje (može se otkazati), zatim KEY_POWER TV-u i uspavljivanje boxa.
 */
class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var tvBig: TextView
    private lateinit var tvSub: TextView
    private var left = 0
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (left <= 0) { shutdown(); return }
            tvBig.text = left.toString()
            left--
            h.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = column()
        root.add(text("Gašenje TV-a i boxa", 30f, bold = true))
        tvBig = text("", 96f, R.color.accent, bold = true)
        root.add(tvBig, top = 8)
        tvSub = text("Pritisni OK za otkazivanje", 18f, R.color.muted)
        root.add(tvSub, top = 4)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cancel = button("Otkaži") { finish() }
        row.add(cancel, h = dp(52), side = 8)
        row.add(button("Ugasi odmah") { h.removeCallbacks(tick); shutdown() }, h = dp(52), side = 8)
        row.add(button("Podešavanja") {
            h.removeCallbacks(tick)
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }, h = dp(52), side = 8)
        root.add(row, top = 36)
        setContentView(root)
        cancel.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        if (running) return
        left = Prefs.countdown(this)
        h.removeCallbacks(tick)
        h.post(tick)
    }

    override fun onPause() {
        super.onPause()
        if (!running) h.removeCallbacks(tick)
    }

    private fun shutdown() {
        if (running) return
        running = true
        tvBig.text = "⏻"
        tvSub.text = "Gasim TV…"
        val ip = Prefs.tvIp(this)
        Thread {
            val err = HisenseMqtt.sendKey(ip, "KEY_POWER")
            h.post { afterTv(err) }
        }.start()
    }

    private fun afterTv(err: String?) {
        tvSub.text = if (err == null) "TV ugašen. Gasim box…" else "TV nije dostupan ($err). Gasim box…"
        h.postDelayed({
            if (Prefs.sleepBox(this) && AdminReceiver.sleepBox(this)) {
                finish()
            } else if (Prefs.sleepBox(this)) {
                tvSub.text = "Box nije ugašen – uključi „Gašenje boxa“ u Podešavanjima."
                h.postDelayed({ finish() }, 4000)
            } else finish()
        }, if (err == null) 1500 else 3000)
    }
}
