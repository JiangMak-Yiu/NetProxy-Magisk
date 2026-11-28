package com.fanjv.netproxy.feature.dashboard.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardWidgetsTest {
    @Test
    fun emptyStoredValueFallsBackToEveryWidgetShown() {
        assertEquals(DefaultDashboardWidgets, parseDashboardWidgets(null))
        assertEquals(DefaultDashboardWidgets, parseDashboardWidgets(""))
        assertEquals(DashboardWidget.entries.toList(), DefaultDashboardWidgets.map { it.widget })
        assertTrue(DefaultDashboardWidgets.all { it.enabled })
    }

    @Test
    fun orderAndVisibilityRoundTrip() {
        val widgets = listOf(
            DashboardWidgetPref(DashboardWidget.Network),
            DashboardWidgetPref(DashboardWidget.Speed, enabled = false),
        )
        val parsed = parseDashboardWidgets(serializeDashboardWidgets(widgets))
        assertEquals(DashboardWidget.Network, parsed.first().widget)
        assertEquals(DashboardWidget.Speed, parsed[1].widget)
        assertFalse(parsed[1].enabled)
    }

    @Test
    fun unknownKeysAreDroppedAndMissingWidgetsAppended() {
        val parsed = parseDashboardWidgets("bogus\nspeed")
        assertEquals(DashboardWidget.Speed, parsed.first().widget)
        assertEquals(DashboardWidget.entries.size, parsed.size)
        assertEquals(DashboardWidget.entries.toSet(), parsed.map { it.widget }.toSet())
    }

    @Test
    fun moveSwapsWithinBoundsOnly() {
        val widgets = DefaultDashboardWidgets
        assertEquals(
            listOf(DashboardWidget.System, DashboardWidget.Speed),
            moveDashboardWidget(widgets, 0, 1).take(2).map { it.widget },
        )
        assertEquals(widgets, moveDashboardWidget(widgets, 0, -1))
        assertEquals(widgets, moveDashboardWidget(widgets, widgets.lastIndex, widgets.size))
    }

    @Test
    fun toggleFlipsOnlyTheTargetedWidget() {
        val toggled = toggleDashboardWidget(DefaultDashboardWidgets, DashboardWidget.Latency)
        assertFalse(toggled.single { it.widget == DashboardWidget.Latency }.enabled)
        assertTrue(toggled.single { it.widget == DashboardWidget.Speed }.enabled)
    }
}
