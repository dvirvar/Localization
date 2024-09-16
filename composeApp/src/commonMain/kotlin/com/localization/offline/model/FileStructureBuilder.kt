package com.localization.offline.model

import androidx.compose.ui.util.fastForEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

sealed interface FileStructureBuilder {
    fun build(keyValues: List<Pair<String, String>>): String
    fun deconstruct(fileBody: String): List<Pair<String, String>>
    class AndroidXml: FileStructureBuilder {
        override fun build(keyValues: List<Pair<String, String>>) = StringBuilder().apply {
            append("<resources>\n")
            keyValues.fastForEach {
                append("\t<string name=\"${it.first}\">${it.second}</string>\n")
            }
            append("</resources>")
        }.toString()

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            return Regex("<string name=\"\\w+\">[\\S\\s]*?</string>").findAll(fileBody).map {
                val keyValue = it.value
                val keyStartIndex = keyValue.indexOf("name=\"") + 6
                val keyEndIndex = keyValue.indexOf("\"", keyStartIndex)
                val key = keyValue.substring(keyStartIndex, keyEndIndex)
                val value = keyValue.substring(keyEndIndex + 2, keyValue.lastIndexOf('<'))
                Pair(key, value)
            }.toList()
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class AppleStrings: FileStructureBuilder {
        override fun build(keyValues: List<Pair<String, String>>) = StringBuilder().apply {
            keyValues.fastForEach {
                append("\"${it.first}\" = \"${it.second}\";\n")
            }
            deleteAt(length - 1)
        }.toString()

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            return Regex("\"\\w+\"\\s?=\\s?\"[\\S\\s]*?\";").findAll(fileBody).map {
                val keyValue = it.value
                val keyEndIndex = keyValue.indexOf("\"", 1)
                val key = keyValue.substring(1, keyEndIndex)
                val value = keyValue.substring(keyValue.indexOf("\"", keyEndIndex + 1) + 1, keyValue.length - 2)
                Pair(key, value)
            }.toList()
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class Json: FileStructureBuilder {
        override fun build(keyValues: List<Pair<String, String>>) = Json { prettyPrint = true }
            .encodeToString(JsonObject.serializer(), buildJsonObject {
            keyValues.fastForEach {
                put(it.first, it.second)
            }
        })

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            return kotlinx.serialization.json.Json.parseToJsonElement(fileBody).jsonObject.map { it.key to it.value.toString() }
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }
}

object FileStructureBuilderFactory {
    fun getBy(fileStructure: FileStructure): FileStructureBuilder = when(fileStructure) {
        FileStructure.AndroidXml -> FileStructureBuilder.AndroidXml()
        FileStructure.AppleStrings -> FileStructureBuilder.AppleStrings()
        FileStructure.Json -> FileStructureBuilder.Json()
    }
}