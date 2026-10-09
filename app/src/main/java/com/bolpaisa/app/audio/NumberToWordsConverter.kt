package com.bolpaisa.app.audio

import android.content.Context

object NumberToWordsConverter {

    private val units = arrayOf(
        "", "ek", "do", "teen", "char", "panch", "chhay", "saat", "aath", "nau",
        "das", "gyarah", "barah", "terah", "chaudah", "pandrah", "solah", "satra", "athra", "unnis"
    )

    private val tens = arrayOf(
        "", "", "bees", "tees", "chalees", "pachas", "satth", "sattar", "assee", "nabbay"
    )

    /**
     * Converts a numeric amount into an ordered list of raw resource IDs for Urdu soundbox announcement.
     */
    fun getUrduResIds(context: Context, amount: Long): List<Int> {
        val resIds = mutableListOf<Int>()
        if (amount <= 0) return resIds

        val numList = convertToWordsList(amount)
        for (word in numList) {
            val resId = context.resources.getIdentifier(word, "raw", context.packageName)
            if (resId != 0) {
                resIds.add(resId)
            }
        }
        return resIds
    }

    private fun convertToWordsList(n: Long): List<String> {
        val list = mutableListOf<String>()
        if (n >= 10000000) {
            val crore = n / 10000000
            list.addAll(convertToWordsList(crore))
            list.add("crore")
            val remainder = n % 10000000
            if (remainder > 0) {
                list.addAll(convertToWordsList(remainder))
            }
        } else if (n >= 100000) {
            val lakh = n / 100000
            list.addAll(convertToWordsList(lakh))
            list.add("lakh")
            val remainder = n % 100000
            if (remainder > 0) {
                list.addAll(convertToWordsList(remainder))
            }
        } else if (n >= 1000) {
            val hazar = n / 1000
            list.addAll(convertToWordsList(hazar))
            list.add("hazaar")
            val remainder = n % 1000
            if (remainder > 0) {
                list.addAll(convertToWordsList(remainder))
            }
        } else if (n >= 100) {
            val sau = n / 100
            list.addAll(convertToWordsList(sau))
            list.add("sau")
            val remainder = n % 100
            if (remainder > 0) {
                list.addAll(convertToWordsList(remainder))
            }
        } else if (n >= 20) {
            val t = (n / 10).toInt()
            list.add(tens[t])
            val remainder = n % 10
            if (remainder > 0) {
                list.add(units[remainder.toInt()])
            }
        } else {
            list.add(units[n.toInt()])
        }
        return list
    }
}
