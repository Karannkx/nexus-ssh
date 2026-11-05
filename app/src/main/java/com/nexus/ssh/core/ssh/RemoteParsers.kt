package com.nexus.ssh.core.ssh

/** One round-trip script; prints "[section]" headers followed by raw command output. */
val TELEMETRY_SCRIPT = """
echo '[os]'; grep '^PRETTY_NAME=' /etc/os-release 2>/dev/null | cut -d= -f2- | tr -d '"'
echo '[kernel]'; uname -sr
echo '[host]'; uname -n
echo '[uptime]'; uptime -p 2>/dev/null || uptime
echo '[load]'; cut -d' ' -f1-3 /proc/loadavg
echo '[cores]'; nproc 2>/dev/null || grep -c '^processor' /proc/cpuinfo
echo '[mem]'; grep -E '^(MemTotal|MemAvailable):' /proc/meminfo
echo '[disk]'; df -P -k / | tail -n 1
echo '[cpu1]'; head -n 1 /proc/stat; echo '[net1]'; cat /proc/net/dev 2>/dev/null
sleep 1
echo '[cpu2]'; head -n 1 /proc/stat; echo '[net2]'; cat /proc/net/dev 2>/dev/null
""".trimIndent()

data class Telemetry(
    val os: String,
    val kernel: String,
    val hostname: String,
    val uptime: String,
    val loadAvg: String,
    val cores: Int,
    val cpuPercent: Float,
    val memTotalKb: Long,
    val memUsedKb: Long,
    val diskTotalKb: Long,
    val diskUsedKb: Long,
    /** Network receive / transmit rate in bytes per second (physical interfaces only). */
    val rxBytesPerSec: Float = 0f,
    val txBytesPerSec: Float = 0f
)

private val SECTION = Regex("^\\[([a-z0-9]+)]$")
private val WS = Regex("\\s+")

private fun cpuTimes(line: String?): Pair<Long, Long>? {
    val p = line?.trim()?.split(WS) ?: return null
    if (p.size < 5 || p[0] != "cpu") return null
    val v = p.drop(1).take(8).map { it.toLongOrNull() ?: 0L }
    val idle = v.getOrElse(3) { 0L } + v.getOrElse(4) { 0L }
    return Pair(v.sum(), idle)
}

private fun netTotals(lines: List<String>?): Pair<Long, Long>? {
    if (lines == null) return null
    var rx = 0L
    var tx = 0L
    var any = false
    for (l in lines) {
        val idx = l.indexOf(':')
        if (idx < 0) continue
        val iface = l.substring(0, idx).trim()
        // skip loopback and virtual bridges so traffic isn't counted twice
        if (iface == "lo" || iface.startsWith("veth") || iface.startsWith("docker") ||
            iface.startsWith("br-") || iface.startsWith("virbr")
        ) continue
        val f = l.substring(idx + 1).trim().split(WS)
        if (f.size < 9) continue
        rx += f[0].toLongOrNull() ?: 0L
        tx += f[8].toLongOrNull() ?: 0L
        any = true
    }
    return if (any) Pair(rx, tx) else null
}

fun parseTelemetry(raw: String): Telemetry? {
    val sections = HashMap<String, MutableList<String>>()
    var current: MutableList<String>? = null
    for (line in raw.lineSequence()) {
        val t = line.trim()
        val m = SECTION.matchEntire(t)
        if (m != null) {
            current = sections.getOrPut(m.groupValues[1]) { mutableListOf() }
        } else if (t.isNotEmpty()) {
            current?.add(t)
        }
    }
    if (sections.isEmpty()) return null

    fun one(key: String): String = sections[key]?.firstOrNull() ?: "—"

    val a = cpuTimes(sections["cpu1"]?.firstOrNull())
    val b = cpuTimes(sections["cpu2"]?.firstOrNull())
    var cpu = 0f
    if (a != null && b != null) {
        val dTotal = b.first - a.first
        val dIdle = b.second - a.second
        if (dTotal > 0) cpu = ((dTotal - dIdle) * 100f / dTotal).coerceIn(0f, 100f)
    }

    var memTotal = 0L
    var memAvailable = 0L
    for (l in sections["mem"].orEmpty()) {
        val parts = l.split(WS)
        if (parts.size < 2) continue
        val value = parts[1].toLongOrNull() ?: continue
        when (parts[0].trimEnd(':')) {
            "MemTotal" -> memTotal = value
            "MemAvailable" -> memAvailable = value
        }
    }

    var diskTotal = 0L
    var diskUsed = 0L
    val disk = sections["disk"]?.lastOrNull()?.split(WS)?.takeLast(5)
    if (disk != null && disk.size == 5) {
        diskTotal = disk[0].toLongOrNull() ?: 0L
        diskUsed = disk[1].toLongOrNull() ?: 0L
    }

    var rxRate = 0f
    var txRate = 0f
    val n1 = netTotals(sections["net1"])
    val n2 = netTotals(sections["net2"])
    if (n1 != null && n2 != null) {
        // both samples are taken ~1 second apart by the script
        rxRate = (n2.first - n1.first).coerceAtLeast(0L).toFloat()
        txRate = (n2.second - n1.second).coerceAtLeast(0L).toFloat()
    }

    return Telemetry(
        os = one("os"),
        kernel = one("kernel"),
        hostname = one("host"),
        uptime = one("uptime"),
        loadAvg = one("load"),
        cores = one("cores").toIntOrNull() ?: 0,
        cpuPercent = cpu,
        memTotalKb = memTotal,
        memUsedKb = (memTotal - memAvailable).coerceAtLeast(0L),
        diskTotalKb = diskTotal,
        diskUsedKb = diskUsed,
        rxBytesPerSec = rxRate,
        txBytesPerSec = txRate
    )
}

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val state: String,
    val status: String
) {
    val running: Boolean get() = state == "running"
}

const val DOCKER_PS_ARGS = "ps -a --format '{{.ID}}|{{.Names}}|{{.Image}}|{{.State}}|{{.Status}}'"

fun parseDockerPs(out: String): List<DockerContainer> =
    out.lines().mapNotNull { line ->
        val p = line.trim().split('|', limit = 5)
        if (p.size == 5) DockerContainer(p[0], p[1], p[2], p[3], p[4]) else null
    }

data class ServiceUnit(
    val name: String,
    val active: String,
    val sub: String,
    val description: String
) {
    val running: Boolean get() = active == "active" && sub == "running"
}

const val LIST_SERVICES_CMD =
    "systemctl list-units --type=service --all --no-pager --no-legend --plain"

fun parseServices(out: String): List<ServiceUnit> =
    out.lines().mapNotNull { line ->
        val p = line.trim().split(WS, limit = 5)
        if (p.size >= 4 && p[0].endsWith(".service")) {
            ServiceUnit(p[0], p[2], p[3], p.getOrElse(4) { "" })
        } else null
    }.sortedBy { it.name }
