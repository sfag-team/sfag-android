package com.example.gramatika

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Inputs : ViewModel() {
    private val _inputs = MutableLiveData<List<String>>(emptyList())
    val inputs: LiveData<List<String>> get() = _inputs

    init {
        // Add an initial row
        _inputs.value = List(5){""}
    }

    fun addRow() {
        _inputs.value = _inputs.value?.plus("")
    }

    fun updateRowText(index: Int, newText: String) {
        _inputs.value = _inputs.value?.toMutableList()?.apply {
            this[index] = newText
        }
    }
    fun removeRowAt(index: Int) {
        _inputs.value = _inputs.value?.toMutableList()?.apply { removeAt(index) }
    }
}
