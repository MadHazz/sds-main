package com.rihlahidali.advdisplay.util

import java.util.Locale

object IdentifierCode {
    fun normalize(value: String): String {
        val code = value.trim().uppercase(Locale.ROOT)
        require(code.matches(Regex("[A-Z0-9_-]{1,64}"))) {
            "Enter a code using letters, numbers, hyphens or underscores (up to 64 characters)."
        }
        return code
    }
}
