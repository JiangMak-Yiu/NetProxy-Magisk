package com.fanjv.netproxy.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fanjv.netproxy.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** 仪表盘小组件编辑：开关显示、上下调整顺序，改一次存一次。 */
@Composable
internal fun DashboardWidgetEditorSheet(
    widgets: List<DashboardWidgetPref>,
    onMove: (Int, Int) -> Unit,
    onToggle: (DashboardWidget) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayBottomSheet(
        show = true,
        title = stringResource(R.string.dashboard_widgets),
        onDismissRequest = onDismiss,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            widgets.forEachIndexed { index, pref ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(pref.widget.labelRes),
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body1,
                    )
                    IconButton(onClick = { onMove(index, index - 1) }, enabled = index > 0) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.dashboard_widget_move_up),
                            tint = if (index > 0) colorScheme.onSurfaceVariantSummary
                            else colorScheme.onSurfaceVariantSummary.copy(alpha = 0.3f),
                        )
                    }
                    IconButton(
                        onClick = { onMove(index, index + 1) },
                        enabled = index < widgets.lastIndex
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.dashboard_widget_move_down),
                            tint = if (index < widgets.lastIndex) colorScheme.onSurfaceVariantSummary
                            else colorScheme.onSurfaceVariantSummary.copy(alpha = 0.3f),
                        )
                    }
                    Switch(
                        checked = pref.enabled,
                        onCheckedChange = { onToggle(pref.widget) },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                stringResource(R.string.dashboard_latency_reset),
                onReset,
                Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColors(textColor = colorScheme.onSurfaceVariantSummary),
            )
            TextButton(
                stringResource(R.string.common_done),
                onDismiss,
                Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
