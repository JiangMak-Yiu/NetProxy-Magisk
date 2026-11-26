package com.fanjv.netproxy.feature.dashboard.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class LatencyTargetsTest {
    @Test
    fun emptyStoredValueFallsBackToBaiduCloudflareGoogle() {
        assertEquals(DefaultLatencyTargets, parseLatencyTargets(null))
        assertEquals(DefaultLatencyTargets, parseLatencyTargets(""))
        assertEquals(listOf("Baidu", "Cloudflare", "Google"), DefaultLatencyTargets.map { it.label })
    }

    @Test
    fun storedTargetsRoundTripWithLabels() {
        val targets = listOf(
            LatencyTarget("家里", "http://192.168.1.1"),
            LatencyTarget("VPS", "https://example.com"),
        )
        assertEquals(targets, parseLatencyTargets(serializeLatencyTargets(targets)))
    }

    @Test
    fun labelDefaultsToHostWhenStoredWithoutOne() {
        val parsed = parseLatencyTargets("|https://www.baidu.com")
        assertEquals("www.baidu.com", parsed.single().label)
    }

    @Test
    fun rebuildKeepsLabelOnlyWhileTheUrlMatches() {
        val previous = DefaultLatencyTargets
        val edited = rebuildLatencyTargets(
            listOf("https://www.baidu.com", "https://1.1.1.1", ""),
            previous,
        )
        assertEquals("Baidu", edited[0].label)
        assertEquals("1.1.1.1", edited[1].label)
        assertEquals(2, edited.size)
    }

    @Test
    fun clearingEverySlotRestoresDefaults() {
        assertEquals(DefaultLatencyTargets, rebuildLatencyTargets(listOf("", " ", ""), DefaultLatencyTargets))
    }
}
