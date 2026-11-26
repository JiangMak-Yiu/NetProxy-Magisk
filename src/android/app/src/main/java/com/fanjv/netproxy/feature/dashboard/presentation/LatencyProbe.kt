package com.fanjv.netproxy.feature.dashboard.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** 仪表盘「延迟目标」的一项：显示名 + 探测地址。 */
internal data class LatencyTarget(val label: String, val url: String)

internal const val LATENCY_TARGETS_KEY = "dashboard_latency_targets"

/** 默认三个目标：百度、Cloudflare、Google。 */
internal val DefaultLatencyTargets = listOf(
    LatencyTarget("Baidu", "https://www.baidu.com"),
    LatencyTarget("Cloudflare", "https://www.cloudflare.com"),
    LatencyTarget("Google", "https://www.google.com"),
)

/** 持久化格式：每行 `标签|URL`；无法解析时回落到默认目标。 */
internal fun parseLatencyTargets(raw: String?): List<LatencyTarget> {
    val parsed = raw.orEmpty()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val label = line.substringBefore('|').trim()
            val url = line.substringAfter('|', "").trim()
            if (url.isEmpty()) null else LatencyTarget(label.ifEmpty { hostOf(url) }, url)
        }
        .toList()
    return parsed.ifEmpty { DefaultLatencyTargets }
}

internal fun serializeLatencyTargets(targets: List<LatencyTarget>): String =
    targets.joinToString("\n") { "${it.label}|${it.url}" }

/** 从 URL 里取主机名，做新目标的默认标签。 */
internal fun hostOf(url: String): String =
    url.substringAfter("://").substringBefore('/').ifEmpty { url }

/** 用输入框里的 URL 列表重建目标，尽量沿用已有标签。 */
internal fun rebuildLatencyTargets(urls: List<String>, previous: List<LatencyTarget>): List<LatencyTarget> =
    urls.mapIndexedNotNull { index, raw ->
        val url = raw.trim()
        if (url.isEmpty()) return@mapIndexedNotNull null
        val existing = previous.getOrNull(index)
        LatencyTarget(
            label = existing?.label?.takeIf { existing.url == url } ?: hostOf(url),
            url = url,
        )
    }.ifEmpty { DefaultLatencyTargets }

private val latencyProbeClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .callTimeout(6, TimeUnit.SECONDS)
    .retryOnConnectionFailure(false)
    .build()

/** HEAD 请求探测一次往返延迟（毫秒）；失败或超时返回 null。 */
internal suspend fun probeLatencyMillis(url: String): Long? = withContext(Dispatchers.IO) {
    val request = runCatching { Request.Builder().url(url.toHttpUrl()).head().build() }.getOrNull()
        ?: return@withContext null
    val started = System.nanoTime()
    val response = runCatching { latencyProbeClient.newCall(request).execute() }.getOrNull()
        ?: return@withContext null
    response.use { }
    (System.nanoTime() - started) / 1_000_000
}
