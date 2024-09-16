package com.localization.offline.model

import androidx.compose.ui.util.fastForEach
import com.localization.offline.db.CustomFormatSpecifierEntity

sealed interface FormatSpecifierFormatter {
    companion object {
        private const val ARGUMENT_INDEX = "(\\d+\\$)"
        private const val FLAGS = "[-,#,+, ,0,\\,,\\(]"
        private const val CONVERSION = "[bBhHsScCdoxXeEfgGaA%n]"
        //Java formatter style with addition of [] and exception of date/time
        //For more information: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html#summary
        private val acceptableToAppFormattingRegex = Regex("%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?$CONVERSION")
        private val appFormattingRegex = Regex("\\[%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?$CONVERSION]")
        val supportedToAppFormatSpecifiers = listOf(FormatSpecifier.Java, FormatSpecifier.AppleEcosystem)
    }
    fun format(value: String): String
    fun toAppFormat(value: String): String
    class Java: FormatSpecifierFormatter {
        override fun format(value: String) = value.replace(appFormattingRegex) {
            it.value.removeSurrounding("[","]")
        }

        override fun toAppFormat(value: String) = value.replace(acceptableToAppFormattingRegex) {
            "[${it.value}]"
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class AppleEcosystem: FormatSpecifierFormatter {
        private val needToFormatRegex = Regex("%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?li|@")
        private val needToFormatAppRegex = Regex("\\[%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?[si]]")
        override fun format(value: String) = value.replace(needToFormatAppRegex) {
            val suffix = if (it.value.endsWith("i]")) "li" else "@"
            "${it.value.substring(1, it.value.length - 2)}$suffix"
        }

        override fun toAppFormat(value: String) = value.replace(needToFormatRegex) {
            val suffix: String
            val suffixLength: Int
            if (it.value.endsWith("li")) {
                suffix = "i"
                suffixLength = 2
            } else {
                suffix = "s"
                suffixLength = 1
            }
            "${it.value.substring(0, it.value.length - suffixLength)}$suffix"
        }

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class I18n: FormatSpecifierFormatter {
        private val argumentIndexRegex = Regex(ARGUMENT_INDEX)
        override fun format(value: String): String {
            var counter = 0
            return value.replace(appFormattingRegex) {
                val argumentIndexValue = it.value
                if (argumentIndexValue.contains(argumentIndexRegex)) {
                    "{{${argumentIndexValue.substring(2, argumentIndexValue.indexOf("$"))}}}"
                } else {
                    "{{${counter++}}}"
                }
            }
        }

        override fun toAppFormat(value: String): String = error("I18n doesn't support toAppFormat")

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }

    class Custom(private val customFormatSpecifiers: List<CustomFormatSpecifierEntity>): FormatSpecifierFormatter {
        override fun format(value: String): String {
            var value = value
            customFormatSpecifiers.fastForEach {
                value = value.replace(Regex(it.from), it.to)
            }
            return value
        }

        override fun toAppFormat(value: String): String = error("Custom doesn't support toAppFormat")

        override fun equals(other: Any?): Boolean {
            return this === other
        }
        override fun hashCode(): Int {
            return System.identityHashCode(this)
        }
    }
}

object FormatSpecifierFormatterFactory {
    fun getBy(argument: Argument): FormatSpecifierFormatter? = when(argument) {
        is Argument.Empty -> {
            when(argument.formatSpecifier) {
                FormatSpecifier.Java -> FormatSpecifierFormatter.Java()
                FormatSpecifier.AppleEcosystem -> FormatSpecifierFormatter.AppleEcosystem()
                FormatSpecifier.I18n -> FormatSpecifierFormatter.I18n()
                FormatSpecifier.None -> null
                FormatSpecifier.Custom -> error("For custom use Argument.Custom")
            }
        }
        is Argument.Custom -> {
            FormatSpecifierFormatter.Custom(argument.customFormatSpecifiers)
        }
    }

    sealed interface Argument {
        data class Empty(val formatSpecifier: FormatSpecifier): Argument
        data class Custom(val customFormatSpecifiers: List<CustomFormatSpecifierEntity>): Argument
    }
}
