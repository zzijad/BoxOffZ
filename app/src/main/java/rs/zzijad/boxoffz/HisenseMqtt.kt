package rs.zzijad.boxoffz

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Minimalni MQTT 3.1.1 klijent za ugrađeni broker Hisense (VIDAA) TV-a.
 * Port 36669, korisnik hisenseservice / multimqttservice.
 * Stariji firmveri (npr. H58A6100) rade bez TLS-a; ako to ne uspe, probamo TLS.
 */
object HisenseMqtt {
    const val PORT = 36669
    private const val USER = "hisenseservice"
    private const val PASS = "multimqttservice"
    private const val CLIENT_ID = "BoxOffZ"
    private const val TIMEOUT_MS = 3000

    /** Šalje taster TV-u. Vraća null ako je uspelo, inače opis greške. */
    fun sendKey(host: String, key: String): String? {
        val plain = runCatching { send(openPlain(host), key) }
        if (plain.isSuccess) return null
        val tls = runCatching { send(openTls(host), key) }
        if (tls.isSuccess) return null
        return plain.exceptionOrNull()?.message ?: "nepoznata greška"
    }

    private fun openPlain(host: String): Socket =
        Socket().apply {
            connect(InetSocketAddress(host, PORT), TIMEOUT_MS)
            soTimeout = TIMEOUT_MS
        }

    private fun openTls(host: String): Socket {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
            override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val ctx = SSLContext.getInstance("TLS").apply { init(null, trustAll, null) }
        val raw = openPlain(host)
        return (ctx.socketFactory.createSocket(raw, host, PORT, true) as SSLSocket).apply {
            soTimeout = TIMEOUT_MS
            startHandshake()
        }
    }

    private fun send(socket: Socket, key: String) {
        socket.use { s ->
            val out = s.getOutputStream()
            val inp = s.getInputStream()

            // CONNECT
            val vh = ByteArrayOutputStream().apply {
                str("MQTT")
                write(4)            // protokol 3.1.1
                write(0xC2)         // username + password + clean session
                write(0); write(60) // keep-alive 60 s
                str(CLIENT_ID); str(USER); str(PASS)
            }.toByteArray()
            packet(out, 0x10, vh)

            // CONNACK: 20 02 00 rc
            val ack = ByteArray(4)
            readFully(inp, ack)
            if ((ack[0].toInt() and 0xF0) != 0x20) error("TV nije odgovorio kao MQTT")
            if (ack[3].toInt() != 0) error("TV je odbio vezu (kod ${ack[3]})")

            // PUBLISH QoS 0
            val topic = "/remoteapp/tv/remote_service/$CLIENT_ID\$normal/actions/sendkey"
            val pub = ByteArrayOutputStream().apply {
                str(topic)
                write(key.toByteArray())
            }.toByteArray()
            packet(out, 0x30, pub)

            Thread.sleep(300)
            out.write(byteArrayOf(0xE0.toByte(), 0)) // DISCONNECT
            out.flush()
        }
    }

    private fun ByteArrayOutputStream.str(s: String) {
        val b = s.toByteArray()
        write(b.size shr 8); write(b.size and 0xFF); write(b)
    }

    private fun packet(out: OutputStream, type: Int, body: ByteArray) {
        val buf = ByteArrayOutputStream()
        buf.write(type)
        var len = body.size
        do {
            var d = len % 128
            len /= 128
            if (len > 0) d = d or 0x80
            buf.write(d)
        } while (len > 0)
        buf.write(body)
        out.write(buf.toByteArray())
        out.flush()
    }

    private fun readFully(inp: InputStream, b: ByteArray) {
        var n = 0
        while (n < b.size) {
            val r = inp.read(b, n, b.size - n)
            if (r < 0) error("TV je prekinuo vezu")
            n += r
        }
    }
}
