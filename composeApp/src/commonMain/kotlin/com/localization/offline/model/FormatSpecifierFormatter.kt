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
        private val appFormattingRegex = Regex("\\[%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?$CONVERSION]")
        val supportedToAppFormatSpecifiers = listOf(FormatSpecifier.Java, FormatSpecifier.AppleEcosystem)
    }
    fun format(value: String): String
    fun toAppFormat(value: String): String
    class Java: FormatSpecifierFormatter {
        private val acceptableToAppFormattingRegex = Regex("%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?$CONVERSION")
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
        private val acceptableToAppFormattingConversion = "[@%dDxXoOfeEgGcCsSaA]"
        private val needToTransform = arrayOf('@', 'D', 'O')
        private val transformInto = arrayOf('s', 'd', 'o')
        //@ -> s | D -> d | O -> o
        private val acceptableToAppFormattingRegex = Regex("%$ARGUMENT_INDEX?($FLAGS)?(\\d+)?(?:.\\d+)?$acceptableToAppFormattingConversion")
        override fun format(value: String) = value.replace(appFormattingRegex) {
            it.value.run {
                if (endsWith("s]", true)) {
                    "${substring(0, it.value.length - 2)}@]"
                } else {
                    this
                }
            }.removeSurrounding("[","]")
        }

        override fun toAppFormat(value: String) = value.replace(acceptableToAppFormattingRegex) {
            var value = it.value
            var index = 0
            for (char in needToTransform) {
                if (value.endsWith(char)) {
                    value = "${value.take(it.value.length - 1)}${transformInto[index]}"
                    break
                }
                ++index
            }
            "[$value]"
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
