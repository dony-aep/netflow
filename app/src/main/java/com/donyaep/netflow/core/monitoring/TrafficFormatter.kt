package com.donyaep.netflow.core.monitoring

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object TrafficFormatter {
    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) {
            return "$bytes B"
        }

        val units = listOf("KB", "MB", "GB", "TB")
        val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.size)
        val scaledValue = bytes / 1024.0.pow(digitGroup.toDouble())
        val unit = units[digitGroup - 1]
        return String.format(Locale.US, "%.1f %s", scaledValue, unit)
    }

    fun formatBits(bits: Long): String {
        if (bits < 1000) {
            return "$bits b"
        }

        val units = listOf("Kb", "Mb", "Gb", "Tb")
        val digitGroup = (ln(bits.toDouble()) / ln(1000.0)).toInt().coerceAtMost(units.size)
        val scaledValue = bits / 1000.0.pow(digitGroup.toDouble())
        val unit = units[digitGroup - 1]
        return String.format(Locale.US, "%.1f %s", scaledValue, unit)
    }

    fun formatSpeed(bytesPerSecond: Long, useBits: Boolean = false): String {
        return if (useBits) {
            "${formatBits(bytesPerSecond * 8)}/s"
        } else {
            "${formatBytes(bytesPerSecond)}/s"
        }
    }
}