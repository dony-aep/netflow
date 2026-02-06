package com.netflow.netflow_traffic_stats

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

/**
 * Generador de iconos dinámicos con el valor de velocidad.
 */
object SpeedIconGenerator {
    private const val ICON_SIZE = 256

    fun generateSpeedIcon(speedBytesPerSecond: Long): IconCompat {
        val parts = formatSpeedParts(speedBytesPerSecond)
        val bitmap = createTextBitmap(parts.value, parts.unit)
        return IconCompat.createWithBitmap(bitmap)
    }

    private data class SpeedParts(val value: String, val unit: String)

    private fun formatSpeedParts(bytesPerSecond: Long): SpeedParts {
        if (bytesPerSecond <= 0) return SpeedParts("0", "B/s")

        return when {
            bytesPerSecond >= 1_000_000_000 -> {
                val value = bytesPerSecond / 1_000_000_000.0
                val valueStr = if (value >= 10) "${value.toInt()}" else String.format("%.1f", value)
                SpeedParts(valueStr, "GB/s")
            }
            bytesPerSecond >= 1_000_000 -> {
                val value = bytesPerSecond / 1_000_000.0
                val valueStr = if (value >= 10) "${value.toInt()}" else String.format("%.1f", value)
                SpeedParts(valueStr, "MB/s")
            }
            bytesPerSecond >= 1_000 -> {
                val value = bytesPerSecond / 1_000.0
                val valueStr = if (value >= 100) {
                    "${value.toInt()}"
                } else if (value >= 10) {
                    "${value.toInt()}"
                } else {
                    String.format("%.1f", value)
                }
                SpeedParts(valueStr, "KB/s")
            }
            else -> SpeedParts("$bytesPerSecond", "B/s")
        }
    }

    private fun createTextBitmap(value: String, unit: String): Bitmap {
        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val valueTextSize = when {
            value.length <= 2 -> ICON_SIZE * 0.58f
            value.length == 3 -> ICON_SIZE * 0.50f
            else -> ICON_SIZE * 0.42f
        }

        val unitTextSize = ICON_SIZE * 0.32f
        val centerX = ICON_SIZE / 2f

        paint.textSize = valueTextSize
        canvas.drawText(value, centerX, ICON_SIZE * 0.45f, paint)

        paint.textSize = unitTextSize
        canvas.drawText(unit, centerX, ICON_SIZE * 0.82f, paint)

        return bitmap
    }
}
