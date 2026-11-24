package id.kalen3r.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.alarmStore by preferencesDataStore(name = "alarm")

/** DataStore-backed settings; the string formats live in the pure [AlarmSettingsCodec]. */
class AlarmSettings(context: Context) {
    private val store = context.applicationContext.alarmStore
    private val settingsKey = stringPreferencesKey("settings")
    private val idsKey = stringPreferencesKey("scheduled_ids")

    val flow: Flow<AlarmSettingsData> = store.data.map { AlarmSettingsCodec.decode(it[settingsKey]) }

    suspend fun load(): AlarmSettingsData = AlarmSettingsCodec.decode(store.data.map { it[settingsKey] }.first())

    suspend fun update(transform: (AlarmSettingsData) -> AlarmSettingsData): AlarmSettingsData {
        var result = AlarmSettingsData()
        store.edit { prefs ->
            result = transform(AlarmSettingsCodec.decode(prefs[settingsKey]))
            prefs[settingsKey] = AlarmSettingsCodec.encode(result)
        }
        return result
    }

    suspend fun scheduledIds(): Set<Int> = AlarmSettingsCodec.decodeIds(store.data.map { it[idsKey] }.first())

    suspend fun saveScheduledIds(ids: Set<Int>) {
        store.edit { it[idsKey] = AlarmSettingsCodec.encodeIds(ids) }
    }
}

