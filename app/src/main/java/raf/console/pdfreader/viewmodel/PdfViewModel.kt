package raf.console.pdfreader.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import raf.console.pdfreader.HorizontalPdfReaderState
import raf.console.pdfreader.NavigablePdfState
import raf.console.pdfreader.PdfReaderState
import raf.console.pdfreader.ResourceType
import raf.console.pdfreader.VerticalPdfReaderState
import raf.console.pdfreader.util.AppPreferences

class PdfViewModel(
    private val preferences: AppPreferences,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _stateFlow = MutableStateFlow<PdfReaderState?>(null)
    val stateFlow: StateFlow<PdfReaderState?> = _stateFlow.asStateFlow()

    var switchState by mutableStateOf(preferences.getReadingMode())

    init {
        viewModelScope.launch {
            preferences.getLastPdfUri()?.let { uriString ->
                val uri = Uri.parse(uriString)
                val resource = if (uri.scheme == "file") {
                    ResourceType.Local(uri)
                } else {
                    ResourceType.Remote(uri.toString())
                }
                openResource(resource, preferences.getCurrentPage())
            }
        }
    }

    fun openResource(resourceType: ResourceType, page: Int = 0) {
        preferences.savePdfUri(
            when(resourceType) {
                is ResourceType.Local -> resourceType.uri.toString()
                is ResourceType.Remote -> resourceType.url
                else -> {}
            }.toString()
        )

        val newState = if (switchState) {
            HorizontalPdfReaderState(resourceType, true)
        } else {
            VerticalPdfReaderState(resourceType, true)
        }

        _stateFlow.value = newState

        viewModelScope.launch {
            // Ждем инициализации PDF
            var attempts = 0
            while (attempts < 10 && newState.pdfPageCount <= 0) {
                delay(100)
                attempts++
            }

            if (newState.pdfPageCount > 0) {
                (newState as? NavigablePdfState)?.jumpTo(
                    page.coerceIn(0, newState.pdfPageCount - 1),
                    viewModelScope
                )
            }
        }
    }

    fun jumpToPage(page: Int) {
        val currentState = _stateFlow.value
        if (currentState?.pdfPageCount ?: 0 > 0) {
            val targetPage = page.coerceIn(0, currentState!!.pdfPageCount - 1)
            viewModelScope.launch {
                (currentState as? NavigablePdfState)?.jumpTo(targetPage, viewModelScope)
                saveCurrentPage(targetPage)
            }
        }
    }

    fun saveCurrentPage(page: Int) {
        if (page >= 0) {
            preferences.saveCurrentPage(page)
        }
    }

    fun clearResource() {
        _stateFlow.value?.close()
        _stateFlow.value = null
        preferences.clearSavedData()
    }

    fun toggleReadingMode() {
        switchState = !switchState
        preferences.saveReadingMode(switchState)
        _stateFlow.value?.let { currentState ->
            openResource(currentState.resource, currentState.currentPage)
        }
    }
}