package raf.console.pdfreader.ui.about

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.ShapeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tom_roush.pdfbox.BuildConfig
import raf.console.archnotes.utils.ChromeCustomTabUtil
import raf.console.pdfreader.MainActivity
import raf.console.pdfreader.R
import raf.console.pdfreader.ads.AdManagerHolder
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
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName = "v1.0.0"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
    ) {
        // Header card with back + logo + name
        item {
            ElevatedCard(shape = ShapeDefaults.Large) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Кнопка назад (по клику — onBack)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = "Назад",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    val activity = (context as? Activity)

                                    if (activity != null) {
                                        var adShown = false

                                        AdManagerHolder.showInterstitialAd(
                                            activity = activity,
                                            adUnitId = "R-M-16660854-3",
                                            onShown = { adShown = true },
                                            onDismissed = {
                                                onBack()
                                            }
                                        )

                                        // Если реклама не начала показываться за 200 мс — идём назад
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            if (!adShown) {
                                                onBack()
                                            }
                                        }, 200)
                                    } else {
                                        onBack()
                                    }
                                }
                        )


                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "О приложении",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Логотип приложения — замени на свой ресурс при желании
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Пункты
                    ListItem(
                        title = "Сообщить об ошибке",
                        subtitle = "Открыть почтовый клиент",
                        icon = painterResource(id = R.drawable.info_24px),
                    ) {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("raf_android-dev@mail.ru"))
                            putExtra(Intent.EXTRA_SUBJECT, "Обратная связь")
                            putExtra(Intent.EXTRA_TEXT, "Здравствуйте,\n\n")
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(emailIntent, "Выберите почтовый клиент")
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Нет почтовых клиентов", Toast.LENGTH_SHORT).show()
                        }
                    }

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        title = "Оценить приложение",
                        subtitle = "Открыть страницу в RuStore",
                        icon = painterResource(id = R.drawable.rate),
                    ) {
                        ChromeCustomTabUtil.openUrl(
                            context = context,
                            url = "https://www.rustore.ru/catalog/app/raf.console.pdfreader",
                        )
                    }

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        title = "Другие приложения",
                        subtitle = "Смотреть приложения разработчика в RuStore",
                        icon = painterResource(id = R.drawable.apps_24px),
                    ) {
                        ChromeCustomTabUtil.openUrl(
                            context = context,
                            url = "https://www.rustore.ru/catalog/developer/90b1826e",
                        )
                        // копируем ссылку в буфер
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(
                                "Ссылка на другие приложения",
                                "https://www.rustore.ru/catalog/developer/90b1826e"
                            )
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Соц.ссылки / профиль / исходники / лицензия
        item {
            ElevatedCard(shape = ShapeDefaults.Large) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ListItem(
                        title = "Профиль разработчика",
                        subtitle = "Открыть профиль на GitHub",
                        icon = painterResource(id = R.drawable.github_24),
                    ) {
                        ChromeCustomTabUtil.openUrl(
                            context = context,
                            url = "https://github.com/Raf0707",
                        )
                    }

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        title = "Исходный код",
                        subtitle = "Открыть репозиторий на GitHub",
                        icon = painterResource(id = R.drawable.code_24px),
                    ) {
                        ChromeCustomTabUtil.openUrl(
                            context = context,
                            url = "https://github.com/Raf0707/PDF_Reader",
                        )
                    }

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        title = "Лицензия",
                        subtitle = "Apache 2.0 (использует проект bouquet)",
                        icon = painterResource(id = R.drawable.license),
                    ) {
                        ChromeCustomTabUtil.openUrl(
                            context = context,
                            url = "https://github.com/Raf0707/PDF_Reader/blob/master/LICENSE",
                        )
                    }

                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        title = "Версия приложения",
                        subtitle = versionName,
                        icon = painterResource(id = R.drawable.update_24)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Рек/кросс-промо/поделиться
        item {
            ElevatedCard(shape = ShapeDefaults.Large) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.shareapp),
                        contentDescription = "Share Icon",
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .size(96.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Поделиться приложением",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "«PDF Reader без рекламы»",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        title = "Поделиться",
                        subtitle = "Отправить ссылку другу",
                        icon = painterResource(id = R.drawable.shareapp),
                    ) {
                        val link = "https://www.rustore.ru/catalog/app/raf.console.pdfreader"
                        // буфер
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Скачайте приложение", link)
                        )
                        // шаринг
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Скачайте приложение «PDF Reader» в каталоге RuStore\n\n$link"
                            )
                        }
                        context.startActivity(Intent.createChooser(share, "Поделиться приложением"))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

    }
}

@Preview(showBackground = true)
@Composable
fun ListItem(
    title: String = "Заголовок",
    subtitle: String = "Описание действия",
    icon: Painter = painterResource(id = R.drawable.ic_launcher_foreground),
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(end = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
