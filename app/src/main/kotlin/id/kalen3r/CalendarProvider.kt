package id.kalen3r

import id.kalen3r.core.BaliCalendar

/** Single shared facade (it caches computed months); delegates to the core's default instance. */
object CalendarProvider {
    val calendar: BaliCalendar get() = BaliCalendar.default
}
