package app.aemu.core

import android.net.LocalSocket
import android.system.Os
import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Host-side stand-in for netd's dnsproxyd (Android 5.0–7.x). Guest libc sends every getaddrinfo/gethostbyname
 * there; the real netd resolves through a per-network interface and marks that do not exist in the sandbox, so
 * every lookup ended in "unknown host". Here the phone resolves the name and the reply is written in the same
 * binary layout DnsProxyListener uses. Netd creates the socket file itself, so [ensure] takes the path over again
 * whenever netd (re)starts.
 */
class DnsProxy(private val paths: VmPaths, private val log: (String) -> Unit) {
    private val server = UnixServer(paths.socket("dnsproxyd"), "dnsproxy") { c -> serve(c) }
    private var inode = -1L
    @Volatile var queries = 0L
        private set

    @Synchronized
    fun ensure() {
        val f = paths.socket("dnsproxyd")
        val ino = runCatching { Os.stat(f.absolutePath).st_ino }.getOrDefault(-2L)
        if (server.running && ino == inode) return
        server.stop()
        if (server.start(log)) inode = runCatching { Os.stat(f.absolutePath).st_ino }.getOrDefault(-1L)
    }

    fun stop() { server.stop(); inode = -1 }

    private fun serve(c: LocalSocket) {
        val req = ByteArrayOutputStream()
        val inp = c.inputStream
        while (req.size() < 1024) {
            val b = inp.read()
            if (b <= 0) break
            req.write(b)
        }
        val t = req.toString("UTF-8").trim().split(' ').filter { it.isNotEmpty() }
        if (t.isEmpty()) return
        queries++
        val out = c.outputStream
        try {
            when (t[0]) {
                "getaddrinfo" -> if (t.size >= 6) getaddrinfo(out, t) else fail(out)
                "gethostbyname" -> hostByName(out, t)
                else -> fail(out)
            }
        } catch (_: Throwable) { runCatching { fail(out) } }
        out.flush()
    }

    private fun fail(o: java.io.OutputStream) {
        o.write(byteArrayOf('4'.code.toByte(), '0'.code.toByte(), '1'.code.toByte(), 0))
        o.write(be(4)); o.write(be(EAI_NODATA))
    }

    private fun be(v: Int) = byteArrayOf((v ushr 24).toByte(), (v ushr 16).toByte(), (v ushr 8).toByte(), v.toByte())

    private fun ok(o: java.io.OutputStream) = o.write(byteArrayOf('2'.code.toByte(), '2'.code.toByte(), '2'.code.toByte(), 0))

    private fun lenData(o: java.io.OutputStream, d: ByteArray) { o.write(be(d.size)); if (d.isNotEmpty()) o.write(d) }

    /** getaddrinfo host serv flags family socktype protocol [netid] */
    private fun getaddrinfo(o: java.io.OutputStream, t: List<String>) {
        val host = t[1].takeIf { it != "^" }
        val serv = t[2].takeIf { it != "^" }
        val flags = t[3].toIntOrNull() ?: 0
        val family = t[4].toIntOrNull() ?: 0
        val socktype = t[5].toIntOrNull() ?: 0
        val port = when (serv) {
            null -> 0
            "http" -> 80; "https" -> 443; "ftp" -> 21; "domain" -> 53
            else -> serv.toIntOrNull() ?: 0
        }
        val addrs: List<InetAddress> = when {
            host == null -> listOf(InetAddress.getByAddress(if (flags and AI_PASSIVE != 0) byteArrayOf(0, 0, 0, 0) else byteArrayOf(127, 0, 0, 1)))
            else -> {
                val all = InetAddress.getAllByName(host).toList()
                val v4 = all.filterIsInstance<Inet4Address>()
                when (family) {
                    AF_INET -> v4
                    AF_INET6 -> all.filterIsInstance<Inet6Address>()
                    else -> v4.ifEmpty { all }   // no IPv6 route inside the guest: prefer IPv4 for AF_UNSPEC
                }
            }
        }
        if (addrs.isEmpty()) return fail(o)
        val types = if (socktype != 0) listOf(socktype) else listOf(1, 2, 3)
        ok(o)
        var first = true
        for (a in addrs) for (st in types) {
            o.write(be(1))
            o.write(be(flags)); o.write(be(if (a is Inet6Address) AF_INET6 else AF_INET))
            o.write(be(st)); o.write(be(when (st) { 1 -> 6; 2 -> 17; else -> 0 }))
            lenData(o, sockaddr(a, port))
            lenData(o, if (first && flags and AI_CANONNAME != 0 && host != null) (host + "\u0000").toByteArray() else ByteArray(0))
            first = false
        }
        o.write(be(0))
    }

    private fun sockaddr(a: InetAddress, port: Int): ByteArray {
        val raw = a.address
        return if (a is Inet6Address) ByteArray(28).also {
            it[0] = AF_INET6.toByte(); it[2] = (port ushr 8).toByte(); it[3] = port.toByte()
            System.arraycopy(raw, 0, it, 8, 16)
        } else ByteArray(16).also {
            it[0] = AF_INET.toByte(); it[2] = (port ushr 8).toByte(); it[3] = port.toByte()
            System.arraycopy(raw, 0, it, 4, 4)
        }
    }

    /** gethostbyname name af netid (5.0 orders the arguments differently; the name is the non-numeric one) */
    private fun hostByName(o: java.io.OutputStream, t: List<String>) {
        val args = t.drop(1)
        val name = args.firstOrNull { !it.all { ch -> ch.isDigit() } } ?: args.firstOrNull() ?: return fail(o)
        val af = args.filter { it != name }.mapNotNull { it.toIntOrNull() }.firstOrNull { it == AF_INET || it == AF_INET6 } ?: AF_INET
        if (af != AF_INET) return fail(o)
        val v4 = InetAddress.getAllByName(name).filterIsInstance<Inet4Address>()
        if (v4.isEmpty()) return fail(o)
        ok(o)
        lenData(o, (name + "\u0000").toByteArray())
        lenData(o, ByteArray(0))                    // no aliases
        o.write(be(AF_INET)); o.write(be(4))
        for (a in v4) lenData(o, a.address.copyOf(16))
        lenData(o, ByteArray(0))
    }

    private companion object {
        const val AF_INET = 2
        const val AF_INET6 = 10
        const val AI_PASSIVE = 1
        const val AI_CANONNAME = 2
        const val EAI_NODATA = 7
    }
}
