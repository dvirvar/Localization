package com.localization.offline.model

import androidx.compose.ui.util.fastForEach
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.xml.sax.InputSource
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

sealed interface FileStructureBuilder {
    fun build(keyValues: List<Pair<String, String>>): String
    fun deconstruct(fileBody: String): List<Pair<String, String>>
    class KmpXml: FileStructureBuilder {
        override fun build(keyValues: List<Pair<String, String>>) = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().run {
            val resources = createElement("resources").apply {
                keyValues.fastForEach { keyValue ->
                    appendChild(createElement("string").apply {
                        setAttribute("name", keyValue.first)
                        textContent = keyValue.second
                    })
                }
            }
            appendChild(resources)
            StringWriter().use {
                TransformerFactory.newInstance().newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                }.transform(DOMSource(this), StreamResult(it))
                it.toString()
            }
        }

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(fileBody)))
            doc.getElementsByTagName("string").apply {
                for (i in 0..<length) {
                    list.add(item(i).run {
                        Pair(attributes.getNamedItem("name").nodeValue, textContent)
                    })
                }
            }
            return list
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }
    class AndroidXml: FileStructureBuilder {
        override fun build(keyValues: List<Pair<String, String>>) = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().run {
            val resources = createElement("resources").apply {
                keyValues.fastForEach { keyValue ->
                    appendChild(createElement("string").apply {
                        setAttribute("name", keyValue.first)
                        textContent = keyValue.second.replace("(\")|(\')".toRegex()) {
                            "\\${it.value}"
                        }
                    })
                }
            }
            appendChild(resources)
            StringWriter().use {
                TransformerFactory.newInstance().newTransformer().apply {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                }.transform(DOMSource(this), StreamResult(it))
                it.toString()
            }
        }

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            val list = mutableListOf<Pair<String, String>>()
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(InputSource(StringReader(fileBody)))
            doc.getElementsByTagName("string").apply {
                for (i in 0..<length) {
                    list.add(item(i).run {
                        Pair(attributes.getNamedItem("name").nodeValue, textContent.replace("(\\\\\")|(\\\\\')".toRegex()) {
                            it.value.removePrefix("\\")
                        })
                    })
                }
            }
            return list
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
                val value = it.second.replace("(\")|(\\\\(?![nru]))".toRegex()) {
                    "\\${it.value}"
                }
                append("\"${it.first}\" = \"${value}\";\n")
            }
            deleteAt(length - 1)
        }.toString()

        override fun deconstruct(fileBody: String): List<Pair<String, String>> {
            return Regex("\"\\w+\"\\s?=\\s?\"[\\S\\s]*?\";").findAll(fileBody).map {
                val keyValue = it.value
                val keyEndIndex = keyValue.indexOf("\"", 1)
                val key = keyValue.substring(1, keyEndIndex)
                val value = keyValue.substring(keyValue.indexOf("\"", keyEndIndex + 1) + 1, keyValue.length - 2).replace("(\\\\\")|(\\\\)".toRegex()) {
                    it.value.removePrefix("\\")
                }
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
        FileStructure.KmpXml -> FileStructureBuilder.KmpXml()
        FileStructure.AndroidXml -> FileStructureBuilder.AndroidXml()
        FileStructure.AppleStrings -> FileStructureBuilder.AppleStrings()
        FileStructure.Json -> FileStructureBuilder.Json()
    }
}