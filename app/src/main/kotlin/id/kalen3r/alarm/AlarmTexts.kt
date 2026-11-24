package id.kalen3r.alarm

import android.content.Context
import id.kalen3r.R
import java.util.Locale

/** User-visible notification texts; the Android implementation reads strings.xml. */
interface AlarmTexts {
    fun triSandyaTitle(slot: Int): String
    fun triSandyaBody(slot: Int): String
    fun rainanTitle(firstName: String): String
    fun rainanBody(names: List<String>): String
    fun customTitle(): String
    fun customBody(names: List<String>): String
}

/** String-resource names the planner texts use (a JVM test checks they all exist in strings.xml). */
object AlarmTextKeys {
    const val SLOT_PAGI = "slot_pagi"
    const val SLOT_SIANG = "slot_siang"
    const val SLOT_SORE = "slot_sore"
    const val TRI_TITLE = "alarm_tri_title"
    const val TRI_BODY = "alarm_tri_body"
    const val RAINAN_TITLE = "alarm_rainan_title"
    const val RAINAN_BODY = "alarm_rainan_body"
    const val CUSTOM_TITLE = "alarm_custom_title"
    const val CUSTOM_BODY = "alarm_custom_body"
    const val CUSTOM_BODY_EMPTY = "alarm_custom_body_empty"
    val ALL = listOf(SLOT_PAGI, SLOT_SIANG, SLOT_SORE, TRI_TITLE, TRI_BODY, RAINAN_TITLE, RAINAN_BODY, CUSTOM_TITLE, CUSTOM_BODY, CUSTOM_BODY_EMPTY)
}

class AndroidAlarmTexts(private val context: Context) : AlarmTexts {
    private val slotRes = listOf(R.string.slot_pagi, R.string.slot_siang, R.string.slot_sore)
    private fun slot(i: Int) = context.getString(slotRes[i])
    private fun joined(names: List<String>) = names.joinToString(" · ")
    override fun triSandyaTitle(slot: Int) = context.getString(R.string.alarm_tri_title, slot(slot))
    override fun triSandyaBody(slot: Int) = context.getString(R.string.alarm_tri_body, slot(slot).lowercase(Locale.ROOT))
    override fun rainanTitle(firstName: String) = context.getString(R.string.alarm_rainan_title, firstName)
    override fun rainanBody(names: List<String>) = context.getString(R.string.alarm_rainan_body, joined(names))
    override fun customTitle() = context.getString(R.string.alarm_custom_title)
    override fun customBody(names: List<String>) =
        if (names.isEmpty()) context.getString(R.string.alarm_custom_body_empty) else context.getString(R.string.alarm_custom_body, joined(names))
}
