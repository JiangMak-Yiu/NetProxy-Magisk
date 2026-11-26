package com.fanjv.netproxy.feature.dashboard.presentation

import com.fanjv.netproxy.core.module.ServiceStatusSnapshot

/** 累计流量偏移的持久化键：核心重启后把读数接上去。 */
internal const val TRAFFIC_TOTAL_DOWNLOAD_KEY = "dashboard_total_download"
internal const val TRAFFIC_TOTAL_UPLOAD_KEY = "dashboard_total_upload"

/** 将服务快照归并为仪表盘状态。 */
internal class DashboardSnapshotReducer(
    private val totalMemoryBytes: Long,
    initialDownloadOffset: Long = 0,
    initialUploadOffset: Long = 0,
    private val onOffsetChange: (download: Long, upload: Long) -> Unit = { _, _ -> },
) {
    private var lastCpuSample: Pair<Long, Long>? = null
    private var offsetDownload = initialDownloadOffset.coerceAtLeast(0)
    private var offsetUpload = initialUploadOffset.coerceAtLeast(0)
    private var lastRawDownload = 0L
    private var lastRawUpload = 0L

    fun reduce(
        current: CatalogDashboardUiState,
        service: ServiceStatusSnapshot,
        nowMillis: Long,
        localAddress: String
    ): CatalogDashboardUiState {
        val nowSeconds = nowMillis / 1000
        val displayedReadyAt = service.readyAt
        val displayedUptime = if (displayedReadyAt > 0 && service.state == "ready") {
            (nowSeconds - displayedReadyAt).coerceAtLeast(0)
        } else {
            service.uptimeSeconds
        }
        val cpuSample = service.processCpuTicks to service.systemCpuTicks
        val previousCpu = lastCpuSample
        val cpuUsage = if (previousCpu != null) {
            val processDelta = (cpuSample.first - previousCpu.first).coerceAtLeast(0)
            val systemDelta = (cpuSample.second - previousCpu.second).coerceAtLeast(0)
            if (systemDelta > 0) {
                (processDelta.toDouble() / systemDelta * service.cpuCount.coerceAtLeast(1) * 100)
                    .toFloat()
            } else 0f
        } else 0f
        val memoryUsage = if (totalMemoryBytes > 0) {
            service.memoryBytes.toFloat() / totalMemoryBytes.toFloat() * 100f
        } else 0f
        val traffic = service.downloadTotal to service.uploadTotal
        val rawDownload = traffic.first.coerceAtLeast(0)
        val rawUpload = traffic.second.coerceAtLeast(0)
        // 核心重启（模块升级/服务重启）后计数从 0 重来：把重启前的读数并进偏移，累计值继续往上走
        var offsetChanged = false
        if (rawDownload < lastRawDownload) {
            offsetDownload += lastRawDownload
            offsetChanged = true
        }
        if (rawUpload < lastRawUpload) {
            offsetUpload += lastRawUpload
            offsetChanged = true
        }
        if (offsetChanged) onOffsetChange(offsetDownload, offsetUpload)
        lastRawDownload = rawDownload
        lastRawUpload = rawUpload

        if (service.state == "ready") {
            lastCpuSample = cpuSample
        } else {
            lastCpuSample = null
        }

        return current.copy(
            loading = false,
            serviceState = service.state,
            serviceError = service.error,
            readyAt = displayedReadyAt,
            uptimeSeconds = displayedUptime,
            outboundMode = service.outboundMode,
            availableOutboundModes = service.availableOutboundModes.toList(),
            activeGroupId = service.activeGroupId,
            currentNode = dashboardNodeName(service),
            downloadTotal = offsetDownload + rawDownload,
            uploadTotal = offsetUpload + rawUpload,
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            internalIp = localAddress
        )
    }
}
