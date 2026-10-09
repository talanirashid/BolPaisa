package com.bolpaisa.app.licensing

object KeyGenerationEngine {

    /**
     * Generates a 100% offline HMAC-SHA256 activation key for a target device ID and duration.
     * Guaranteed to match LicenseValidator.kt and SubscriptionManager.kt.
     */
    fun generateActivationKey(deviceId: String, durationDays: Int): String {
        return LicenseValidator.generateKey(deviceId, durationDays)
    }
}
