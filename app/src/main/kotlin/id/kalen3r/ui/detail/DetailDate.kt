package id.kalen3r.ui.detail

import java.time.LocalDate

private val RANGE = LocalDate.of(2000, 1, 1)..LocalDate.of(2100, 12, 31)

/** Parses a `detail/{date}` argument; null when malformed or outside 2000-01-01..2100-12-31. */
fun parseDetailDate(arg: String?): LocalDate? = try {
    arg?.let { LocalDate.parse(it) }?.takeIf { it in RANGE }
} catch (e: java.time.format.DateTimeParseException) {
    null
}
