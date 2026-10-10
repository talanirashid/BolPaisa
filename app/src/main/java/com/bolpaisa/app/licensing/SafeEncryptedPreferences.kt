package com.bolpaisa.app.licensing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

object SafeEncryptedPreferences {

    private const val TAG = "BolPaisaAuth"

    fun get(context: Context, fileName: String): SharedPreferences {
        return try {
            createPrefs(context, fileName)
        } catch (e: Exception) {
            // Handles AEADBadTagException, KeyStoreException, and IOException
            Log.e(TAG, "Keystore desynchronization or corruption detected for $fileName. Resetting corrupted preferences.", e)
            try {
                // Delete corrupted XML preference file and retry
                val prefFile = File(context.filesDir.parent, "shared_prefs/$fileName.xml")
                if (prefFile.exists()) {
                    prefFile.delete()
                }
                createPrefs(context, fileName)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback to standard SharedPreferences for $fileName", fallbackError)
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createPrefs(context: Context, fileName: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
