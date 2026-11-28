package com.fanjv.netproxy.feature.apps.presentation

import org.junit.Assert.assertEquals
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
        assertEquals(
            "com.example.one\ncom.example.two\n10:com.example.work",
            serializeAppListEntries(entries),
        )
    }

    @Test
    fun exportedTextImportsBackToTheSameEntries() {
        val entries = listOf("0:com.example.one", "10:com.example.work")
        assertEquals(entries, parseAppListEntries(serializeAppListEntries(entries)))
    }

    @Test
    fun blankInputYieldsNothing() {
        assertEquals(emptyList<String>(), parseAppListEntries(""))
        assertEquals(emptyList<String>(), parseAppListEntries("\n  \n# only comments\n"))
    }
}
