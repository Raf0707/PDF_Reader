package raf.console.pdfreader

import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
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
import raf.console.pdfreader.ui.theme.AppTheme
import raf.console.pdfreader.viewmodel.PdfViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : ComponentActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private val REQUEST_CODE_DOCUMENT = 1002
    private val REQUEST_CODE_MANAGE_STORAGE = 1003

    private val viewModel: PdfViewModel by viewModels()
    private var pendingPdfUri: Uri? = null

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




        /*intent?.data?.let { uri ->
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }*/

        /*intent?.data?.let { uri ->
            if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } else {
                // Обработка случая, когда постоянное разрешение не предоставлено
                // Можно запросить временный доступ или показать сообщение пользователю
            }
        }*/

       // handleIncomingIntent(intent)

        handleIncomingIntent(intent)

        intent?.data?.let { uri ->
            try {
                // Проверяем, есть ли флаг PERSISTABLE в исходном Intent
                if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } else {
                    // Для временных URI просто запрашиваем временный доступ
                    contentResolver.query(uri, null, null, null, null)?.close()
                }
            } catch (e: SecurityException) {
                // Обработка ошибки доступа
                Log.e("MainActivity", "Failed to get access to URI: $uri", e)
                // Можно показать пользователю сообщение об ошибке
            }
        }

        //val context = LocalContext.current
        //context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state = viewModel.stateFlow.collectAsState()

                    intent?.data?.let { uri ->
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
        intent?.data?.let { uri ->
            try {
                if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                // Обновляем данные в ViewModel
                viewModel.openResource(ResourceType.Remote(uri.toString(), headers = hashMapOf("" to "")))
            } catch (e: Exception) {
                Log.e("MainActivity", "Error handling new intent", e)
            }
        }
    }

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
        // Та же логика обработки URI, что и в onCreate
    }*/

    /*override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }*/

    /*private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    // Проверяем MIME тип
                    if (contentResolver.getType(uri) == "application/pdf") {
                        viewModel.openResource(ResourceType.Local(uri))
                    }
                }
            }
        }
    }*/



    /*private fun handleIncomingIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    try {
                        // Запрашиваем постоянные разрешения
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        // Теперь можно безопасно открывать файл
                        openPdfFromUri(uri)
                    } catch (e: SecurityException) {
                        // Обработка ошибки, если разрешения не даны
                        //showError("Нет доступа к файлу. Пожалуйста, выберите файл снова.")
                    }
                }
            }
        }
    }*/

    private fun handleIncomingIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            try {
                // 1. Попробуем определить реальный MIME-тип
                val mimeType = contentResolver.getType(uri) ?: "application/pdf"

                // 2. Если это не PDF - выходим
                if (!mimeType.equals("application/pdf", ignoreCase = true)) {
                    Toast.makeText(this, "Файл не является PDF", Toast.LENGTH_SHORT).show()
                    return
                }

                // 3. Пробуем получить доступ к файлу (даже без persistable прав)
                val inputStream = contentResolver.openInputStream(uri)
                inputStream?.use { stream ->
                    // 4. Если дошли сюда - доступ есть, можно работать с файлом
                    viewModel.openResource(ResourceType.Remote(uri.toString()))

                    // 5. Пробуем получить постоянные права (если доступны)
                    try {
                        if (intent?.flags?.and(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } else {

                        }
                    } catch (e: SecurityException) {
                        Log.w("MainActivity", "Cannot get persistable permissions for $uri")
                    }
                } ?: run {
                    Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error handling URI", e)
                Toast.makeText(this, "Ошибка при открытии файла", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleWhatsAppUri(uri: Uri) {
        try {
            // 1. Запрашиваем временные разрешения
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            // 2. Копируем файл во внутреннее хранилище
            val cachedFile = copyFileToCache(uri) ?: throw IOException("Не удалось скопировать файл")

            // 3. Открываем копию файла
            viewModel.openResource(ResourceType.Local(Uri.fromFile(cachedFile)))

        } catch (e: SecurityException) {
            // Если нет разрешений, просим пользователя выбрать файл через системный пикер
            openDocumentPicker()
        } catch (e: Exception) {
            //showError("Ошибка: ${e.localizedMessage}")
        }
    }

    private fun copyFileToCache(uri: Uri): File? {
        return try {
            val cacheFile = File(cacheDir, "temp_${System.currentTimeMillis()}.pdf")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    private fun copyFileToTempStorage(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "temp_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun openPdfFromUri(uri: Uri) {
        try {
            // Проверяем MIME тип
            val mimeType = contentResolver.getType(uri)
            if (mimeType == "application/pdf") {
                viewModel.openResource(ResourceType.Local(uri))
            } else {
                //showError("Выбранный файл не является PDF")
            }
        } catch (e: Exception) {
            //showError("Ошибка открытия файла: ${e.localizedMessage}")
        }
    }


    private fun makeDefaultPdfViewer() {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setDataAndType(Uri.parse("file://dummy.pdf"), "application/pdf")

        val chooser = Intent.createChooser(intent, "Выберите PDF ридер")
        startActivity(chooser)
    }

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

            SelectionElement(
                title = "Открыть файл",
                text = "Открыть файл в памяти устройства"
            ) {
                openDocumentPicker()
            }

            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ){
                Text(text = "Прокрутка")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = viewModel.switchState.value,
                    onCheckedChange = {
                        viewModel.switchState.value = it
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Страницы")
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

    fun shareFile(file: File) {
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

    fun isRtl(): Boolean {
        return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    }

    fun sharePdfState(pdfState: PdfReaderState) {
        pdfState.file?.let { file ->
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
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
            startActivity(shareIntent)
        } ?: run {
            Toast.makeText(
                this,
                "Ошибка...файл поврежден или еще что-то случилось...",
                Toast.LENGTH_SHORT
            ).show()
        }
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


