package com.gmvpn.client.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for [LogcatTail.filterByPid]. The full read path
 * shells out to /system/bin/logcat which only exists on Android, but
 * the parsing/filtering logic that protects us from the
 * "default-only-self" Android contract changing in a future release
 * is plain text — testable here.
 */
class LogcatTailTest {

    private val mine = 12345
    private val other = 67890

    @Test
    fun `keeps own pid lines and drops foreign`() {
        val lines = listOf(
            "05-06 08:30:00.123 $mine $mine D Tunnel: starting",
            "05-06 08:30:00.456 $other $other D Other: noise",
            "05-06 08:30:00.789 $mine $mine I Tunnel: connected",
        )
        val result = LogcatTail.filterByPid(lines, mine)
        assertEquals(2, result.kept.size)
        assertEquals(1, result.droppedCount)
        assertTrue(result.kept[0].contains("starting"))
        assertTrue(result.kept[1].contains("connected"))
    }

    @Test
    fun `unparseable lines pass through verbatim`() {
        val lines = listOf(
            "--------- beginning of main",
            "",
            "05-06 08:30:00.123 $mine $mine D Tunnel: ok",
        )
        val result = LogcatTail.filterByPid(lines, mine)
        assertEquals(3, result.kept.size)
        assertEquals(0, result.droppedCount)
    }

    @Test
    fun `lines with non-numeric pid column are kept verbatim`() {
        // Logcat may emit framing or future-format lines we don't
        // recognise; we'd rather show them than swallow them.
        val lines = listOf("not-a-log-line at all", "05-06 D nope")
        val result = LogcatTail.filterByPid(lines, mine)
        assertEquals(lines, result.kept)
        assertEquals(0, result.droppedCount)
    }

    @Test
    fun `unknown level letter is treated as not-a-log-line`() {
        val lines = listOf("05-06 08:30:00.123 $mine $mine X Tunnel: weird")
        val result = LogcatTail.filterByPid(lines, mine)
        // Better to keep an oddly-formatted line than silently drop a
        // log entry whose pid we cannot trust.
        assertEquals(1, result.kept.size)
        assertEquals(0, result.droppedCount)
    }

    @Test
    fun `all six standard levels are recognised`() {
        for (level in listOf('V', 'D', 'I', 'W', 'E', 'F')) {
            val line = "05-06 08:30:00.123 $other $other $level Other: noise"
            val result = LogcatTail.filterByPid(listOf(line), mine)
            assertEquals(
                "level $level should be parsed as a log line and dropped (foreign pid)",
                0,
                result.kept.size,
            )
            assertEquals(1, result.droppedCount)
        }
    }
}
