package com.nexus.ssh.core.ssh

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalBufferTest {
    @Test
    fun handlesNewLinesAndCarriageReturnOverwrite() {
        val buffer = TerminalBuffer()
        buffer.write("hello\rabc\nnext")
        assertEquals(listOf("abclo", "next"), buffer.snapshot())
    }

    @Test
    fun stripsAnsiColorAndSupportsEraseLine() {
        val buffer = TerminalBuffer()
        buffer.write("\u001B[31mred\u001B[0m text\r\u001B[2Kclean")
        assertEquals(listOf("clean"), buffer.snapshot())
    }

    @Test
    fun keepsOnlyConfiguredNumberOfLines() {
        val buffer = TerminalBuffer(maxLines = 2)
        buffer.write("one\ntwo\nthree")
        assertEquals(listOf("two", "three"), buffer.snapshot())
    }
}
