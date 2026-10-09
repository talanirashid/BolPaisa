package com.bolpaisa.app.util

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.bolpaisa.app.R
import com.google.android.material.snackbar.Snackbar

object FeedbackHelper {

    fun showSuccess(view: View, message: String) {
        showCustomSnackbar(view, message, R.drawable.ic_check)
    }

    fun showError(view: View, message: String) {
        showCustomSnackbar(view, message, android.R.drawable.ic_dialog_alert)
    }

    fun showInfo(view: View, message: String) {
        showCustomSnackbar(view, message, android.R.drawable.ic_dialog_info)
    }

    private fun showCustomSnackbar(view: View, message: String, iconRes: Int) {
        try {
            val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            val sbView = snackbar.view

            sbView.setBackgroundResource(R.drawable.bg_snackbar_dark)
            sbView.setPadding(24, 16, 24, 16)

            val textView = sbView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            if (textView != null) {
                textView.setTextColor(Color.WHITE)
                textView.textSize = 14f
                textView.typeface = Typeface.DEFAULT_BOLD
                try {
                    textView.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
                    textView.compoundDrawablePadding = 16
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            snackbar.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
