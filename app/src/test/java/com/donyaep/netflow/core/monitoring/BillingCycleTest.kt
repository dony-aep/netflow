package com.donyaep.netflow.core.monitoring

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillingCycleTest {

    @Test
    fun `el día del ciclo ya pasó este mes`() {
        assertEquals(LocalDate.of(2026, 10, 1), billingCycleStart(1, LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `hoy es el día del ciclo`() {
        assertEquals(LocalDate.of(2026, 10, 5), billingCycleStart(5, LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `el día del ciclo aún no llega`() {
        assertEquals(LocalDate.of(2026, 9, 20), billingCycleStart(20, LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun `el ciclo empezó el año anterior`() {
        assertEquals(LocalDate.of(2025, 12, 15), billingCycleStart(15, LocalDate.of(2026, 1, 10)))
    }

    @Test
    fun `el mes anterior no tiene ese día`() {
        assertEquals(LocalDate.of(2026, 2, 28), billingCycleStart(31, LocalDate.of(2026, 3, 15)))
    }

    @Test
    fun `el mes actual no tiene ese día y hoy es su último día`() {
        assertEquals(LocalDate.of(2026, 2, 28), billingCycleStart(31, LocalDate.of(2026, 2, 28)))
    }
}
