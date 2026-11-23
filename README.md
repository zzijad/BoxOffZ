# BoxOffZ

Android TV aplikacija za Mi Box S: jednim pokretanjem gasi **Hisense TV** (preko MQTT-a, port 36669) i **uspava box**.

- APK: [Releases → BoxOffZ.apk](https://github.com/zzijad/BoxOffZ/releases/latest/download/BoxOffZ.apk)
- Podešavanja: IP TV-a (podrazumevano 192.168.0.22), odbrojavanje, gašenje boxa.
- Ako se IP TV-a promeni (DHCP), aplikacija ga sama nalazi skeniranjem mreže na portu 36669 i pamti novi IP
  (i pri gašenju i preko dugmeta „Pronađi TV“ u Podešavanjima).
- Gašenje boxa koristi Device Admin (`lockNow`). Ako box ne nudi ekran za uključivanje:
  `adb shell dpm set-active-admin rs.zzijad.boxoffz/.AdminReceiver`
