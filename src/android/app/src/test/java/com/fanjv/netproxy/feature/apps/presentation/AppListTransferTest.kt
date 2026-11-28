package com.fanjv.netproxy.feature.apps.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppListTransferTest {
    @Test
    fun barePackagesGoToPrimaryUserAndNoiseIsDropped() {
        val parsed = parseAppListEntries(
            """
            # 备注行
            com.example.one
            com.example.two   # 行尾备注

            not a package
            com.example.one
            10:com.example.work
            wrong:com.example.other
            """.trimIndent()
        )
        assertEquals(
            listOf("0:com.example.one", "0:com.example.two", "10:com.example.work"),
            parsed,
        )
    }

    @Test
    fun serializeDropsThePrimaryUserPrefixAndStaysATextList() {
        val entries = listOf("10:com.example.work", "0:com.example.two", "0:com.example.one")
        val text = serializeAppListEntries(entries)
        assertEquals(
            setOf("com.example.one", "com.example.two", "10:com.example.work"),
            text.lines().toSet(),
        )
        assertFalse(text.lines().any { it.startsWith("0:") })
    }

    @Test
    fun exportedTextImportsBackToTheSameEntries() {
        val entries = listOf("0:com.example.one", "10:com.example.work")
        assertEquals(entries.toSet(), parseAppListEntries(serializeAppListEntries(entries)).toSet())
    }

    @Test
    fun blankInputYieldsNothing() {
        assertEquals(emptyList<String>(), parseAppListEntries(""))
        assertEquals(emptyList<String>(), parseAppListEntries("\n  \n# only comments\n"))
    }
}
