package id.kalen3r.alarm

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.alarmStore by preferencesDataStore(name = "alarm", corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() })

/** DataStore-backed settings; the string formats live in the pure [AlarmSettingsCodec]. */
class AlarmSettings(context: Context) {
    private val store = context.applicationContext.alarmStore
    private val settingsKey = stringPreferencesKey("settings")
    private val idsKey = stringPreferencesKey("scheduled_ids")

    private val askedKey = booleanPreferencesKey("notification_asked")
    private val safeData = store.data.catch { emit(emptyPreferences()) }

    val flow: Flow<AlarmSettingsData> = safeData.map { AlarmSettingsCodec.decode(it[settingsKey]) }

    suspend fun load(): AlarmSettingsData = AlarmSettingsCodec.decode(safeData.map { it[settingsKey] }.first())

    suspend fun update(transform: (AlarmSettingsData) -> AlarmSettingsData): AlarmSettingsData {
        var result = AlarmSettingsData()
        store.edit { prefs ->
            result = transform(AlarmSettingsCodec.decode(prefs[settingsKey]))
            prefs[settingsKey] = AlarmSettingsCodec.encode(result)
        }
        return result
    }

    suspend fun scheduledIds(): Set<Int> = AlarmSettingsCodec.decodeIds(safeData.map { it[idsKey] }.first())

    suspend fun saveScheduledIds(ids: Set<Int>) {
        store.edit { it[idsKey] = AlarmSettingsCodec.encodeIds(ids) }
    }

    suspend fun notificationAsked(): Boolean = safeData.map { it[askedKey] == true }.first()

    suspend fun markNotificationAsked() {
        store.edit { it[askedKey] = true }
    }
}
