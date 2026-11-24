package id.kalen3r.alarm

import id.kalen3r.core.RerainanKind
import java.time.LocalDate
import java.time.LocalTime

/** Rainan switches shown on the Alarm screen. [HARI_RAYA_BESAR] bundles all the big holy-day kinds. */
enum class RainanGroup(val kinds: Set<RerainanKind>, val label: String) {
    PURNAMA(setOf(RerainanKind.PURNAMA), "Purnama"),
    TILEM(setOf(RerainanKind.TILEM), "Tilem"),
    KAJENG_KLIWON(setOf(RerainanKind.KAJENG_KLIWON), "Kajeng Kliwon"),
    HARI_RAYA_BESAR(
        setOf(
            RerainanKind.TUMPEK, RerainanKind.GALUNGAN_KUNINGAN, RerainanKind.SARASWATI_PAGERWESI,
            RerainanKind.NYEPI, RerainanKind.SIWARATRI, RerainanKind.NGEMBAK_GENI,
        ),
        "Hari raya besar",
    ),
    ANGGARA_KASIH(setOf(RerainanKind.ANGGARA_KASIH), "Anggara Kasih"),
    BUDA_CEMENG(setOf(RerainanKind.BUDA_CEMENG), "Buda Cemeng"),
}

enum class SoundChoice { TRI_SANDYA, DEFAULT }

data class SlotSetting(val enabled: Boolean, val time: LocalTime)

data class AlarmSettingsData(
    /** Exactly 3 slots: Pagi, Siang, Sore. */
    val triSandya: List<SlotSetting> = DEFAULT_SLOTS,
    val rainanGroups: Set<RainanGroup> = DEFAULT_GROUPS,
    val rainanTime: LocalTime = LocalTime.of(6, 0),
    val sound: SoundChoice = SoundChoice.DEFAULT,
    val customReminders: Set<LocalDate> = emptySet(),
) {
    init { require(triSandya.size == 3) { "triSandya needs 3 slots" } }

    companion object {
        val DEFAULT_SLOTS = listOf(
            SlotSetting(true, LocalTime.of(6, 0)),
            SlotSetting(true, LocalTime.of(12, 0)),
            SlotSetting(true, LocalTime.of(18, 0)),
        )
        val DEFAULT_GROUPS = setOf(RainanGroup.PURNAMA, RainanGroup.TILEM, RainanGroup.HARI_RAYA_BESAR)
    }
}

object AlarmSounds {
    /** Options to offer: the Tri Sandya sound is hidden until the raw file exists. */
    fun available(hasTriSandyaFile: Boolean): List<SoundChoice> =
        if (hasTriSandyaFile) listOf(SoundChoice.TRI_SANDYA, SoundChoice.DEFAULT) else listOf(SoundChoice.DEFAULT)

    /** A saved Tri Sandya choice falls back to the default tone while the file is missing. */
    fun effective(saved: SoundChoice, hasTriSandyaFile: Boolean): SoundChoice =
        if (saved == SoundChoice.TRI_SANDYA && !hasTriSandyaFile) SoundChoice.DEFAULT else saved
}

enum class ReminderOutcome { ADDED, PAST }

object ReminderLogic {
    /** A reminder fires at [rainanTime] on [date]; if that moment is not in the future it is not added. */
    fun outcome(now: java.time.LocalDateTime, date: LocalDate, rainanTime: LocalTime): ReminderOutcome =
        if (date.atTime(rainanTime).isAfter(now)) ReminderOutcome.ADDED else ReminderOutcome.PAST
}

/** Pure (de)serialisation for DataStore: one string per value, tolerant of garbage (falls back to defaults). */
object AlarmSettingsCodec {
    fun encode(s: AlarmSettingsData): String = listOf(
        "ts=" + s.triSandya.joinToString(",") { (if (it.enabled) "1" else "0") + "@" + it.time },
        "rg=" + s.rainanGroups.joinToString(",") { it.name },
        "rt=" + s.rainanTime,
        "sn=" + s.sound.name,
        "cr=" + s.customReminders.sorted().joinToString(","),
    ).joinToString(";")

    fun decode(text: String?): AlarmSettingsData {
        val d = AlarmSettingsData()
        if (text.isNullOrBlank()) return d
        val map = text.split(';').mapNotNull { p -> p.indexOf('=').takeIf { it > 0 }?.let { p.substring(0, it) to p.substring(it + 1) } }.toMap()
        val slots = runCatching {
            map["ts"]?.split(',')?.map { slot ->
                val (e, t) = slot.split('@').also { require(it.size == 2) }
                SlotSetting(e == "1", LocalTime.parse(t))
            }?.takeIf { it.size == 3 }
        }.getOrNull()
        return AlarmSettingsData(
            triSandya = slots ?: d.triSandya,
            rainanGroups = map["rg"]?.let { v -> v.split(',').filter { it.isNotEmpty() }.mapNotNull { n -> RainanGroup.entries.firstOrNull { it.name == n } }.toSet() } ?: d.rainanGroups,
            rainanTime = map["rt"]?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: d.rainanTime,
            sound = map["sn"]?.let { n -> SoundChoice.entries.firstOrNull { it.name == n } } ?: d.sound,
            customReminders = map["cr"]?.split(',')?.filter { it.isNotEmpty() }?.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }?.toSet() ?: d.customReminders,
        )
    }

    fun encodeIds(ids: Set<Int>): String = ids.sorted().joinToString(",")
    fun decodeIds(text: String?): Set<Int> = text?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
}
