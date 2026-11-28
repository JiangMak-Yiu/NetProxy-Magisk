package com.fanjv.netproxy.feature.dashboard.presentation

import androidx.annotation.StringRes
import com.fanjv.netproxy.R

/** 仪表盘可编辑的小组件；顺序即渲染顺序，enabled 为是否显示。 */
internal enum class DashboardWidget(val key: String, @StringRes val labelRes: Int) {
    Speed("speed", R.string.dashboard_widget_speed),
    System("system", R.string.dashboard_widget_system),
    Latency("latency", R.string.dashboard_latency_targets),
    LandingIp("landing_ip", R.string.ip_card_title),
    Network("network", R.string.dashboard_widget_network),
    Route("route", R.string.dashboard_outbound_mode),
}

internal data class DashboardWidgetPref(val widget: DashboardWidget, val enabled: Boolean = true)

internal const val DASHBOARD_WIDGETS_KEY = "dashboard_widgets"

internal val DefaultDashboardWidgets: List<DashboardWidgetPref> =
    DashboardWidget.entries.map { DashboardWidgetPref(it) }

/** 持久化格式：每行一个 key，`!` 前缀表示隐藏；未记录的小组件追加到末尾并默认显示。 */
internal fun parseDashboardWidgets(raw: String?): List<DashboardWidgetPref> {
    val known = DashboardWidget.entries.associateBy { it.key }
    val parsed = raw.orEmpty()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { line ->
            val key = line.removePrefix("!")
            known[key]?.let { DashboardWidgetPref(it, enabled = !line.startsWith("!")) }
        }
        .distinctBy { it.widget }
        .toList()
    if (parsed.isEmpty()) return DefaultDashboardWidgets
    val missing = DashboardWidget.entries.filter { widget -> parsed.none { it.widget == widget } }
    return parsed + missing.map { DashboardWidgetPref(it) }
}

internal fun serializeDashboardWidgets(widgets: List<DashboardWidgetPref>): String =
    widgets.joinToString("\n") { (if (it.enabled) "" else "!") + it.widget.key }

internal fun moveDashboardWidget(
    widgets: List<DashboardWidgetPref>,
    from: Int,
    to: Int,
): List<DashboardWidgetPref> {
    if (from !in widgets.indices || to !in widgets.indices || from == to) return widgets
    return widgets.toMutableList().apply { add(to, removeAt(from)) }
}

internal fun toggleDashboardWidget(
    widgets: List<DashboardWidgetPref>,
    widget: DashboardWidget,
): List<DashboardWidgetPref> = widgets.map {
    if (it.widget == widget) it.copy(enabled = !it.enabled) else it
}
