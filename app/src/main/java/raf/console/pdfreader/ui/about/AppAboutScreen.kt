package raf.console.pdfreader.ui.about

import android.content.*
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import raf.console.archnotes.utils.ChromeCustomTabUtil
import raf.console.pdfreader.R
import raf.console.pdfreader.ui.theme.AppTheme

class AppAboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AboutScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Кнопка "Назад"
        SelectionElement(
            icon = painterResource(R.drawable.back),
            title = "Назад",
            text = "Вернуться на главный экран"
        ) { onBack() }

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
                ChromeCustomTabUtil.openUrl(context, url)
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
            val clip = ClipData.newPlainText(
                "Ссылка на другие приложения",
                "https://www.rustore.ru/catalog/developer/90b1826e"
            )
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
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("raf_android-dev@mail.ru"))
                putExtra(Intent.EXTRA_SUBJECT, "Обратная связь")
                putExtra(Intent.EXTRA_TEXT, "Здравствуйте,\n\n")
            }
            try {
                context.startActivity(Intent.createChooser(emailIntent, "Выберите почтовый клиент"))
            } catch (e: Exception) {
                Toast.makeText(context, "Нет почтовых клиентов", Toast.LENGTH_SHORT).show()
            }
        }

        SelectionElement(
            icon = painterResource(id = raf.console.pdfreader.R.drawable.shareapp),
            title = "Поделиться приложением",
            text = "Поделитесь приложением «PDF Reader без рекламы»"
        ) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Скачайте приложение",
                "https://www.rustore.ru/catalog/app/raf.console.pdfreader"
            )
            clipboard.setPrimaryClip(clip)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Скачайте приложение «PDF Reader без рекламы» в каталоге RuStore \n\n https://www.rustore.ru/catalog/app/raf.console.pdfreader"
                )
            }
            context.startActivity(Intent.createChooser(shareIntent, "Поделиться приложением"))
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
