package com.nexus.ssh.core.ssh

/**
 * Minimal terminal emulator for line-oriented shells: handles CR/LF/BS/TAB, erase-line,
 * cursor left/right, delete/insert char and clear-screen, and swallows colour/OSC/other escape sequences.
 * Full-screen programs (vim, htop) are not supported.
 */
class TerminalBuffer(private val maxLines: Int = 1000) {

    private enum class Mode { NORMAL, ESC, CSI, OSC, OSC_ESC, SKIP_ONE }

    private val lines = ArrayList<StringBuilder>().apply { add(StringBuilder()) }
    private var col = 0
    private var mode = Mode.NORMAL
    private val csi = StringBuilder()

    fun clear() {
        resetScreen()
        mode = Mode.NORMAL
        csi.setLength(0)
    }

    fun snapshot(): List<String> = lines.map { it.toString() }

    /** Cursor column on the last (active) line. */
    val cursorColumn: Int get() = col

    fun write(text: String) {
        for (ch in text) process(ch)
    }

    private fun resetScreen() {
        lines.clear()
        lines.add(StringBuilder())
        col = 0
    }

    private fun process(ch: Char) {
        when (mode) {
            Mode.NORMAL -> normal(ch)
            Mode.ESC -> {
                when (ch) {
                    '[' -> {
                        csi.setLength(0)
                        mode = Mode.CSI
                    }
                    ']' -> mode = Mode.OSC
                    '(', ')', '*', '+', '#' -> mode = Mode.SKIP_ONE
                    else -> mode = Mode.NORMAL
                }
            }
            Mode.SKIP_ONE -> mode = Mode.NORMAL
            Mode.CSI -> {
                if (ch in '@'..'~') {
                    runCsi(ch)
                    mode = Mode.NORMAL
                } else {
                    csi.append(ch)
                }
            }
            Mode.OSC -> {
                if (ch == '\u0007') mode = Mode.NORMAL
                else if (ch == '\u001b') mode = Mode.OSC_ESC
            }
            Mode.OSC_ESC -> mode = Mode.NORMAL
        }
    }

    private fun normal(ch: Char) {
        when (ch) {
            '\u001b' -> mode = Mode.ESC
            '\n' -> newLine()
            '\r' -> col = 0
            '\b' -> if (col > 0) col--
            '\t' -> {
                val next = (col / 8 + 1) * 8
                while (col < next) put(' ')
            }
            else -> if (ch >= ' ' && ch != '\u007f') put(ch)
        }
    }

    private fun newLine() {
        lines.add(StringBuilder())
        col = 0
        while (lines.size > maxLines) lines.removeAt(0)
    }

    private fun put(ch: Char) {
        val line = lines[lines.size - 1]
        while (line.length < col) line.append(' ')
        if (col < line.length) line.setCharAt(col, ch) else line.append(ch)
        col++
    }

    private fun runCsi(fin: Char) {
        val params = csi.toString().trimStart('?', '>', '=', '!')
        val nums = params.split(';').map { it.toIntOrNull() }
        val first = nums.firstOrNull()
        val n = if (first == null || first < 1) 1 else first
        val line = lines[lines.size - 1]
        when (fin) {
            'K' -> when (first ?: 0) {
                0 -> if (col < line.length) line.setLength(col)
                1 -> for (i in 0 until minOf(col + 1, line.length)) line.setCharAt(i, ' ')
                else -> line.setLength(0)
            }
            'C' -> col += n
            'D' -> col = maxOf(0, col - n)
            'G' -> col = maxOf(0, n - 1)
            'P' -> if (col < line.length) line.delete(col, minOf(line.length, col + n))
            '@' -> {
                while (line.length < col) line.append(' ')
                line.insert(col, " ".repeat(n))
            }
            'J' -> if (first == 2 || first == 3) resetScreen()
            else -> {}
        }
    }
}
