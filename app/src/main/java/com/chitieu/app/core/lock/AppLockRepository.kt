package com.chitieu.app.core.lock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock")

@Singleton
class AppLockRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds get() = context.lockDataStore

    private val KEY_ENABLED = booleanPreferencesKey("lock_enabled")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")

    val lockEnabled: Flow<Boolean> = ds.data.map {
        it[KEY_ENABLED] == true && !it[KEY_PIN_HASH].isNullOrBlank()
    }

    suspend fun isLockEnabled(): Boolean = lockEnabled.first()

    suspend fun setPin(pin: String) {
        require(pin.length in 4..8 && pin.all { it.isDigit() }) { "PIN phải 4–8 chữ số" }
        ds.edit {
            it[KEY_PIN_HASH] = hash(pin)
            it[KEY_ENABLED] = true
        }
    }

    suspend fun disableLock() {
        ds.edit {
            it[KEY_ENABLED] = false
            it.remove(KEY_PIN_HASH)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = ds.data.map { it[KEY_PIN_HASH] }.first()
        return stored != null && stored == hash(pin)
    }

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
