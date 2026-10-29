package com.cyz.wxyd.ui.captcha

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyz.wxyd.WxydApp
import com.cyz.wxyd.ui.common.appFactory
import kotlin.math.roundToInt

/** 后端原图尺寸（`SliderCaptchaService.BG_W / BG_H`）。 */
private const val CAPTCHA_W = 300f
private const val CAPTCHA_H = 150f

/** 拼图块原图左边缘最大值（H5：`pieceMax = BG_W - PIECE = 256`，提交时 +4）。 */
private const val PIECE_MAX_X = CAPTCHA_W - 44f

/**
 * 原生滑块验证码（spec 约束 13、计划 Task 5）。
 *
 * 复刻 H5 `register.html` 的 `#bgCanvas` + `#knob` 交互：
 * - 底图按容器宽度等比缩放（300→容器宽），拼图块按同比例偏移；
 * - 手指托在滑轨把手上拖，拼图块按比例跟随（滑轨满行程 ↔ 拼图满行程）；
 * - 松手提交 `x = 拼图屏幕left / scale + 4`；把手拖到最右提交 `256 + 4 = 260`；
 * - 验证失败自动换新题并在上方提示（ticket 一次性、失败即毁题，见 [CaptchaViewModel]）。
 *
 * 成功后调用 [onVerified]（只会调用一次）；[onFailed] 在题目自身加载失败时调用，
 * 由外层决定展示方式（一般是 Snackbar）。
 */
@Composable
fun SliderCaptcha(
    app: WxydApp,
    onVerified: (ticket: String) -> Unit,
    onFailed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: CaptchaViewModel = viewModel(factory = appFactory(app) { CaptchaViewModel(it.container) })
    val state by vm.state.collectAsState()

    // ticket 一次性：仅在首次进入 Success 时上报，重组不重复触发
    val successTicket = (state as? CaptchaState.Success)?.ticket
    LaunchedEffect(successTicket) {
        if (successTicket != null) onVerified(successTicket)
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val scale = with(density) { maxWidth.toPx() } / CAPTCHA_W
        val canvasHeight: Dp = with(density) { (CAPTCHA_H * scale).toDp() }

        when (val s = state) {
            CaptchaState.Loading -> LoadingBox(canvasHeight)

            CaptchaState.Verifying -> LoadingBox(canvasHeight)

            is CaptchaState.LoadFailed -> Column(
                Modifier.fillMaxWidth().height(canvasHeight + 56.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onFailed(s.message) }) { Text("重试") }
            }

            is CaptchaState.Success -> Box(
                Modifier.fillMaxWidth().height(canvasHeight + 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("验证通过 ✓", color = MaterialTheme.colorScheme.primary)
            }

            is CaptchaState.Ready -> Column {
                s.notice?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                CaptchaBody(
                    ready = s,
                    scale = scale,
                    canvasHeight = canvasHeight,
                    onRefresh = { vm.load() },
                    onRelease = vm::submit,
                )
            }
        }
    }
}

@Composable
private fun LoadingBox(height: Dp) {
    Box(Modifier.fillMaxWidth().height(height + 56.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CaptchaBody(
    ready: CaptchaState.Ready,
    scale: Float,
    canvasHeight: Dp,
    onRefresh: () -> Unit,
    onRelease: (originalX: Int) -> Unit,
) {
    var pieceLeft by remember(ready.captchaId) { mutableFloatStateOf(0f) }
    var knobLeft by remember(ready.captchaId) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box {
        Image(
            bitmap = ready.bg,
            contentDescription = "验证码底图",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        )
        Image(
            bitmap = ready.piece,
            contentDescription = "拼图块",
            modifier = Modifier
                .offset { IntOffset(pieceLeft.roundToInt(), (ready.pieceY * scale).roundToInt()) }
                .size(with(density) { (52f * scale).toDp() }),
        )
    }

    val knobSize = with(density) { 40.dp.toPx() }
    val pieceMaxScreen = PIECE_MAX_X * scale

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(44.dp).padding(top = 8.dp),
    ) {
        val trackW = with(density) { maxWidth.toPx() }
        val knobMax = (trackW - knobSize).coerceAtLeast(1f)
        val ratio = pieceMaxScreen / knobMax

        Box(
            Modifier.fillMaxSize().background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp),
            ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "按住滑块，拖动到右侧完成拼图",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp),
            )
            Box(
                Modifier
                    .offset { IntOffset(knobLeft.roundToInt(), 0) }
                    .size(with(density) { knobSize.toDp() })
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                    .pointerInput(ready.captchaId) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val atRight = knobLeft >= knobMax - 2f
                                val originalX = if (atRight) {
                                    (PIECE_MAX_X + 4f).roundToInt()
                                } else {
                                    (pieceLeft / scale + 4f).roundToInt()
                                }
                                onRelease(originalX)
                            },
                            onDragCancel = {
                                knobLeft = 0f
                                pieceLeft = 0f
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            knobLeft = (knobLeft + dragAmount).coerceIn(0f, knobMax)
                            pieceLeft = (knobLeft * ratio).coerceIn(0f, pieceMaxScreen)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("›", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onRefresh) {
            Text("换一张", style = MaterialTheme.typography.bodySmall)
        }
    }
}
