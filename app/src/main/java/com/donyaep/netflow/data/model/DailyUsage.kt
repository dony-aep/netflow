package com.donyaep.netflow.data.model

data class DailyUsage(
    val date: String,
    val wifiReceivedBytes: Long = 0,
    val wifiSentBytes: Long = 0,
    val mobileReceivedBytes: Long = 0,
    val mobileSentBytes: Long = 0,
) {
    val wifiTotalBytes: Long = wifiReceivedBytes + wifiSentBytes
    val mobileTotalBytes: Long = mobileReceivedBytes + mobileSentBytes
    val totalReceivedBytes: Long = wifiReceivedBytes + mobileReceivedBytes
    val totalSentBytes: Long = wifiSentBytes + mobileSentBytes
    val totalBytes: Long = wifiTotalBytes + mobileTotalBytes

    companion object {
        fun empty(date: String): DailyUsage = DailyUsage(date = date)
    }
}