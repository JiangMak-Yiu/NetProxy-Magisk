package com.fanjv.netproxy.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fanjv.netproxy.core.ui.component.BlurredBar
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 悬浮模式的圆角与外边距：与设置页开关「悬浮导航栏」对应。 */
private val FloatingBottomBarShape = RoundedCornerShape(28.dp)
private val FloatingBottomBarHorizontalMargin = 16.dp
private val FloatingBottomBarBottomMargin = 8.dp

/** 主界面唯一的 Miuix 底部导航栏；[floating] 为真时收进圆角悬浮块。 */
@Composable
internal fun MainBottomBar(
    mainState: MainPagerState,
    blurBackdrop: LayerBackdrop?,
    items: List<NavigationItem>,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    val bar: @Composable (Modifier) -> Unit = { barModifier ->
        BlurredBar(backdrop = blurBackdrop) {
            NavigationBar(
                modifier = barModifier.fillMaxWidth(),
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
    if (floating) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = FloatingBottomBarHorizontalMargin,
                    end = FloatingBottomBarHorizontalMargin,
                    bottom = FloatingBottomBarBottomMargin,
                )
                .clip(FloatingBottomBarShape),
        ) {
            bar(Modifier)
        }
    } else {
        bar(modifier)
    }
}
