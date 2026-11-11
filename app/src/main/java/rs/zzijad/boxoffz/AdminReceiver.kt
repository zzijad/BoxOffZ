package rs.zzijad.boxoffz

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class AdminReceiver : DeviceAdminReceiver() {
    companion object {
        fun component(c: Context) = ComponentName(c, AdminReceiver::class.java)
        fun dpm(c: Context) = c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        fun isActive(c: Context) = dpm(c).isAdminActive(component(c))

        /** Uspavljuje box (isto kao taster za gašenje). */
        fun sleepBox(c: Context): Boolean =
            if (isActive(c)) runCatching { dpm(c).lockNow(); true }.getOrDefault(false) else false
    }
}
