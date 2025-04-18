package raf.console.pdfreader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.net.toUri
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
class HorizontalPdfReaderState(
    resource: ResourceType,
    isZoomEnable: Boolean = false,
    isAccessibleEnable: Boolean = false,
) : PdfReaderState(resource, isZoomEnable, isAccessibleEnable), NavigablePdfState {

    internal var pagerState: PagerState = PagerState()

    override val currentPage: Int
        get() = pagerState.currentPage

    override val isScrolling: Boolean
        get() = pagerState.isScrollInProgress

    override fun jumpTo(page: Int, coroutineScope: CoroutineScope) {
        if (page < 0 || page > pdfPageCount) return

        val targetPage = page // Преобразуем в 0-based индекс
        coroutineScope.launch {
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    companion object {
        val Saver: Saver<HorizontalPdfReaderState, *> = listSaver(
            save = {
                val resource = it.file?.let { file ->
                    ResourceType.Local(
                        file.toUri()
                    )
                } ?: it.resource
                listOf(
                    resource,
                    it.isZoomEnable,
                    it.isAccessibleEnable,
                    it.pagerState.currentPage
                )
            },
            restore = {
                HorizontalPdfReaderState(
                    it[0] as ResourceType,
                    it[1] as Boolean,
                    it[2] as Boolean
                ).apply {
                    pagerState = PagerState(currentPage = it[3] as Int)
                }
            }
        )
    }
}

@Composable
fun rememberHorizontalPdfReaderState(
    resource: ResourceType,
    isZoomEnable: Boolean = true,
    isAccessibleEnable: Boolean = false,
): HorizontalPdfReaderState {
    return rememberSaveable(saver = HorizontalPdfReaderState.Saver) {
        HorizontalPdfReaderState(resource, isZoomEnable, isAccessibleEnable)
    }
}
