package id.kalen3r.ui.detail

import id.kalen3r.core.BaliDay
import id.kalen3r.core.Pawukon
import id.kalen3r.core.TithiType

enum class TithiSpecial { PURNAMA, TILEM }

/** Tithi to display: "Penanggal 15" / "Pangelong 7" plus an optional Purnama/Tilem suffix. */
data class TithiDisplay(val type: TithiType, val number: Int, val special: TithiSpecial?)

/**
 * Pure formatting of the full wewaran (Eka..Dasa), Urip and tithi for the Detail screen (JVM-testable).
 * Ekawara/Dwiwara are derived from the urip (saptawara urip + pancawara urip): odd gives Luang/Pepet,
 * even gives no Ekawara ("-") and Menga.
 */
object DetailFormat {
    private fun uripIsOdd(day: BaliDay) = day.urip % 2 == 1

    fun ekawara(day: BaliDay) = if (uripIsOdd(day)) "Luang" else "-"
    fun dwiwara(day: BaliDay) = if (uripIsOdd(day)) "Pepet" else "Menga"
    fun caturwara(day: BaliDay) = Pawukon.CATURWARA_NAMES[day.caturwara]
    fun sadwara(day: BaliDay) = Pawukon.SADWARA_NAMES[day.sadwara]
    fun astawara(day: BaliDay) = Pawukon.ASTAWARA_NAMES[day.astawara]
    fun sangawara(day: BaliDay) = Pawukon.SANGAWARA_NAMES[day.sangawara]
    fun dasawara(day: BaliDay) = Pawukon.DASAWARA_NAMES[day.dasawara]

    /** "5+9 = 14": saptawara urip + pancawara urip = urip. */
    fun urip(day: BaliDay) =
        "${Pawukon.saptawaraUrip(day.saptawara)}+${Pawukon.pancawaraUrip(day.pancawara)} = ${day.urip}"

    fun tithi(day: BaliDay): TithiDisplay = TithiDisplay(
        type = day.tithi.type,
        number = day.tithi.number,
        special = when {
            day.isPurnama -> TithiSpecial.PURNAMA
            day.isTilem -> TithiSpecial.TILEM
            else -> null
        },
    )
}
