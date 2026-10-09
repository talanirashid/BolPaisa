package com.bolpaisa.app.licensing

import java.security.MessageDigest

object LicenseValidator {

    // Keep this salt private (do not commit to public repositories)
    private const val SECRET_SALT = "BolPaisa#Secret_Salt_Key_2026!Karachi"

    /**
     * Generates the expected verification code for a specific device and plan.
     * @param deviceId The user's Settings.Secure.ANDROID_ID
     * @param planDurationDays "30" for monthly, "365" for yearly
     * @return 8-character human-readable activation code (e.g., "A3F8-9B2C")
     */
    fun generateKey(deviceId: String, planDurationDays: Int): String {
        val payload = "$deviceId:$planDurationDays:$SECRET_SALT"
        val hash = sha256(payload)
        
        // Take first 8 hex characters and format nicely
        val part1 = hash.substring(0, 4).uppercase()
        val part2 = hash.substring(4, 8).uppercase()
        return "$part1-$part2"
    }

    /**
     * Checks if the code entered by the merchant is authentic for this phone.
     */
    fun verifyKey(deviceId: String, enteredCode: String, planDurationDays: Int): Boolean {
        val cleanEntered = enteredCode.replace("-", "").trim().uppercase()
        val expected = generateKey(deviceId, planDurationDays).replace("-", "")
        return cleanEntered == expected
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
