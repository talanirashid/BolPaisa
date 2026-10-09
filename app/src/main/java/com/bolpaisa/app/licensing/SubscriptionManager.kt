package com.bolpaisa.app.licensing

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.TimeUnit

class SubscriptionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_subscription_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val deviceId: String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"

    companion object {
        private const val KEY_INSTALL_TIME = "install_timestamp"
        private const val KEY_EXPIRY_TIME = "expiry_timestamp"
        private const val KEY_LAST_KNOWN_TIME = "last_known_timestamp"
        private const val KEY_BURNED_KEYS = "burned_keys"
        private const val TRIAL_DAYS = 30L
    }

    init {
        checkClockTampering()
        if (!prefs.contains(KEY_INSTALL_TIME)) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_INSTALL_TIME, now).apply()
            val defaultExpiry = now + TimeUnit.DAYS.toMillis(TRIAL_DAYS)
            prefs.edit().putLong(KEY_EXPIRY_TIME, defaultExpiry).apply()
        }
    }

    /**
     * Checks if the clock was rolled back. If current time < last known time, triggers tampering lock.
     */
    private fun checkClockTampering() {
        val currentTime = System.currentTimeMillis()
        val lastRecordedTime = prefs.getLong(KEY_LAST_KNOWN_TIME, 0L)

        if (currentTime < lastRecordedTime) {
            // Clock tampering detected! Lock subscription immediately.
            prefs.edit().putLong(KEY_EXPIRY_TIME, 0L).apply()
        } else {
            prefs.edit().putLong(KEY_LAST_KNOWN_TIME, currentTime).apply()
        }
    }

    fun isSubscriptionActive(): Boolean {
        checkClockTampering()
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0L)
        return System.currentTimeMillis() < expiryTime
    }

    fun getRemainingDays(): Long {
        checkClockTampering()
        val expiryTime = prefs.getLong(KEY_EXPIRY_TIME, 0L)
        val remainingMillis = expiryTime - System.currentTimeMillis()
        return if (remainingMillis > 0) TimeUnit.MILLISECONDS.toDays(remainingMillis) else 0L
    }

    fun getDeviceId(): String = deviceId

    /**
     * Attempts to activate the app with an 8-character unlock code.
     */
    fun activateLicense(enteredCode: String, planDays: Int = 30): Boolean {
        checkClockTampering()
        val cleanCode = enteredCode.replace("-", "").trim().uppercase()

        val burnedKeys = prefs.getStringSet(KEY_BURNED_KEYS, emptySet()) ?: emptySet()
        if (burnedKeys.contains(cleanCode)) {
            return false // Key already used
        }

        if (LicenseValidator.verifyKey(deviceId, cleanCode, planDays)) {
            val updatedBurnedKeys = burnedKeys.toMutableSet().apply { add(cleanCode) }
            
            val currentExpiry = prefs.getLong(KEY_EXPIRY_TIME, System.currentTimeMillis())
            val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
            val newExpiry = baseTime + TimeUnit.DAYS.toMillis(planDays.toLong())

            prefs.edit()
                .putStringSet(KEY_BURNED_KEYS, updatedBurnedKeys)
                .putLong(KEY_EXPIRY_TIME, newExpiry)
                .apply()

            return true
        }

        return false
    }
}
