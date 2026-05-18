package com.localization.offline.model

import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.Serializable

@Serializable
data class ExportToTranslator(
    val languages: List<Language>,
    val keyValues: List<KeyValues>
) {
    @Serializable
    data class Language(
        val id: Int,
        val name: String,
        val readOnly: Boolean
    )
    @Serializable
    data class KeyValues(
        val id: String,
        val name: String,
        val description: String,
        val values: List<Value>
    ) {
        @Serializable
        data class Value(
            val languageId: Int,
            val value: String
        )
    }

    fun keyValuesAsObservable() = keyValues.fastMap { it.copy(values = it.values.toMutableStateList()) }.toMutableStateList()
}
