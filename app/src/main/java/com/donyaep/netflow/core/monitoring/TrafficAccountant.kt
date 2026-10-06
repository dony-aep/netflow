package com.donyaep.netflow.core.monitoring

import com.donyaep.netflow.data.model.TrafficSnapshot

/**
 * Convierte muestras sucesivas de los contadores del sistema en bytes consumidos
 * por red y velocidad suavizada. No depende de Android, para poder probarla.
 */
class TrafficAccountant(private val speedBufferSize: Int = 3) {

    data class Sample(
        val wifiRxDelta: Long = 0L,
        val wifiTxDelta: Long = 0L,
        val mobileRxDelta: Long = 0L,
        val mobileTxDelta: Long = 0L,
        val downloadSpeed: Long = 0L,
        val uploadSpeed: Long = 0L,
    ) {
        val totalRxDelta: Long get() = wifiRxDelta + mobileRxDelta
        val totalTxDelta: Long get() = wifiTxDelta + mobileTxDelta
    }

    private val downloadSpeedBuffer = ArrayDeque<Long>()
    private val uploadSpeedBuffer = ArrayDeque<Long>()
    private var lastSnapshot: TrafficSnapshot? = null
    private var lastElapsedMs: Long = 0L
    private var lastNetworkType: NetworkType = NetworkType.None

    /** Fija el punto de partida al empezar a monitorear o tras reiniciar los contadores. */
    fun start(snapshot: TrafficSnapshot, elapsedMs: Long, networkType: NetworkType) {
        clear()
        lastSnapshot = snapshot
        lastElapsedMs = elapsedMs
        lastNetworkType = networkType
    }

    /** Mueve el punto de partida sin contar nada. Se usa al cambiar de día. */
    fun rebase(snapshot: TrafficSnapshot, elapsedMs: Long) {
        lastSnapshot = snapshot
        lastElapsedMs = elapsedMs
        downloadSpeedBuffer.clear()
        uploadSpeedBuffer.clear()
    }

    fun clear() {
        downloadSpeedBuffer.clear()
        uploadSpeedBuffer.clear()
        lastSnapshot = null
        lastElapsedMs = 0L
        lastNetworkType = NetworkType.None
    }

    fun onSample(snapshot: TrafficSnapshot, elapsedMs: Long, networkType: NetworkType): Sample {
        val previous = lastSnapshot
        val intervalMs = elapsedMs - lastElapsedMs
        val networkChanged = networkType != lastNetworkType
        var sample = Sample()

        if (previous != null && intervalMs > 100L) {
            val rxDelta = (snapshot.totalRxBytes - previous.totalRxBytes).coerceAtLeast(0L)
            val txDelta = (snapshot.totalTxBytes - previous.totalTxBytes).coerceAtLeast(0L)

            // Todo el intervalo se atribuye a la red activa ahora. Separar WiFi de móvil
            // restando contadores inflaba los valores al cambiar de red.
            sample = when (networkType) {
                NetworkType.Wifi -> Sample(wifiRxDelta = rxDelta, wifiTxDelta = txDelta)
                NetworkType.Mobile -> Sample(mobileRxDelta = rxDelta, mobileTxDelta = txDelta)
                NetworkType.None -> Sample()
            }

            // La velocidad solo tiene sentido con la red estable y con conexión.
            if (!networkChanged && networkType != NetworkType.None) {
                push(downloadSpeedBuffer, (rxDelta * 1000L) / intervalMs)
                push(uploadSpeedBuffer, (txDelta * 1000L) / intervalMs)
                sample = sample.copy(
                    downloadSpeed = downloadSpeedBuffer.sum() / downloadSpeedBuffer.size,
                    uploadSpeed = uploadSpeedBuffer.sum() / uploadSpeedBuffer.size,
                )
            } else {
                downloadSpeedBuffer.clear()
                uploadSpeedBuffer.clear()
            }
        } else {
            downloadSpeedBuffer.clear()
            uploadSpeedBuffer.clear()
        }

        lastSnapshot = snapshot
        lastElapsedMs = elapsedMs
        lastNetworkType = networkType
        return sample
    }

    private fun push(buffer: ArrayDeque<Long>, value: Long) {
        buffer.addLast(value)
        if (buffer.size > speedBufferSize) buffer.removeFirst()
    }
}
