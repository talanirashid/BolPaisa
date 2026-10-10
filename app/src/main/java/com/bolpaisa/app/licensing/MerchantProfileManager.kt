package com.bolpaisa.app.licensing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class MerchantProfileManager(private val context: Context) {

    private val prefs = SafeEncryptedPreferences.get(context, "secure_merchant_profile_prefs")

    companion object {
        const val KEY_SHOP_NAME = "key_shop_name"
        const val KEY_GATEWAY_TYPE = "key_gateway_type" // "EASYPAISA", "JAZZCASH", "RAAST"
        const val KEY_ACCOUNT_OR_TILL = "key_account_or_till"
        const val KEY_DEFAULT_GATEWAY = "key_default_gateway"
        const val KEY_RAAST_ID = "key_raast_id"
        const val KEY_EASYPAISA_ID = "key_easypaisa_id"
        const val KEY_JAZZCASH_ID = "key_jazzcash_id"
        const val KEY_HAS_PROFILE = "key_has_profile"
        private const val LOGO_FILE_NAME = "shop_logo.png"
    }

    fun hasProfile(): Boolean {
        return prefs.getBoolean(KEY_HAS_PROFILE, false)
    }

    fun getShopName(): String {
        return prefs.getString(KEY_SHOP_NAME, "BolPaisa Merchant") ?: "BolPaisa Merchant"
    }

    fun getGatewayType(): String {
        return prefs.getString(KEY_DEFAULT_GATEWAY, prefs.getString(KEY_GATEWAY_TYPE, "EASYPAISA")) ?: "EASYPAISA"
    }

    fun getAccountOrTill(): String {
        return prefs.getString(KEY_ACCOUNT_OR_TILL, "03336366291") ?: "03336366291"
    }

    fun getGatewayId(gatewayType: String): String {
        val key = when (gatewayType.uppercase()) {
            "RAAST" -> KEY_RAAST_ID
            "JAZZCASH" -> KEY_JAZZCASH_ID
            else -> KEY_EASYPAISA_ID
        }
        val savedId = prefs.getString(key, "")
        if (!savedId.isNullOrEmpty()) return savedId

        if (getGatewayType().equals(gatewayType, true)) {
            return getAccountOrTill()
        }
        return ""
    }

    fun saveGatewayId(gatewayType: String, id: String) {
        val key = when (gatewayType.uppercase()) {
            "RAAST" -> KEY_RAAST_ID
            "JAZZCASH" -> KEY_JAZZCASH_ID
            else -> KEY_EASYPAISA_ID
        }
        prefs.edit().putString(key, id).apply()
    }

    fun saveProfile(shopName: String, gatewayType: String, accountOrTill: String) {
        prefs.edit()
            .putString(KEY_SHOP_NAME, shopName)
            .putString(KEY_GATEWAY_TYPE, gatewayType)
            .putString(KEY_DEFAULT_GATEWAY, gatewayType)
            .putString(KEY_ACCOUNT_OR_TILL, accountOrTill)
            .putBoolean(KEY_HAS_PROFILE, true)
            .apply()

        saveGatewayId(gatewayType, accountOrTill)
    }

    fun saveShopLogo(bitmap: Bitmap): Boolean {
        return try {
            val file = File(context.filesDir, LOGO_FILE_NAME)
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            out.flush()
            out.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getShopLogo(): Bitmap? {
        return try {
            val file = File(context.filesDir, LOGO_FILE_NAME)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
