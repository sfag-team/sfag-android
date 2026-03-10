package com.sfag.grammar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MultiInputViewModel
    @Inject
    constructor() : ViewModel() {
        var inputs by mutableStateOf(List(5) { "" })
            private set

        fun addRow() {
            inputs = inputs + ""
        }

        fun updateRowText(
            index: Int,
            newText: String,
        ) {
            inputs = inputs.toMutableList().apply { this[index] = newText }
        }

        fun removeRowAt(index: Int) {
            inputs = inputs.toMutableList().apply { removeAt(index) }
        }
    }
