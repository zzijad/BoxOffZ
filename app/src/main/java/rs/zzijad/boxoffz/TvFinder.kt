package rs.zzijad.boxoffz

import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Traži Hisense TV u lokalnoj mreži (IP mu se menja preko DHCP-a).
 * Skenira /24 mrežu boxa na MQTT portu TV-a i proverava da li je tamo zaista MQTT broker.
 */
object TvFinder {
    private const val CONNECT_TIMEOUT_MS = 600

    /** Vraća IP pronađenog TV-a ili null. [fallbackIp] se koristi samo ako se mreža boxa ne može odrediti. */
    fun find(fallbackIp: String): String? {
        val prefixes = localPrefixes().ifEmpty { listOf(fallbackIp.substringBeforeLast('.')) }
        for (prefix in prefixes) {
            val tv = openHosts(prefix).firstOrNull { HisenseMqtt.probe(it) }
            if (tv != null) return tv
        }
        return null
    }

    private fun localPrefixes(): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress }
            .map { it.hostAddress!!.substringBeforeLast('.') }
            .distinct()
    }.getOrDefault(emptyList())

    private fun openHosts(prefix: String): List<String> {
        val pool = Executors.newFixedThreadPool(64)
        try {
            val tasks = (1..254).map { i ->
                Callable<String?> {
                    val host = "$prefix.$i"
                    try {
                        Socket().use { it.connect(InetSocketAddress(host, HisenseMqtt.PORT), CONNECT_TIMEOUT_MS) }
                        host
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            return pool.invokeAll(tasks).mapNotNull { runCatching { it.get() }.getOrNull() }
        } finally {
            pool.shutdownNow()
        }
    }
}
