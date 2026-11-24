package id.kalen3r.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class InvalidWewaranException(message: String) : IllegalArgumentException(message)

/** Next [count] otonan (210-day) dates of [birth] on or after [from] (and never before [birth]). Pure arithmetic. */
fun otonan(birth: LocalDate, count: Int, from: LocalDate): List<LocalDate> {
    require(count > 0) { "count harus lebih dari 0" }
    val days = ChronoUnit.DAYS.between(birth, from)
    val steps = if (days <= 0) 0L else Math.floorDiv(days + Pawukon.CYCLE - 1, Pawukon.CYCLE.toLong())
    val first = birth.plusDays(steps * Pawukon.CYCLE)
    return List(count) { first.plusDays(it.toLong() * Pawukon.CYCLE) }
}

private fun pawukonIndex(wuku: Int, saptawara: Saptawara, pancawara: Pancawara): Int {
    if (wuku !in 1..30) throw InvalidWewaranException("Wuku harus 1..30, bukan $wuku")
    val index = (wuku - 1) * 7 + saptawara.ordinal
    val actual = Pancawara.entries[index % 5]
    if (actual != pancawara) {
        throw InvalidWewaranException(
            "Kombinasi tidak valid: Wuku ${Pawukon.WUKU_NAMES[wuku - 1]} $saptawara jatuh pada pancawara $actual, bukan $pancawara",
        )
    }
    return index
}

/** First date on or after [from] with Pawukon index [index]. */
private fun firstOnOrAfter(index: Int, from: LocalDate): LocalDate {
    val current = Pawukon.of(from).index
    return from.plusDays(Math.floorMod(index - current, Pawukon.CYCLE).toLong())
}

/** Next [count] dates on or after [from] with the given wuku + saptawara + pancawara. */
fun findByWewaran(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, count: Int): List<LocalDate> {
    require(count > 0) { "count harus lebih dari 0" }
    val first = firstOnOrAfter(pawukonIndex(wuku, saptawara, pancawara), from)
    return List(count) { first.plusDays(it.toLong() * Pawukon.CYCLE) }
}

/** All dates in [from]..[to] (inclusive) with the given wuku + saptawara + pancawara. */
fun odalanByPawukon(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, to: LocalDate): List<LocalDate> {
    val index = pawukonIndex(wuku, saptawara, pancawara)
    val result = ArrayList<LocalDate>()
    var d = firstOnOrAfter(index, from)
    while (!d.isAfter(to)) {
        result.add(d)
        d = d.plusDays(Pawukon.CYCLE.toLong())
    }
    return result
}
