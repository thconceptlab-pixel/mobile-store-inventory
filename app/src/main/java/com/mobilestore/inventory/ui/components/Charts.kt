package com.mobilestore.inventory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Minimal Compose Canvas bar chart — used for the Reports screen's profit
 * trend and top-brand/top-model breakdowns instead of a third-party
 * charting library, to keep the app's dependency footprint small.
 */
@Composable
fun SimpleBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    negativeColor: Color = MaterialTheme.colorScheme.error,
    height: androidx.compose.ui.unit.Dp = 160.dp
) {
    if (data.isEmpty()) return
    val maxAbs = data.maxOf { kotlin.math.abs(it.second) }.let { if (it == 0.0) 1.0 else it }

    Column(modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val barCount = data.size
            val spacing = 6.dp.toPx()
            val barWidth = (size.width - spacing * (barCount - 1)) / barCount
            val zeroY = size.height / 2f

            data.forEachIndexed { index, (_, value) ->
                val barHeight = (kotlin.math.abs(value) / maxAbs).toFloat() * (size.height / 2f - 8.dp.toPx())
                val x = index * (barWidth + spacing)
                val top = if (value >= 0) zeroY - barHeight else zeroY
                drawRoundRect(
                    color = if (value >= 0) barColor else negativeColor,
                    topLeft = Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight.coerceAtLeast(2f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
            // Zero line
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(0f, zeroY),
                end = Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        Row(Modifier.fillMaxWidth()) {
            // Only label every other bar if there are many, to avoid crowding
            val step = if (data.size > 8) 2 else 1
            data.forEachIndexed { index, (label, _) ->
                if (index % step == 0) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f * step)
                    )
                }
            }
        }
    }
}

/** Horizontal ranked bars used for Top Selling Brands / Top Selling Models. */
@Composable
fun RankedBarList(
    items: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    if (items.isEmpty()) return
    val max = items.maxOf { it.second }.coerceAtLeast(1)
    Column(modifier) {
        items.forEach { (label, count) ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                Box(Modifier.weight(1f).height(18.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val fraction = count.toFloat() / max.toFloat()
                        drawRoundRect(
                            color = barColor,
                            size = androidx.compose.ui.geometry.Size(size.width * fraction, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(count.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
