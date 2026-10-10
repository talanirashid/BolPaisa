package com.bolpaisa.app.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.bolpaisa.app.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val uiLang = LocaleHelper.getAppUiLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, uiLang))
    }
}
