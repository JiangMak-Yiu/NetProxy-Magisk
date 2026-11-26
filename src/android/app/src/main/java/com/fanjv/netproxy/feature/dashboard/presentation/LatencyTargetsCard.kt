package com.fanjv.netproxy.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fanjv.netproxy.R
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** 仪表盘「延迟目标」卡：三个可自定义目标的往返延迟，进入页面自动测一次。 */
@Composable
internal fun LatencyTargetsCard(
    targets: List<LatencyTarget>,
    values: Map<String, Long?>,
    testing: Boolean,
    onRefresh: () -> Unit,
    onSaveTargets: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_latency_targets),
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1,
            )
            IconButton(onClick = { editing = true }) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.dashboard_latency_edit),
                    tint = colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onRefresh, enabled = !testing) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.dashboard_latency_retest),
                    tint = if (testing) colorScheme.onSurfaceVariantSummary else colorScheme.primary,
                )
            }
        }
        targets.forEachIndexed { index, target ->
            if (index == 0) HorizontalDivider()
            InfoRow(
                title = target.label,
                content = latencyValueText(target.url, values, testing),
                icon = Icons.Rounded.Speed,
            )
        }
    }

    if (editing) {
        val first = rememberTextFieldState(targets.getOrNull(0)?.url.orEmpty())
        val second = rememberTextFieldState(targets.getOrNull(1)?.url.orEmpty())
        val third = rememberTextFieldState(targets.getOrNull(2)?.url.orEmpty())
        val dismiss = { editing = false }
        OverlayDialog(
            show = true,
            title = stringResource(R.string.dashboard_latency_targets),
            onDismissRequest = dismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(first, second, third).forEachIndexed { index, field ->
                    TextField(
                        state = field,
                        label = targets.getOrNull(index)?.label
                            ?: stringResource(R.string.dashboard_latency_slot, index + 1),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        stringResource(R.string.dashboard_latency_reset),
                        {
                            onSaveTargets(DefaultLatencyTargets.map { it.url })
                            dismiss()
                        },
                        Modifier.weight(1f),
                    )
                    TextButton(
                        stringResource(R.string.common_cancel),
                        dismiss,
                        Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(textColor = colorScheme.onSurfaceVariantSummary),
                    )
                    TextButton(
                        stringResource(R.string.save_text),
                        {
                            onSaveTargets(listOf(first.text.toString(), second.text.toString(), third.text.toString()))
                            dismiss()
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun latencyValueText(url: String, values: Map<String, Long?>, testing: Boolean): String {
    if (!values.containsKey(url)) {
        return if (testing) stringResource(R.string.dashboard_latency_testing) else "--"
    }
    val millis = values[url]
    return if (millis == null) stringResource(R.string.dashboard_latency_failed) else "$millis ms"
}
