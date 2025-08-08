package raf.console.pdfreader.helper


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raf.console.pdfreader.helper.ZoomableImage
import java.io.*

// Иконки +/- (если хочешь кастомные — поменяй на painterResource своих ресурсов)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove

enum class PdfListDirection {
    HORIZONTAL, VERTICAL
}

@ExperimentalFoundationApi
@Composable
fun PdfViewer(
    @RawRes pdfResId: Int?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF909090),
    pageColor: Color = Color.White,
    listDirection: PdfListDirection = PdfListDirection.VERTICAL,
    arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(16.dp),
    loadingListener: (
        isLoading: Boolean,
        currentPage: Int?,
        maxPage: Int?,
    ) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    pdfResId?.let { context.resources.openRawResource(it) }?.let {
        PdfViewer(
            pdfStream = it,
            modifier = modifier,
            pageColor = pageColor,
            backgroundColor = backgroundColor,
            listDirection = listDirection,
            arrangement = arrangement,
            loadingListener = loadingListener,
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun PdfViewer(
    pdfStream: InputStream,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF909090),
    pageColor: Color = Color.White,
    listDirection: PdfListDirection = PdfListDirection.VERTICAL,
    arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(16.dp),
    loadingListener: (
        isLoading: Boolean,
        currentPage: Int?,
        maxPage: Int?,
    ) -> Unit = { _, _, _ -> }
) {
    PdfViewer(
        pdfStream = pdfStream,
        modifier = modifier,
        backgroundColor = backgroundColor,
        listDirection = listDirection,
        loadingListener = loadingListener,
        arrangement = arrangement
    ) { lazyState, imagem ->
        PaginaPDF(
            imagem = imagem,
            lazyState = lazyState,
            backgroundColor = pageColor
        )
    }
}

@ExperimentalFoundationApi
@Composable
fun PdfViewer(
    pdfStream: InputStream,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF909090),
    listDirection: PdfListDirection = PdfListDirection.VERTICAL,
    arrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(16.dp),
    loadingListener: (
        isLoading: Boolean,
        currentPage: Int?,
        maxPage: Int?,
    ) -> Unit = { _, _, _ -> },
    page: @Composable (LazyListState, ImageBitmap) -> Unit
) {
    val context = LocalContext.current
    val pagePaths = remember { mutableStateListOf<String>() }

    LaunchedEffect(true) {
        if (pagePaths.isEmpty()) {
            val paths = context.loadPdf(pdfStream, loadingListener)
            pagePaths.addAll(paths)
        }
    }

    val lazyState = rememberLazyListState()

    when (listDirection) {
        PdfListDirection.HORIZONTAL ->
            LazyRow(
                modifier = modifier.background(backgroundColor),
                state = lazyState,
                horizontalArrangement = arrangement
            ) {
                items(pagePaths) { path ->
                    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(path) {
                        imageBitmap = BitmapFactory.decodeFile(path).asImageBitmap()
                    }
                    imageBitmap?.let { page(lazyState, it) }
                }
            }

        PdfListDirection.VERTICAL ->
            LazyColumn(
                modifier = modifier.background(backgroundColor),
                state = lazyState,
                verticalArrangement = arrangement
            ) {
                items(pagePaths) { path ->
                    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(path) {
                        imageBitmap = BitmapFactory.decodeFile(path).asImageBitmap()
                    }
                    imageBitmap?.let { page(lazyState, it) }
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaginaPDF(
    imagem: ImageBitmap,
    lazyState: LazyListState,
    backgroundColor: Color = Color.White
) {
    // Состояние внешнего масштаба (доп. к pinch-to-zoom)
    var toolbarScale by remember { mutableStateOf(1f) }
    val minScale = 0.5f
    val maxScale = 4f
    val step = 0.25f

    fun formatScale(s: Float): String {
        val intPart = s.toInt()
        return if (kotlin.math.abs(s - intPart) < 0.001f) "${intPart}x"
        else String.format("%.1fx", s)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RectangleShape
    ) {
        Box {
            // Контент страницы: ZoomableImage (pinch) + внешний масштаб из тулбара (graphicsLayer)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = toolbarScale
                        scaleY = toolbarScale
                    }
            ) {
                ZoomableImage(
                    painter = BitmapPainter(imagem),
                    backgroundColor = backgroundColor,
                    shape = RectangleShape,
                    minScale = 1f,
                    maxScale = 3f,
                    contentScale = ContentScale.Fit,
                    isRotation = false,
                    isZoomable = true,
                    scrollState = lazyState // чтобы pinch не конфликтовал со скроллом списка
                )
            }

            // Панель управления зумом в правом верхнем углу страницы
            Surface(
                tonalElevation = 3.dp,
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = { toolbarScale = (toolbarScale - step).coerceIn(minScale, maxScale) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Уменьшить масштаб"
                        )
                    }

                    Text(
                        text = formatScale(toolbarScale),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = { toolbarScale = (toolbarScale + step).coerceIn(minScale, maxScale) }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Увеличить масштаб"
                        )
                    }
                }
            }
        }
    }
}

suspend fun Context.loadPdf(
    inputStream: InputStream,
    loadingListener: (
        isLoading: Boolean,
        currentPage: Int?,
        maxPage: Int?
    ) -> Unit = { _, _, _ -> }
): List<String> = withContext(Dispatchers.Default) {
    loadingListener(true, null, null)
    val outputDir = cacheDir
    val tempFile = File.createTempFile("temp", "pdf", outputDir)
    tempFile.mkdirs()
    tempFile.deleteOnExit()

    val outputStream = FileOutputStream(tempFile)
    copy(inputStream, outputStream)

    val input = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(input)

    (0 until renderer.pageCount).map { pageNumber ->
        loadingListener(true, pageNumber, renderer.pageCount)

        val file = File.createTempFile("PDFpage$pageNumber", "png", outputDir)
        file.mkdirs()
        file.deleteOnExit()

        val page = renderer.openPage(pageNumber)
        val bitmap = Bitmap.createBitmap(1240, 1754, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
        Log.i("PDF_VIEWER", "Loaded page $pageNumber")
        file.absolutePath.also { Log.d("TESTE", it) }
    }.also {
        loadingListener(false, null, renderer.pageCount)
        renderer.close()
    }
}

@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } > 0) {
        target.write(buf, 0, length)
    }
}