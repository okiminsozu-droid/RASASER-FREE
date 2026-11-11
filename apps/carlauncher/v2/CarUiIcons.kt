package com.rasaser.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Native vector controls instead of device-dependent emoji glyphs.
 * Keeps all existing bottom-card geometry and accent colors.
 */
@Composable
fun CarUiIcon(kind: String, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.075f
        when (kind) {
            "radio" -> {
                // A small vintage radio, antenna, loudspeaker and tuning lines
                drawLine(tint, Offset(w * .20f, h * .20f), Offset(w * .72f, h * .08f), stroke)
                drawRoundRect(tint, Offset(w * .10f, h * .28f),
                    Size(w * .80f, h * .57f), CornerRadius(w * .10f, h * .10f),
                    style = Stroke(stroke))
                drawLine(tint, Offset(w * .22f, h * .47f), Offset(w * .55f, h * .47f), stroke)
                drawLine(tint, Offset(w * .22f, h * .61f), Offset(w * .48f, h * .61f), stroke)
                drawCircle(tint, w * .085f, Offset(w * .74f, h * .60f),
                    style = Stroke(stroke * .8f))
            }
            "apps" -> {
                for (x in 0..1) for (y in 0..1) {
                    drawRoundRect(tint, Offset(w * (.12f + .42f*x), h * (.12f + .42f*y)),
                        Size(w * .30f, h * .30f),
                        CornerRadius(w * .075f, h * .075f))
                }
            }
            "widget" -> {
                drawRoundRect(tint, Offset(w * .10f, h * .10f), Size(w * .78f, h * .78f),
                    CornerRadius(w * .10f, h * .10f), style = Stroke(stroke))
                drawRoundRect(tint, Offset(w * .20f, h * .20f), Size(w * .49f, h * .21f),
                    CornerRadius(w * .055f, h * .055f))
                drawRoundRect(tint, Offset(w * .20f, h * .52f), Size(w * .19f, h * .24f),
                    CornerRadius(w * .04f, h * .04f))
                drawRoundRect(tint, Offset(w * .48f, h * .52f), Size(w * .21f, h * .24f),
                    CornerRadius(w * .04f, h * .04f))
            }
            "power" -> {
                drawArc(tint, 135f, 270f, false,
                    Offset(w*.14f,h*.17f), Size(w*.72f,h*.72f), style=Stroke(stroke*1.15f))
                drawLine(tint, Offset(w*.50f,h*.08f), Offset(w*.50f,h*.53f),
                    stroke*1.15f)
            }
            "drive" -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w*.50f, h*.04f)
                    lineTo(w*.84f, h*.87f)
                    lineTo(w*.50f, h*.71f)
                    lineTo(w*.16f, h*.87f)
                    close()
                }
                drawPath(path, tint)
            }
        }
    }
}
