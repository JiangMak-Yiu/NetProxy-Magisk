package com.fanjv.netproxy.feature.dashboard.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fanjv.netproxy.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** 仪表盘「落地 IP」卡：摘要一行，点开底部弹窗看完整质量信息。 */
@Composable
internal fun LandingIpCard(
    info: LandingIpInfo?,
    loading: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.ip_card_title),
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.body1,
            )
            IconButton(onClick = { if (info != null) showDetails = true }, enabled = info != null) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.ip_details),
                    tint = if (info != null) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onRefresh, enabled = !loading) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.ip_retest),
                    tint = if (loading) colorScheme.onSurfaceVariantSummary else colorScheme.primary,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when {
                loading -> Text(
                    text = stringResource(R.string.ip_querying),
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary,
                )

                info == null -> Text(
                    text = message.ifBlank { stringResource(R.string.ip_hint) },
                    style = MiuixTheme.textStyles.body2,
                    color = colorScheme.onSurfaceVariantSummary,
                )

                else -> {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = colorScheme.primary,
                    )
                    val country = "${countryFlag(info.countryCode)} ${info.country.ifBlank { info.countryCode }}".trim()
                    val level = levelLabel(info.level)
                    val type = typeLabel(info.networkType)
                    val summary = listOfNotNull(
                        country,
                        info.city.takeIf { it.isNotBlank() },
                        level,
                        type,
                        info.asn.takeIf { it.isNotBlank() },
                    ).joinToString(" · ")
                    Text(
                        text = summary,
                        modifier = Modifier.weight(1f),
                        style = MiuixTheme.textStyles.body2,
                        color = levelColor(info.level),
                    )
                }
            }
        }
    }

    if (showDetails && info != null) {
        OverlayBottomSheet(
            show = true,
            title = stringResource(R.string.ip_card_title),
            onDismissRequest = { showDetails = false },
        ) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                val tagsText = if (info.tags.isEmpty()) {
                    stringResource(R.string.ip_tags_none)
                } else {
                    info.tags.map { tagLabel(it) }.joinToString(" / ")
                }
                DetailRow(stringResource(R.string.ip_address), info.ip)
                HorizontalDivider()
                DetailRow(stringResource(R.string.ip_level), levelLabel(info.level), levelColor(info.level))
                HorizontalDivider()
                DetailRow(stringResource(R.string.ip_type), typeLabel(info.networkType))
                HorizontalDivider()
                DetailRow(stringResource(R.string.ip_tags), tagsText)
                HorizontalDivider()
                DetailRow(stringResource(R.string.ip_org), info.isp.ifBlank { "--" })
                HorizontalDivider()
                DetailRow(stringResource(R.string.ip_asn), info.asn.ifBlank { "--" })
                HorizontalDivider()
                DetailRow(
                    stringResource(R.string.ip_source),
                    info.source.ifBlank { "--" } + " · " + stringResource(R.string.ip_elapsed, info.elapsedMillis),
                )
            }
            Text(
                text = stringResource(R.string.ip_disclaimer),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                style = MiuixTheme.textStyles.footnote2,
                color = colorScheme.onSurfaceVariantSummary,
            )
            TextButton(
                text = stringResource(R.string.ip_retest),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                onClick = {
                    showDetails = false
                    onRefresh()
                },
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = colorScheme.onSurfaceVariantActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MiuixTheme.textStyles.body2, color = colorScheme.onSurfaceVariantSummary)
        Text(value, style = MiuixTheme.textStyles.body2, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun levelLabel(level: IpRiskLevel): String = when (level) {
    IpRiskLevel.CLEAN -> stringResource(R.string.ip_level_clean)
    IpRiskLevel.NORMAL -> stringResource(R.string.ip_level_normal)
    IpRiskLevel.RISKY -> stringResource(R.string.ip_level_risky)
}

@Composable
private fun typeLabel(type: IpNetworkType): String = when (type) {
    IpNetworkType.RESIDENTIAL -> stringResource(R.string.ip_type_residential)
    IpNetworkType.DATACENTER -> stringResource(R.string.ip_type_datacenter)
    IpNetworkType.MOBILE -> stringResource(R.string.ip_type_mobile)
    IpNetworkType.BUSINESS -> stringResource(R.string.ip_type_business)
    IpNetworkType.UNKNOWN -> stringResource(R.string.ip_type_unknown)
}

@Composable
private fun tagLabel(tag: IpRiskTag): String = when (tag) {
    IpRiskTag.PROXY -> stringResource(R.string.ip_tag_proxy)
    IpRiskTag.VPN -> stringResource(R.string.ip_tag_vpn)
    IpRiskTag.TOR -> stringResource(R.string.ip_tag_tor)
}

@Composable
private fun levelColor(level: IpRiskLevel): Color = when (level) {
    IpRiskLevel.CLEAN -> Color(0xFF32A852)
    IpRiskLevel.NORMAL -> Color(0xFFE39A20)
    IpRiskLevel.RISKY -> if (MiuixTheme.isDynamicColor) colorScheme.error else Color(0xFFF05252)
}
