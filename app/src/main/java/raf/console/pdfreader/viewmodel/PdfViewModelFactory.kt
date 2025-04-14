package raf.console.pdfreader.viewmodel

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import raf.console.pdfreader.util.AppPreferences

class PdfViewModelFactory(
    private val preferences: AppPreferences,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        if (modelClass.isAssignableFrom(PdfViewModel::class.java)) {
            return PdfViewModel(preferences, handle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}