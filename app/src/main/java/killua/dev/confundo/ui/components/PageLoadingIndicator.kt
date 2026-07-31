package killua.dev.confundo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PageLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // 使用默认尺寸：ContainedLoadingIndicator 的容器会自适应内部指示器。
        // 之前强制 size(118.dp) 会把圆形容器撑成巨大一块、而内部形状仍是小尺寸，看起来是坏的。
        ContainedLoadingIndicator()
    }
}
