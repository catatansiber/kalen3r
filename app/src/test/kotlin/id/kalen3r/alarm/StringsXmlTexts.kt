package id.kalen3r.alarm

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** JVM [AlarmTexts] backed by the real strings.xml, so tests exercise the shipped Indonesian text. */
object StringsXml {
    val file = File("src/main/res/values/strings.xml")
    val values: Map<String, String> by lazy {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        (0 until nodes.length).associate { nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent }
    }
}

object StringsXmlTexts : AlarmTexts {
    private fun s(key: String, vararg args: Any) = String.format(Locale.ROOT, StringsXml.values.getValue(key), *args)
    private val slots = listOf(AlarmTextKeys.SLOT_PAGI, AlarmTextKeys.SLOT_SIANG, AlarmTextKeys.SLOT_SORE)
    private fun slot(i: Int) = s(slots[i])
    override fun triSandyaTitle(slot: Int) = s(AlarmTextKeys.TRI_TITLE, slot(slot))
    override fun triSandyaBody(slot: Int) = s(AlarmTextKeys.TRI_BODY, slot(slot).lowercase(Locale.ROOT))
    override fun rainanTitle(firstName: String) = s(AlarmTextKeys.RAINAN_TITLE, firstName)
    override fun rainanBody(names: List<String>) = s(AlarmTextKeys.RAINAN_BODY, names.joinToString(" · "))
    override fun customTitle() = s(AlarmTextKeys.CUSTOM_TITLE)
    override fun customBody(names: List<String>) =
        if (names.isEmpty()) s(AlarmTextKeys.CUSTOM_BODY_EMPTY) else s(AlarmTextKeys.CUSTOM_BODY, names.joinToString(" · "))
}
