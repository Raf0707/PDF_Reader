package raf.console.pdfreader

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import raf.console.archnotes.utils.ChromeCustomTabUtil
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

        handleIncomingIntent(intent, isNewIntent = false)

        intent?.data?.let { uri ->
            try {
                // Проверяем, есть ли флаг PERSISTABLE в исходном Intent
                if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                    /*contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )*/
                    //openDocumentPicker()
                    handleIncomingIntent2(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent, isNewIntent = true)
    }

    private fun handlePdfUri(uri: Uri, intent: Intent? = null) {
        try {
            // 1. Всегда пробуем получить персистентные права
            if (intent?.flags?.and(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // 2. Для файлов из WhatsApp/других источников - создаем копию
            if (!isPersistableUri(uri)) {
                val tempFile = copyFileToTempStorage(uri) ?: throw IOException("Failed to copy file")
                viewModel.openResource(ResourceType.Local(Uri.parse(tempFile.absolutePath)))
            } else {
                // 3. Для файлов через OPEN_DOCUMENT - работаем напрямую
                viewModel.openResource(ResourceType.Remote(uri.toString()))
            }

        } catch (e: SecurityException) {
            // Если нет прав доступа - предлагаем выбрать файл через OPEN_DOCUMENT
            openDocumentPicker()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка открытия файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isPersistableUri(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any { it.uri == uri }
    }

    private fun handleIncomingIntent2(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    if (isPdfFile(uri)) {
                        handlePdfUri(uri, intent)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    if (isPdfFile(uri)) {
                        handlePdfUri(uri, intent)
                    }
                }
            }
        }
    }

    private fun isPdfFile(uri: Uri): Boolean {
        return contentResolver.getType(uri)?.equals("application/pdf", ignoreCase = true) == true
    }

    companion object {
        private const val REQUEST_CODE_OPEN_DOCUMENT = 1001
    }

    private fun handleIncomingIntent(intent: Intent?, isNewIntent: Boolean) {
        when {
            intent?.action == Intent.ACTION_VIEW -> {
                intent.data?.let { uri -> processPdfUri(uri, intent) }
            }
            intent?.action == Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    processPdfUri(uri, intent)
                }
            }
            isNewIntent -> {
                // Если это новый Intent, но без данных - возможно нужно обновить UI
                viewModel.stateFlow.value?.let { currentState ->
                    viewModel.openResource(currentState.resource)
                }
            }
        }
    }

    private fun processPdfUri(uri: Uri, originalIntent: Intent) {
        try {
            // 1. Проверяем MIME тип
            val mimeType = contentResolver.getType(uri) ?: "application/pdf"
            if (!mimeType.equals("application/pdf", ignoreCase = true)) {
                Toast.makeText(this, "Файл не является PDF", Toast.LENGTH_SHORT).show()
                return
            }

            // 2. Пробуем получить доступ к файлу
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use {
                // 3. Для файлов с персистентными правами
                if (originalIntent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        viewModel.openResource(ResourceType.Remote(uri.toString()))
                    } catch (e: SecurityException) {
                        // Если не получилось - копируем файл
                        openWithTempCopy(uri)
                    }
                } else {
                    // 4. Для файлов без персистентных прав (WhatsApp/Telegram)
                    openWithTempCopy(uri)
                }
            } ?: run {
                Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка обработки PDF", e)
            Toast.makeText(this, "Ошибка при открытии файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWithTempCopy(uri: Uri) {
        try {
            val tempFile = createTempPdfCopy(uri) ?: throw IOException("Не удалось создать копию файла")
            viewModel.openResource(ResourceType.Local(Uri.fromFile(tempFile)))
        } catch (e: Exception) {
            Log.e("MainActivity", "Ошибка копирования файла", e)
            Toast.makeText(this, "Ошибка при обработке файла", Toast.LENGTH_SHORT).show()
            openDocumentPicker() // Предлагаем выбрать файл через системный пикер
        }
    }

    private fun createTempPdfCopy(uri: Uri): File? {
        return try {
            val tempFile = File(cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf").apply {
                createNewFile()
            }

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            null
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
    fun SelectionElement(
        icon: Painter,        // <- теперь это Painter
        title: String,
        text: String,
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }

    @Composable
    fun SelectionElement(
        icon: Painter,
        title: String,
        text: AnnotatedString? = null,
        onTextClick: ((String) -> Unit)? = null,  // Новый параметр для обработки кликов по ссылкам
        onClick: () -> Unit
    ) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    text?.let {
                        ClickableText(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            onClick = { offset ->
                                text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        onTextClick?.let { it1 -> it1(annotation.item) }
                                    }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SelectionView() {
        val context: Context = LocalContext.current
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

            HorizontalDivider(Modifier.height(8.dp))

            SelectionElement(
                icon = painterResource(id = raf.console.pdfreader.R.drawable.code_24px),
                title = "Исходный код",
                text = "Открыть исходный код приложения"
            ) {
                ChromeCustomTabUtil.openUrl(
                    context = context,
                    url = "https://github.com/Raf0707/PDF_Reader",
                )
            }

            val annotatedText = buildAnnotatedString {
                val baseTextStyle = MaterialTheme.typography.titleMedium.toSpanStyle()
                val defaultColor = MaterialTheme.colorScheme.onSurface
                val bouquetColor = MaterialTheme.colorScheme.primary

                withStyle(style = baseTextStyle.copy(color = defaultColor)) {
                    append("Данный проект выполнен с использованием проекта ")

                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://github.com/GRizzi91/bouquet"
                    )
                    withStyle(
                        style = baseTextStyle.copy(
                            color = bouquetColor,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("bouquet")
                    }
                    pop()

                    append(" и распространяется по лицензии Apache 2.0")
                }
            }

            SelectionElement(
                icon = painterResource(id = raf.console.pdfreader.R.drawable.license),
                title = "Лицензия",
                text = annotatedText,
                onTextClick = { url ->
                    ChromeCustomTabUtil.openUrl(
                        context = context,
                        url = url
                    )
                }
            ) {
                ChromeCustomTabUtil.openUrl(
                    context = context,
                    url = "https://github.com/Raf0707/PDF_Reader/blob/master/LICENSE"
                )
            }

            SelectionElement(
                icon = painterResource(id = raf.console.pdfreader.R.drawable.apps_24px),
                title = "Другие приложения",
                text = "Скачивайте другие приложения в каталоге RuStore"
            ) {
                ChromeCustomTabUtil.openUrl(
                    context = context,
                    url = "https://www.rustore.ru/catalog/developer/90b1826e",
                )

                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Ссылка на другие приложения скопирована", "https://www.rustore.ru/catalog/developer/90b1826e")
                clipboard.setPrimaryClip(clip)
            }


            SelectionElement(
                icon = painterResource(id = raf.console.pdfreader.R.drawable.github_24),
                title = "Профиль разработчика",
                text = "Открыть Github-профиль разработчика"
            ) {
                ChromeCustomTabUtil.openUrl(
                    context = context,
                    url = "https://github.com/Raf0707",
                )
            }

            SelectionElement(
                icon = painterResource(id = raf.console.pdfreader.R.drawable.info_24px),
                title = "v1.0.0",
                text = "Предложите идею или сообщите об ошибке"
            ) {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:") // Только email-клиенты
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("raf_android-dev@mail.ru")) // Адрес
                    putExtra(Intent.EXTRA_SUBJECT, "Обратная связь") // Тема письма
                    putExtra(Intent.EXTRA_TEXT, "Здравствуйте,\n\n") // Текст по умолчанию
                }

                try {
                    context.startActivity(Intent.createChooser(emailIntent, "Выберите почтовый клиент"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        context,
                        "Ошибка: нет доступных почтовых клиентов",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                    Text("Выбрать файл для открытия")
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
                    Text("Выбрать файл для открытия")
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


