package com.localization.offline.service

import androidx.compose.ui.text.intl.PlatformLocale
import com.localization.offline.model.AppLocale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object LocaleService {
    private val _current = MutableStateFlow(AppLocale.entries.firstOrNull { it.locale.toLanguageTag() == PlatformLocale.getDefault().toLanguageTag() } ?: AppLocale.English)
    val current = _current.asStateFlow()

    fun changeLocale(appLocale: AppLocale) {
        PlatformLocale.setDefault(appLocale.locale)
        _current.value = appLocale
    }
}