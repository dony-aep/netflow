package com.donyaep.netflow.core.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.donyaep.netflow.core.monitoring.TrafficFormatter
import com.donyaep.netflow.data.model.SpeedUnit
import kotlin.math.max

object DynamicSpeedIconFactory {
    private const val ICON_SIZE = 256

    fun create(
        context: Context,
        downloadSpeedBytesPerSecond: Long,
        uploadSpeedBytesPerSecond: Long,
        speedUnit: SpeedUnit,
    ): Bitmap {
        val peakSpeed = max(downloadSpeedBytesPerSecond, uploadSpeedBytesPerSecond)
        val label = compactLabel(peakSpeed, speedUnit)

        val bitmap = createBitmap(ICON_SIZE, ICON_SIZE)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            isFakeBoldText = true
            textSize = when {
                label.value.length <= 2 -> ICON_SIZE * 0.52f
                label.value.length == 3 -> ICON_SIZE * 0.44f
                else                   -> ICON_SIZE * 0.38f
            }
        }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            textSize = ICON_SIZE * 0.28f
        }

        val centerX = ICON_SIZE / 2f
        canvas.drawText(label.value, centerX, ICON_SIZE * 0.46f, valuePaint)
        canvas.drawText(label.unit,  centerX, ICON_SIZE * 0.82f, unitPaint)

        return bitmap
    }

    private fun compactLabel(bytesPerSecond: Long, speedUnit: SpeedUnit): SpeedLabel {
        val useBits = speedUnit == SpeedUnit.BitsPerSecond
        val value = if (useBits) bytesPerSecond * 8 else bytesPerSecond
        val base = if (useBits) 1000.0 else 1024.0
        val suffixes = if (useBits) listOf("b", "Kb", "Mb", "Gb") else listOf("B", "KB", "MB", "GB")

        var scaled = value.toDouble()
        var index = 0
        while (scaled >= base && index < suffixes.lastIndex) {
            scaled /= base
            index += 1
        }

        val renderedValue = when {
            scaled >= 100 -> scaled.toInt().toString()
            scaled >= 10  -> "%.0f".format(scaled)
            else          -> "%.1f".format(scaled)
        }
        return SpeedLabel(renderedValue, "${suffixes[index]}/s")
    }

    private data class SpeedLabel(
        val value: String,
        val unit: String,
    )
}