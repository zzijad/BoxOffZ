# BoxOffZ

Android TV aplikacija za Mi Box S: jednim pokretanjem gasi **Hisense TV** (preko MQTT-a, port 36669) i **uspava box**.

- APK: [Releases → BoxOffZ.apk](https://github.com/zzijad/BoxOffZ/releases/latest/download/BoxOffZ.apk)
- Podešavanja: IP TV-a (podrazumevano 192.168.0.22), odbrojavanje, gašenje boxa.
- Gašenje boxa koristi Device Admin (`lockNow`). Ako box ne nudi ekran za uključivanje:
  `adb shell dpm set-active-admin rs.zzijad.boxoffz/.AdminReceiver`
