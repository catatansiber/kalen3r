package id.kalen3r.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * One day of the 210-day Pawukon cycle. Int fields are 0-based indexes into the
 * name lists on [Pawukon] ([Pawukon.CATURWARA_NAMES] etc.); [urip] is the
 * saptawara urip + pancawara urip sum.
 */
data class PawukonDay(
    val index: Int,
    val wuku: Int,
    val saptawara: Saptawara,
    val pancawara: Pancawara,
    val triwara: Triwara,
    val caturwara: Int,
    val sadwara: Int,
    val astawara: Int,
    val sangawara: Int,
    val dasawara: Int,
    val urip: Int,
)

object Pawukon {
    /** 2026-11-01 = index 0 = Wuku Sinta, Redite, Paing. */
    val ANCHOR: LocalDate = LocalDate.of(2026, 11, 1)
    const val CYCLE = 210

    val WUKU_NAMES = listOf(
        "Sinta", "Landep", "Ukir", "Kulantir", "Tolu", "Gumbreg", "Wariga", "Warigadean", "Julungwangi", "Sungsang",
        "Dungulan", "Kuningan", "Langkir", "Medangsia", "Pujut", "Pahang", "Krulut", "Merakih", "Tambir", "Medangkungan",
        "Matal", "Uye", "Menail", "Prangbakat", "Bala", "Ugu", "Wayang", "Kulawu", "Dukut", "Watugunung",
    )

    // Name orders; the Int fields of PawukonDay index into these lists.
    val SADWARA_NAMES = listOf("Tungleh", "Aryang", "Urukung", "Paniron", "Was", "Maulu")
    val CATURWARA_NAMES = listOf("Sri", "Laba", "Jaya", "Menala")
    val ASTAWARA_NAMES = listOf("Sri", "Indra", "Guru", "Yama", "Ludra", "Brahma", "Kala", "Uma")
    val SANGAWARA_NAMES = listOf("Dangu", "Jangur", "Gigis", "Nohan", "Ogan", "Erangan", "Urungan", "Tulus", "Dadi")

    /** Indexed by (saptawara urip + pancawara urip) mod 10. */
    val DASAWARA_NAMES = listOf("Pandita", "Pati", "Suka", "Duka", "Sri", "Manuh", "Manusa", "Raja", "Dewa", "Raksasa")

    private val SAPTA_URIP = intArrayOf(5, 4, 3, 7, 8, 6, 9) // Redite..Saniscara
    private val PANCA_URIP = intArrayOf(9, 7, 4, 8, 5) // Paing, Pon, Wage, Kliwon, Umanis

    fun saptawaraUrip(s: Saptawara): Int = SAPTA_URIP[s.ordinal]
    fun pancawaraUrip(p: Pancawara): Int = PANCA_URIP[p.ordinal]

    /*
     * Verification status. Golden covers cycle indexes 0..196 contiguously (Nov 2026 = 0..29,
     * Jan-Jun 2024 = 15..196), so every quirk below is checked against golden except the tail
     * indexes 197..209 (rest of Wuku Dukut, all of Watugunung), which follow the same formulas
     * (they wrap consistently into index 0) but are verified by published rule only.
     * Sadwara is not a golden column; it is index%6 by definition.
     */
    fun of(date: LocalDate): PawukonDay {
        val index = Math.floorMod(ChronoUnit.DAYS.between(ANCHOR, date), CYCLE.toLong()).toInt()
        val sapta = Saptawara.entries[index % 7]
        val panca = Pancawara.entries[index % 5]
        // Caturwara: plain mod 4 through index 70 (Jaya); indexes 71,72 repeat Jaya; from 73 shift by 2.
        val catur = if (index <= 70) index % 4 else (maxOf(index, 72) - 2) % 4
        // Astawara: plain mod 8 through index 70 (Kala); indexes 71,72 repeat Kala; from 73 shift by 2.
        val asta = if (index <= 70) index % 8 else (maxOf(index, 72) - 2) % 8
        // Sangawara: Dangu repeats for indexes 0..3 (shift by 3), then mod 9.
        val sanga = if (index < 4) 0 else (index - 3) % 9
        val urip = SAPTA_URIP[sapta.ordinal] + PANCA_URIP[panca.ordinal]
        return PawukonDay(
            index = index,
            wuku = index / 7 + 1,
            saptawara = sapta,
            pancawara = panca,
            triwara = Triwara.entries[index % 3],
            caturwara = catur,
            sadwara = index % 6,
            astawara = asta,
            sangawara = sanga,
            dasawara = urip % 10,
            urip = urip,
        )
    }
}
