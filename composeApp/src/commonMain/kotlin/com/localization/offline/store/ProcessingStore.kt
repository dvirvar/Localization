package com.localization.offline.store

import kotlinx.coroutines.flow.MutableStateFlow

object ProcessingStore {
    val exportAndOverwriteTranslations = MutableStateFlow(false)
    val exportTranslationsAsZip = MutableStateFlow(false)
    val importTranslations = MutableStateFlow(false)
}