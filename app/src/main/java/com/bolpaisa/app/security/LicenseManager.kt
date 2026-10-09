package com.bolpaisa.app.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.TimeUnit

class LicenseManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_license_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_INSTALL_TIME = "install_timestamp"
        private const val TRIAL_DAYS = 30L
    }

    init {
        if (!sharedPreferences.contains(KEY_INSTALL_TIME)) {
            sharedPreferences.edit().putLong(KEY_INSTALL_TIME, System.currentTimeMillis()).apply()
        }
    }

    fun isTrialValid(): Boolean {
        val installTime = sharedPreferences.getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()
        val elapsedMillis = currentTime - installTime
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis)
        return elapsedDays <= TRIAL_DAYS
    }

    fun getRemainingDays(): Long {
        val installTime = sharedPreferences.getLong(KEY_INSTALL_TIME, System.currentTimeMillis())
        val currentTime = System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(currentTime - installTime)
        return (TRIAL_DAYS - elapsedDays).coerceAtLeast(0)
    }

    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
    }
}
