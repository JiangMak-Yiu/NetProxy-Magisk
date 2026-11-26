package com.fanjv.netproxy.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fanjv.netproxy.core.ui.component.BlurredBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 悬浮模式（iOS 液态玻璃）只给外边距，胶囊形状与滑动指示器由 GlassNavigationBar 负责。 */
private val FloatingBottomBarHorizontalMargin = 16.dp
private val FloatingBottomBarBottomMargin = 8.dp

/** 主界面唯一的底部导航栏；[floating] 为真时换成 Miuix 的液态玻璃悬浮条。 */
@Composable
internal fun MainBottomBar(
    mainState: MainPagerState,
    blurBackdrop: LayerBackdrop?,
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    if (floating) {
        val glassItems = remember(items) {
            items.map { item ->
                GlassNavigationItem(
                    icon = item.icon,
                    label = item.label,
                    contentDescription = item.label,
                )
            }
        }
        GlassNavigationBar(
            items = glassItems,
            selectedIndex = mainState.selectedPage,
            onSelect = { mainState.animateToPage(it) },
            backdrop = blurBackdrop,
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = FloatingBottomBarHorizontalMargin,
                    end = FloatingBottomBarHorizontalMargin,
                    bottom = FloatingBottomBarBottomMargin,
                ),
        )
        return
    }
    BlurredBar(backdrop = blurBackdrop) {
        NavigationBar(
            modifier = modifier.fillMaxWidth(),
            color = if (blurBackdrop != null) Color.Transparent else MiuixTheme.colorScheme.surface,
            content = {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        icon = item.icon,
                        label = item.label,
                        selected = mainState.selectedPage == index,
                        onClick = { mainState.animateToPage(index) },
                    )
                }
            },
        )
    }
}
