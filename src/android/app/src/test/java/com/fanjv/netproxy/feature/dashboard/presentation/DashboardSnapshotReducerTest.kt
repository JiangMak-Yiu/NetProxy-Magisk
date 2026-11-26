package com.fanjv.netproxy.feature.dashboard.presentation

import com.fanjv.netproxy.core.module.ServiceStatusSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardSnapshotReducerTest {
    @Test
    fun `computes resource usage from a service snapshot`() {
        val reducer = DashboardSnapshotReducer(totalMemoryBytes = 2_000)
        val initial = CatalogDashboardUiState()
        reducer.reduce(initial, service(download = 1_000, upload = 500), 1_000, "10.0.0.1")

        val state = reducer.reduce(
            initial,
            service(
                download = 3_000,
                upload = 1_500,
                processTicks = 30,
                systemTicks = 200,
                memory = 500
            ),
            3_000,
            "10.0.0.2"
        )

        assertEquals(3_000, state.downloadTotal)
        assertEquals(1_500, state.uploadTotal)
        assertEquals(40f, state.cpuUsage)
        assertEquals(25f, state.memoryUsage)
        assertEquals("10.0.0.2", state.internalIp)
    }

    @Test
    fun `uses service ready time as the only startup time`() {
        val reducer = DashboardSnapshotReducer(totalMemoryBytes = 1)

        val first = reducer.reduce(
            CatalogDashboardUiState(),
            service(pid = 10, readyAt = 95),
            101_000,
            "--"
        )
        val restarted = reducer.reduce(
            first,
            service(pid = 11, readyAt = 105),
            106_000,
            "--"
        )

        assertEquals(95, first.readyAt)
        assertEquals(6, first.uptimeSeconds)
        assertEquals(105, restarted.readyAt)
        assertEquals(1, restarted.uptimeSeconds)
    }

    @Test
    fun `shows the effective runtime outbound mode`() {
        val reducer = DashboardSnapshotReducer(totalMemoryBytes = 1)

        val state = reducer.reduce(
            CatalogDashboardUiState(),
            service(outboundMode = "Direct", configuredOutboundMode = "Rule"),
            1_000,
            "--"
        )

        assertEquals("Direct", state.outboundMode)
    }

    @Test
    fun `preserves custom modes from native status`() {
        val snapshot = service(outboundMode = "Office").copy(
            availableOutboundModes = listOf("Office", "Direct")
        )
        val state = DashboardSnapshotReducer(1).reduce(
            CatalogDashboardUiState(), snapshot, 1_000, "--"
        )
        assertEquals("Office", state.outboundMode)
        assertEquals(listOf("Office", "Direct"), state.availableOutboundModes)
    }

    @Test
    fun `accumulates totals across core restarts`() {
        var persistedDown = 0L
        var persistedUp = 0L
        var current = CatalogDashboardUiState()
        val reducer = DashboardSnapshotReducer(
            totalMemoryBytes = 1,
            onOffsetChange = { down, up ->
                persistedDown = down
                persistedUp = up
            },
        )

        current = reducer.reduce(current, service(download = 1_000, upload = 400), 1_000, "--")
        assertEquals(1_000, current.downloadTotal)
        assertEquals(400, current.uploadTotal)

        // 核心重启，计数归零：累计值不倒退
        current = reducer.reduce(current, service(download = 0, upload = 0), 2_000, "--")
        assertEquals(1_000, current.downloadTotal)
        assertEquals(400, current.uploadTotal)
        assertEquals(1_000, persistedDown)
        assertEquals(400, persistedUp)

        // 新会话继续往上加
        current = reducer.reduce(current, service(download = 500, upload = 100), 3_000, "--")
        assertEquals(1_500, current.downloadTotal)
        assertEquals(500, current.uploadTotal)
    }

    @Test
    fun `restores offsets from preferences`() {
        val state = DashboardSnapshotReducer(
            totalMemoryBytes = 1,
            initialDownloadOffset = 9_000,
            initialUploadOffset = 3_000,
        ).reduce(CatalogDashboardUiState(), service(download = 700, upload = 200), 1_000, "--")

        assertEquals(9_700, state.downloadTotal)
        assertEquals(3_200, state.uploadTotal)
    }

    private fun service(
        pid: Int = 10,
        readyAt: Long = 1,
        download: Long = 0,
        upload: Long = 0,
        processTicks: Long = 10,
        systemTicks: Long = 100,
        memory: Long = 0,
        outboundMode: String = "Rule",
        configuredOutboundMode: String = outboundMode
    ) = ServiceStatusSnapshot(
        state = "ready",
        pid = pid,
        readyAt = readyAt,
        downloadTotal = download,
        uploadTotal = upload,
        processCpuTicks = processTicks,
        systemCpuTicks = systemTicks,
        cpuCount = 2,
        memoryBytes = memory,
        activeGroupNodeCount = 1,
        outboundMode = outboundMode,
        configuredOutboundMode = configuredOutboundMode
    )
}
