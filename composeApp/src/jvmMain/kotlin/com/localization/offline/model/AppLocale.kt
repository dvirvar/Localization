package com.localization.offline.model

import java.util.Locale

enum class AppLocale(val locale: Locale, val isRtl: Boolean = false, val languageName: String) {
    English(Locale.ENGLISH, languageName = "English"),
    Hebrew(Locale.forLanguageTag("he"), true, "עברית");
}