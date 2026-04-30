package com.sfag.grammar.domain.grammar

import androidx.annotation.StringRes
import com.sfag.R

enum class GrammarType(@param:StringRes val displayNameRes: Int) {
    REGULAR(R.string.grammar_type_regular),
    CONTEXT_FREE(R.string.grammar_type_context_free),
    CONTEXT_SENSITIVE(R.string.grammar_type_context_sensitive),
    UNRESTRICTED(R.string.grammar_type_unrestricted),
    INVALID(R.string.grammar_type_invalid),
}
