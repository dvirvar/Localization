package com.localization.offline.service

import com.localization.offline.model.AppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleService {
    private val _current = MutableStateFlow(AppLocale.entries.firstOrNull { it.locale.toLanguageTag() == Locale.getDefault().toLanguageTag() } ?: AppLocale.English)
    val current = _current.asStateFlow()

    fun changeLocale(appLocale: AppLocale) {
        Locale.setDefault(appLocale.locale)
        _current.value = appLocale
    }
}