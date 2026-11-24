package id.kalen3r.core

import java.time.LocalDate

/**
 * Groups of holy days. GALUNGAN_KUNINGAN holds the whole Sugihan..Pemacekan Agung series,
 * SARASWATI_PAGERWESI the Saraswati..Pagerwesi series (Banyu Pinaruh, Soma Ribek, Sabuh Mas).
 * OTHER is only used for names added by the correction table.
 */
enum class RerainanKind {
    PURNAMA, TILEM, KAJENG_KLIWON, ANGGARA_KASIH, BUDA_CEMENG, TUMPEK, GALUNGAN_KUNINGAN,
    SARASWATI_PAGERWESI, NYEPI, SIWARATRI, NGEMBAK_GENI, OTHER,
}

data class Rerainan(val date: LocalDate, val name: String, val kind: RerainanKind)

/** All holy days of [date]: Pawukon rules (exact) plus lunar rules read from the corrected lunar day. */
fun rerainanOn(date: LocalDate): List<Rerainan> = RerainanRules.on(date) { Corrections.apply(rawLunarDay(it)) }

/**
 * Rule tables. Pawukon rules match (wuku, saptawara, pancawara) where null means "any". Wuku numbers are 1-based
 * (Sinta 1, Landep 2, Wariga 7, Dungulan 11, Kuningan 12, Krulut 17, Uye 22, Wayang 27, Watugunung 30).
 * Golden-verified rows are marked [G]; the rest are rule-only (see the task-8 report).
 */
internal object RerainanRules {
    private class PawukonRule(
        val wuku: Int?, val sapta: Saptawara?, val panca: Pancawara?, val name: String, val kind: RerainanKind,
    )

    private val tumpek = RerainanKind.TUMPEK
    private val gk = RerainanKind.GALUNGAN_KUNINGAN
    private val sp = RerainanKind.SARASWATI_PAGERWESI

    private val PAWUKON_RULES = listOf(
        PawukonRule(null, Saptawara.ANGGARA, Pancawara.KLIWON, "Anggara Kasih", RerainanKind.ANGGARA_KASIH), // [G]
        PawukonRule(null, Saptawara.BUDA, Pancawara.WAGE, "Buda Cemeng", RerainanKind.BUDA_CEMENG), // rule-only (Rabu Wage, every 35 days)
        PawukonRule(2, Saptawara.SANISCARA, Pancawara.KLIWON, "Tumpek Landep", tumpek), // [G]
        PawukonRule(7, Saptawara.SANISCARA, Pancawara.KLIWON, "Tumpek Wariga (Uduh/Pengatag)", tumpek), // [G]
        PawukonRule(17, Saptawara.SANISCARA, Pancawara.KLIWON, "Tumpek Krulut", tumpek), // [G]
        PawukonRule(22, Saptawara.SANISCARA, Pancawara.KLIWON, "Tumpek Kandang (Uye)", tumpek), // [G]
        PawukonRule(27, Saptawara.SANISCARA, Pancawara.KLIWON, "Tumpek Wayang", tumpek), // [G]
        PawukonRule(10, Saptawara.WRASPATI, Pancawara.WAGE, "Sugihan Jawa", gk), // [G]
        PawukonRule(10, Saptawara.SUKRA, Pancawara.KLIWON, "Sugihan Bali", gk), // [G]
        PawukonRule(11, Saptawara.REDITE, Pancawara.PAING, "Penyekeban Galungan", gk), // [G]
        PawukonRule(11, Saptawara.SOMA, Pancawara.PON, "Penyajaan Galungan", gk), // [G]
        PawukonRule(11, Saptawara.ANGGARA, Pancawara.WAGE, "Penampahan Galungan", gk), // [G]
        PawukonRule(11, Saptawara.BUDA, Pancawara.KLIWON, "Hari Raya Galungan", gk), // [G]
        PawukonRule(11, Saptawara.WRASPATI, Pancawara.UMANIS, "Manis Galungan", gk), // [G]
        PawukonRule(11, Saptawara.SANISCARA, Pancawara.PON, "Pemaridan Guru", gk), // [G]
        PawukonRule(12, Saptawara.SOMA, Pancawara.KLIWON, "Pemacekan Agung", gk), // [G]
        PawukonRule(12, Saptawara.SANISCARA, Pancawara.KLIWON, "Hari Raya Kuningan", gk), // [G]
        PawukonRule(30, Saptawara.SANISCARA, Pancawara.UMANIS, "Hari Raya Saraswati", sp), // rule-only (hari-raya.csv checks dates)
        PawukonRule(1, Saptawara.REDITE, Pancawara.PAING, "Banyu Pinaruh", sp), // [G]
        PawukonRule(1, Saptawara.SOMA, Pancawara.PON, "Soma Ribek", sp), // [G]
        PawukonRule(1, Saptawara.ANGGARA, Pancawara.WAGE, "Sabuh Mas", sp), // rule-only; golden's site lists nothing on this day
        PawukonRule(1, Saptawara.BUDA, Pancawara.KLIWON, "Hari Raya Pagerwesi", sp), // [G]
    )

    fun on(date: LocalDate, lunar: (LocalDate) -> RawLunarDay): List<Rerainan> {
        val p = Pawukon.of(date)
        val out = ArrayList(lunarRules(date, lunar))
        if (p.triwara == Triwara.KAJENG && p.pancawara == Pancawara.KLIWON) out += Rerainan(date, "Kajeng Kliwon", RerainanKind.KAJENG_KLIWON) // [G]
        for (r in PAWUKON_RULES) {
            if ((r.wuku == null || r.wuku == p.wuku) && (r.sapta == null || r.sapta == p.saptawara) && (r.panca == null || r.panca == p.pancawara)) {
                out += Rerainan(date, r.name, r.kind)
            }
        }
        return applyCorrections(lunar(date), out)
    }

    /**
     * Lunar rules from corrected lunar days ([lunar] must apply the correction table). Purnama/Tilem come from
     * the day's flags; Nyepi is the day after Tilem Kesanga (KANAWA); Ngembak Geni the day after Nyepi; Siwaratri is
     * the day before Tilem of Sasih Kapitu (pangelong 14, or 13 when tithi 14 is skipped: golden 2024-01-09).
     * Correction rows (applied to the final list in [on]) may add rerainan names (kind OTHER) or remove names.
     */
    fun fromLunar(date: LocalDate, lunar: (LocalDate) -> RawLunarDay): List<Rerainan> =
        applyCorrections(lunar(date), lunarRules(date, lunar))

    private fun lunarRules(date: LocalDate, lunar: (LocalDate) -> RawLunarDay): List<Rerainan> {
        val day = lunar(date)
        fun isNyepi(d: LocalDate) = lunar(d.minusDays(1)).let { it.isTilem && it.sasih == Sasih.KANAWA && !it.isNampihSasih }
        val out = ArrayList<Rerainan>()
        if (day.isPurnama) out += Rerainan(date, "Purnama", RerainanKind.PURNAMA) // [G]
        if (day.isTilem) out += Rerainan(date, "Tilem", RerainanKind.TILEM) // [G]
        if (isNyepi(date)) out += Rerainan(date, "Hari Raya Nyepi", RerainanKind.NYEPI) // [G]
        if (isNyepi(date.minusDays(1))) out += Rerainan(date, "Ngembak Geni", RerainanKind.NGEMBAK_GENI) // [G]
        lunar(date.plusDays(1)).let {
            if (it.isTilem && it.sasih == Sasih.KAPITU && !it.isNampihSasih) out += Rerainan(date, "Hari Siwaratri", RerainanKind.SIWARATRI) // [G]
        }
        return out
    }

    /** Applies rerainanAdd / rerainanRemove of the corrected day to a list built for that same day. */
    fun applyCorrections(day: RawLunarDay, list: List<Rerainan>): List<Rerainan> =
        list.filter { it.name !in day.rerainanRemove } + day.rerainanAdd.map { Rerainan(day.date, it, RerainanKind.OTHER) }
}
