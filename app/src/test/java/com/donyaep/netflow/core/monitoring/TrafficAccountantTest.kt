package com.donyaep.netflow.core.monitoring

import com.donyaep.netflow.data.model.TrafficSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class TrafficAccountantTest {

    private fun snapshot(rx: Long, tx: Long) =
        TrafficSnapshot(totalRxBytes = rx, totalTxBytes = tx, mobileRxBytes = 0L, mobileTxBytes = 0L)

    private fun startedOn(networkType: NetworkType) = TrafficAccountant().apply {
        start(snapshot(1_000, 500), elapsedMs = 0, networkType = networkType)
    }

    private fun assertNothingCounted(sample: TrafficAccountant.Sample) {
        assertEquals(TrafficAccountant.Sample(), sample)
    }

    @Test
    fun `atribuye a WiFi`() {
        val sample = startedOn(NetworkType.Wifi).onSample(snapshot(3_000, 900), 2_000, NetworkType.Wifi)

        assertEquals(2_000, sample.wifiRxDelta)
        assertEquals(400, sample.wifiTxDelta)
        assertEquals(0, sample.mobileRxDelta)
        assertEquals(0, sample.mobileTxDelta)
        assertEquals(1_000, sample.downloadSpeed)
        assertEquals(200, sample.uploadSpeed)
    }

    @Test
    fun `atribuye a móvil`() {
        val sample = startedOn(NetworkType.Mobile).onSample(snapshot(3_000, 900), 2_000, NetworkType.Mobile)

        assertEquals(2_000, sample.mobileRxDelta)
        assertEquals(400, sample.mobileTxDelta)
        assertEquals(0, sample.wifiRxDelta)
        assertEquals(0, sample.wifiTxDelta)
    }

    @Test
    fun `sin conexión no atribuye`() {
        assertNothingCounted(startedOn(NetworkType.None).onSample(snapshot(3_000, 900), 2_000, NetworkType.None))
    }

    @Test
    fun `un contador que retrocede no cuenta`() {
        assertNothingCounted(startedOn(NetworkType.Wifi).onSample(snapshot(200, 100), 2_000, NetworkType.Wifi))
    }

    @Test
    fun `un intervalo de 100 ms o menos se ignora`() {
        assertNothingCounted(startedOn(NetworkType.Wifi).onSample(snapshot(3_000, 900), 100, NetworkType.Wifi))
    }

    @Test
    fun `tras ignorar una muestra la base se mueve`() {
        val accountant = startedOn(NetworkType.Wifi)
        accountant.onSample(snapshot(3_000, 900), 100, NetworkType.Wifi)

        val sample = accountant.onSample(snapshot(5_000, 900), 2_100, NetworkType.Wifi)

        assertEquals(2_000, sample.wifiRxDelta)
    }

    @Test
    fun `al cambiar de red cuenta los bytes pero no da velocidad`() {
        val sample = startedOn(NetworkType.Wifi).onSample(snapshot(3_000, 900), 2_000, NetworkType.Mobile)

        assertEquals(2_000, sample.mobileRxDelta)
        assertEquals(0, sample.downloadSpeed)
    }

    @Test
    fun `la velocidad es la media de las tres últimas`() {
        val accountant = startedOn(NetworkType.Wifi)

        assertEquals(1_000, accountant.onSample(snapshot(3_000, 500), 2_000, NetworkType.Wifi).downloadSpeed)
        assertEquals(1_500, accountant.onSample(snapshot(7_000, 500), 4_000, NetworkType.Wifi).downloadSpeed)
        assertEquals(2_000, accountant.onSample(snapshot(13_000, 500), 6_000, NetworkType.Wifi).downloadSpeed)
    }

    @Test
    fun `el búfer descarta la muestra más antigua`() {
        val accountant = startedOn(NetworkType.Wifi)
        accountant.onSample(snapshot(3_000, 500), 2_000, NetworkType.Wifi)
        accountant.onSample(snapshot(7_000, 500), 4_000, NetworkType.Wifi)
        accountant.onSample(snapshot(13_000, 500), 6_000, NetworkType.Wifi)

        val sample = accountant.onSample(snapshot(21_000, 500), 8_000, NetworkType.Wifi)

        assertEquals(3_000, sample.downloadSpeed)
    }

    @Test
    fun `rebase no cuenta lo anterior y reinicia la media`() {
        val accountant = startedOn(NetworkType.Wifi)
        accountant.onSample(snapshot(3_000, 500), 2_000, NetworkType.Wifi)
        accountant.onSample(snapshot(7_000, 500), 4_000, NetworkType.Wifi)
        accountant.onSample(snapshot(13_000, 500), 6_000, NetworkType.Wifi)

        accountant.rebase(snapshot(20_000, 500), 7_000)
        val sample = accountant.onSample(snapshot(22_000, 500), 9_000, NetworkType.Wifi)

        assertEquals(2_000, sample.wifiRxDelta)
        assertEquals(1_000, sample.downloadSpeed)
    }

    @Test
    fun `sin start la primera muestra no cuenta`() {
        assertNothingCounted(TrafficAccountant().onSample(snapshot(3_000, 900), 2_000, NetworkType.Wifi))
    }
}
