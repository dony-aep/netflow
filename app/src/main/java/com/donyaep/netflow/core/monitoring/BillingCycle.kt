package com.donyaep.netflow.core.monitoring

import java.time.LocalDate

/**
 * Primer día del ciclo de facturación que contiene a [today]. Si el mes no tiene
 * el día [billingCycleDay] (31 en febrero), el ciclo empieza en su último día.
 */
fun billingCycleStart(billingCycleDay: Int, today: LocalDate = LocalDate.now()): LocalDate {
    val safeDay = billingCycleDay.coerceIn(1, today.lengthOfMonth())
    return if (today.dayOfMonth >= safeDay) {
        today.withDayOfMonth(safeDay)
    } else {
        val previousMonth = today.minusMonths(1)
        previousMonth.withDayOfMonth(billingCycleDay.coerceAtMost(previousMonth.lengthOfMonth()))
    }
}
