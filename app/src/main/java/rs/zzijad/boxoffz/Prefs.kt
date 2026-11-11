package rs.zzijad.boxoffz

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("boxoffz", Context.MODE_PRIVATE)

    fun tvIp(c: Context): String = sp(c).getString("ip", "192.168.0.22")!!
    fun countdown(c: Context): Int = sp(c).getInt("secs", 5)
    fun sleepBox(c: Context): Boolean = sp(c).getBoolean("sleep_box", true)

    fun save(c: Context, ip: String, secs: Int, sleepBox: Boolean) =
        sp(c).edit().putString("ip", ip).putInt("secs", secs).putBoolean("sleep_box", sleepBox).apply()
}
