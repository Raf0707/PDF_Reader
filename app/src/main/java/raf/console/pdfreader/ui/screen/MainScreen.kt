package raf.console.pdfreader.ui.screen


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import raf.console.pdfreader.HorizontalPDFReader
import raf.console.pdfreader.HorizontalPdfReaderState
import raf.console.pdfreader.ResourceType
import raf.console.pdfreader.VerticalPDFReader
import raf.console.pdfreader.VerticalPdfReaderState
import raf.console.pdfreader.ui.theme.AppTheme
import raf.console.pdfreader.viewmodel.PdfViewModel
import java.io.File


class MainScreen : ComponentActivity() {

    private val viewModel: PdfViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.clearResource()
            }
        })
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state = viewModel.stateFlow.collectAsState()
                    Scaffold(

                    ) { padding ->
                        Box(modifier = Modifier.padding(padding)) {
                            when (val actualState = state.value) {
                                null -> SelectionView()
                                is VerticalPdfReaderState -> PDFView(
                                    pdfState = actualState,
                                    onBack = { viewModel.clearResource() },
                                    onOpenDocument = { openDocumentPicker() }
                                )
                                is HorizontalPdfReaderState -> HPDFView(
                                    pdfState = actualState,
                                    onBack = { viewModel.clearResource() },
                                    onOpenDocument = { openDocumentPicker() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /*@Composable
    fun TopAppBar() {
        TopAppBar(
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.h6
            )
        }
    }*/

    //@OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun SelectionElement(
        title: String,
        text: String,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 4.dp
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = text,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    @Composable
    fun SelectionView() {
        Column(modifier = Modifier.fillMaxSize()) {
            /*SelectionElement(
                title = "Open Base64",
                text = "Try to open a base64 pdf"
            ) {
                viewModel.openResource(
                    ResourceType.Base64(
                        this@MainActivity.getString(R.string.base64_pdf)
                    )
                )
            }
            SelectionElement(
                title = "Open Remote file",
                text = "Open a remote file from url"
            ) {
                viewModel.openResource(
                    ResourceType.Remote(
                        url = this@MainActivity.getString(
                            R.string.pdf_url
                        ),
                        headers = hashMapOf("headerKey" to "headerValue")
                    )
                )
            }*/
            SelectionElement(
                title = "Open Local file",
                text = "Open a file in device memory"
            ) {
                openDocumentPicker()
            }
            /*SelectionElement(
                title = "Open asset file",
                text = "Open asset file in raw folder"
            ) {
                viewModel.openResource(
                    ResourceType.Asset(R.raw.lorem_ipsum)
                )
            }*/
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "List view")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = viewModel.switchState,
                    onCheckedChange = {
                        viewModel.switchState = it
                    }
                    /*colors = SwitchDefaults.colors(
                        uncheckedThumbColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackColor = MaterialTheme.colors.secondaryVariant,
                        uncheckedTrackAlpha = 0.54f
                    )*/
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Pager view")
            }
        }
    }

    @Composable
    fun PDFView(
        pdfState: VerticalPdfReaderState,
        onBack: () -> Unit,
        onOpenDocument: () -> Unit
    ) {
        var showDialog by remember { mutableStateOf(false) }
        var pageInput by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        // Обработка ошибок
        LaunchedEffect(pdfState.error) {
            pdfState.error?.let { error ->
                snackbarHostState.showSnackbar(
                    message = "Файл поврежден, выберите другой файл",
                    actionLabel = "Выбрать",
                    duration = SnackbarDuration.Long
                )
                onBack()
            }
        }

        Box(contentAlignment = Alignment.TopStart) {
            // Показываем индикатор загрузки, если файл ещё не загружен
            if (pdfState.pdfPageCount == 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            VerticalPDFReader(
                state = pdfState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            )
                        ) {
                            Text(
                                text = if (pdfState.pdfPageCount > 0)
                                    "Страница: ${pdfState.currentPage}/${pdfState.pdfPageCount}"
                                else "Загрузка...",
                                modifier = Modifier.clickable {
                                    if (pdfState.pdfPageCount > 0) showDialog = true
                                }
                            )
                            if (pdfState.pdfPageCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        pdfState.file?.let { file ->
                                            sharePdfFile(context, file)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Поделиться",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Перейти на страницу") },
                    text = {
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { newValue ->
                                pageInput = newValue.filter { it.isDigit() }
                            },
                            label = { Text("Номер страницы (1-${pdfState.pdfPageCount})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val page = pageInput.toIntOrNull() ?: return@Button
                                when {
                                    page < 1 -> {
                                        // Показываем ошибку, если страница меньше 1
                                    }
                                    page > pdfState.pdfPageCount -> {
                                        // Показываем ошибку, если страница больше максимума
                                    }
                                    else -> {
                                        pdfState.jumpTo(page - 1, coroutineScope)
                                        showDialog = false
                                    }
                                }
                            }
                        ) {
                            Text("Перейти")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }

        // Snackbar для отображения ошибок
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { data ->
                Snackbar(
                    action = {
                        TextButton(
                            onClick = {
                                data.performAction()
                                onOpenDocument()
                            }
                        ) {
                            Text("Выбрать")
                        }
                    }
                ) {
                    Text("data.message")
                }
            }
        }
    }

    @Composable
    fun HPDFView(
        pdfState: HorizontalPdfReaderState,
        onBack: () -> Unit,
        onOpenDocument: () -> Unit
    ) {
        var showDialog by remember { mutableStateOf(false) }
        var pageInput by remember { mutableStateOf("") }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }

        // Обработка ошибок
        LaunchedEffect(pdfState.error) {
            pdfState.error?.let { error ->
                snackbarHostState.showSnackbar(
                    message = "Файл поврежден, выберите другой файл",
                    actionLabel = "Выбрать",
                    duration = SnackbarDuration.Long
                )
                onBack()
            }
        }

        Box(contentAlignment = Alignment.TopStart) {
            // Показываем индикатор загрузки, если файл ещё не загружен
            if (pdfState.pdfPageCount == 0) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        color = Color.Red,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            HorizontalPDFReader(
                state = pdfState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 4.dp
                            )
                        ) {
                            Text(
                                text = if (pdfState.pdfPageCount > 0)
                                    "Страница: ${pdfState.currentPage}/${pdfState.pdfPageCount}"
                                else "Загрузка...",
                                modifier = Modifier.clickable {
                                    if (pdfState.pdfPageCount > 0) showDialog = true
                                }
                            )
                            if (pdfState.pdfPageCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        pdfState.file?.let { file ->
                                            sharePdfFile(context, file)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Поделиться",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Перейти на страницу") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = pageInput,
                                onValueChange = { newValue ->
                                    pageInput = newValue.filter { it.isDigit() }
                                },
                                label = { Text("Номер страницы (1-${pdfState.pdfPageCount})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            if (pageInput.isNotEmpty()) {
                                val page = pageInput.toIntOrNull()
                                when {
                                    page == null -> Text("Введите число", color = MaterialTheme.colorScheme.error)
                                    page < 1 -> Text("Страница не может быть меньше 1", color = MaterialTheme.colorScheme.error)
                                    page > pdfState.pdfPageCount -> Text("Максимальная страница: ${pdfState.pdfPageCount}",
                                        color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            enabled = pageInput.toIntOrNull()?.let { it in 1..pdfState.pdfPageCount } ?: false,
                            onClick = {
                                pdfState.jumpTo(pageInput.toInt() - 1, coroutineScope)
                                showDialog = false
                            }
                        ) {
                            Text("Перейти")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
        }

        // Snackbar для отображения ошибок
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { data ->
                Snackbar(
                    action = {
                        TextButton(
                            onClick = {
                                data.performAction()
                                onOpenDocument()
                            }
                        ) {
                            Text("Выбрать")
                        }
                    }
                ) {
                    Text("data.message")
                }
            }
        }
    }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, OPEN_DOCUMENT_REQUEST_CODE)
    }

    private fun openDocument(documentUri: Uri) {
        documentUri.path?.let {
            viewModel.openResource(
                ResourceType.Local(
                    documentUri
                )
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == OPEN_DOCUMENT_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { documentUri ->
                contentResolver.takePersistableUriPermission(
                    documentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                openDocument(documentUri)
            }
        }
    }

    private fun shareFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${this.packageName}.fileprovider",
            file
        )
        val intent = ShareCompat.IntentBuilder.from(this)
            .setType("application/pdf")
            .setStream(uri)
            .createChooserIntent()
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    private fun sharePdfFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Поделиться файлом"
        )
        context.startActivity(shareIntent)
    }
}

private const val OPEN_DOCUMENT_REQUEST_CODE = 0x33
