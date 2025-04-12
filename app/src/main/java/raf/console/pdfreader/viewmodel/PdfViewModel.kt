package raf.console.pdfreader.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import raf.console.pdfreader.HorizontalPdfReaderState
import raf.console.pdfreader.PdfReaderState
import raf.console.pdfreader.ResourceType
import raf.console.pdfreader.VerticalPdfReaderState

class PdfViewModel : ViewModel() {
    private val mStateFlow = MutableStateFlow<PdfReaderState?>(null)
    val stateFlow: StateFlow<PdfReaderState?> = mStateFlow

    val switchState = mutableStateOf(false)

    fun openResource(resourceType: ResourceType) {
        mStateFlow.tryEmit(
            if (switchState.value) {
                HorizontalPdfReaderState(resourceType, true)
            } else {
                VerticalPdfReaderState(resourceType, true)
            }
        )
    }

    fun clearResource() {
        mStateFlow.tryEmit(null)
    }
}
