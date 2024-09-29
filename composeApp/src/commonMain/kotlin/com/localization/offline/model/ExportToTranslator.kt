package com.localization.offline.model

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
}
