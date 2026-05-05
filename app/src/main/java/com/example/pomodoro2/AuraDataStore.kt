package com.example.pomodoro2

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aura_settings")

class AuraDataStore(private val context: Context) {

    companion object {
        val AURA_DROPS = intPreferencesKey("aura_drops")
        val USER_COINS = intPreferencesKey("user_coins")
        val LANGUAGE = stringPreferencesKey("language")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_STRICT_MODE = booleanPreferencesKey("is_strict_mode")
        val WHITELISTED_PACKAGES = stringSetPreferencesKey("whitelisted_packages")
        val UNLOCKED_PLANTS = stringSetPreferencesKey("unlocked_plants") 
        val USER_XP = intPreferencesKey("user_xp")
        val ACHIEVEMENTS_COUNT = intPreferencesKey("achievements_count")
        val LAST_ACHIEVEMENT_DATE = stringPreferencesKey("last_achievement_date")
        val ALARM_SOUND_NAME = stringPreferencesKey("alarm_sound_name")
    }

    val auraDropsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AURA_DROPS] ?: 50 
    }

    suspend fun saveAuraDrops(drops: Int) {
        context.dataStore.edit { preferences ->
            preferences[AURA_DROPS] = drops
        }
    }

    val userCoinsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[USER_COINS] ?: 0
    }

    suspend fun saveUserCoins(coins: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_COINS] = coins
        }
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE] ?: "vi"
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = lang
        }
    }

    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    suspend fun saveDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = isDark
        }
    }

    val isStrictModeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_STRICT_MODE] ?: false
    }

    suspend fun saveStrictMode(isStrict: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_STRICT_MODE] = isStrict
        }
    }

    val whitelistedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[WHITELISTED_PACKAGES] ?: setOf(context.packageName)
    }

    suspend fun saveWhitelist(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[WHITELISTED_PACKAGES] = packages
        }
    }

    val unlockedPlantsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[UNLOCKED_PLANTS] ?: setOf("🌸")
    }

    suspend fun saveUnlockedPlants(plants: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[UNLOCKED_PLANTS] = plants
        }
    }

    val userXPFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[USER_XP] ?: 0
    }

    suspend fun saveUserXP(xp: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_XP] = xp
        }
    }

    val achievementsCountFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ACHIEVEMENTS_COUNT] ?: 0
    }

    suspend fun saveAchievementsCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[ACHIEVEMENTS_COUNT] = count
        }
    }

    val lastAchievementDateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_ACHIEVEMENT_DATE] ?: ""
    }

    suspend fun saveLastAchievementDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ACHIEVEMENT_DATE] = date
        }
    }

    val alarmSoundNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALARM_SOUND_NAME] ?: "Default"
    }

    suspend fun saveAlarmSoundName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_SOUND_NAME] = name
        }
    }
}
