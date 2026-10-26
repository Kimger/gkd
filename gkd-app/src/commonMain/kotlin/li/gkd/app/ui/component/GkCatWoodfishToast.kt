package li.gkd.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import li.gkd.app.resources.*
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

class CatWoodfishToastState {
    // Identity distinguishes repeated requests of the same size, so every preview restarts.
    class Request(val size: Int)

    var request by mutableStateOf<Request?>(null)
        private set

    fun show(size: Int) {
        request = Request(size)
    }

    fun dismiss(expected: Request) {
        if (request === expected) request = null
    }
}

/** Shared Android/Desktop artwork. No background, pointer handlers, sound or business state. */
@Composable
fun GkCatWoodfishToast(
    modifier: Modifier = Modifier,
    bodySize: Int = 40,
    onFinished: () -> Unit = {},
) {
    val timeline = remember { Animatable(0f) }
    val finished = rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        // This transient notification has a fixed wall-clock lifetime, including its fade-out.
        withContext(object : MotionDurationScale {
            override val scaleFactor = 1f
        }) {
            timeline.animateTo(1800f, tween(1800, easing = LinearEasing))
        }
        finished.value()
    }
    val time = timeline.value
    val opacity = (time / 120f).coerceIn(0f, 1f) *
        ((1800f - time) / 450f).coerceIn(0f, 1f)
    val strike = when {
        time < 160f -> 18f * time / 160f
        time < 340f -> 18f - 46f * (time - 160f) / 180f
        time < 490f -> -28f + 35f * (time - 340f) / 150f
        time < 640f -> 7f * (640f - time) / 150f
        else -> 0f
    }
    val squash = when {
        time < 340f -> 0f
        time < 390f -> (time - 340f) / 50f
        time < 550f -> (550f - time) / 160f
        else -> 0f
    }
    val labelProgress = ((time - 340f) / 1000f).coerceIn(0f, 1f)
    val label = stringResource(Res.string.action_toast_woodfish_merit)
    val body = painterResource(Res.drawable.cat_woodfish)
    val artworkScale = bodySize.coerceIn(20, 80) / 80f
    Box(
        modifier.size(maxOf(96f, 144f * artworkScale).dp, (32f + 108f * artworkScale).dp)
            .graphicsLayer { alpha = opacity }
            .semantics(mergeDescendants = true) {
                contentDescription = label
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            label,
            modifier = Modifier.align(Alignment.TopCenter)
                .offset(y = (12f - labelProgress * 10f).dp)
                .graphicsLayer { alpha = ((time - 340f) / 140f).coerceIn(0f, 1f) },
            style = TextStyle(
                color = Color(0xFFFFD486),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(Color(0xCC43362B), Offset(0f, 1f), 3f),
            ),
        )
        Canvas(Modifier.size((128f * artworkScale).dp, (108f * artworkScale).dp)) {
            scale(size.width / 128f, size.height / 108f, pivot = Offset.Zero) {
                translate(16f, 38f) {
                    scale(1f + squash * 0.035f, 1f - squash * 0.065f, Offset(48f, 64f)) {
                        with(body) { draw(Size(96f, 64f)) }
                    }
                }
                // Separate mallet pivots at the handle: its head actually contacts the forehead.
                rotate(strike, Offset(110f, 58f)) {
                    drawLine(
                        Brush.linearGradient(
                            listOf(Color(0xFFD39449), Color(0xFFFFDDA2), Color(0xFFCA873E)),
                            Offset(90f, 35f), Offset(96f, 31f),
                        ),
                        Offset(77f, 22f), Offset(110f, 58f),
                        strokeWidth = 7f, cap = StrokeCap.Round,
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFE8BC), Color(0xFFE8B775), Color(0xFFBF8645)),
                            center = Offset(71f, 14f), radius = 18f,
                        ),
                        radius = 10.5f, center = Offset(74f, 18f),
                    )
                }
            }
        }
    }
}
