Проект создан на основе форка проекта bouquet (по ссылке https://github.com/GRizzi91/bouquet) и распространяется по лицензии Apache 2.0

Уведомляю разработчика об изменениях в файлах библиотеки: HorizontalPdfReaderState.kt и VerticalPdfReaderState.kt:
- Добавлена функция jumpTo(page: Int, coroutineScope: CoroutineScope) - реализация перехода на страницу в диапазоне документа.

Реализация:

```
fun jumpTo(page: Int) {
    if (page < 1 || page > pdfPageCount) return
    
    val targetIndex = page - 1
    coroutineScope.launch {
        lazyState.animateScrollToItem(targetIndex)
    }
}
```

- В примере приложения в MainActivity модернизированы функции PDFView и HPDFView - добавлено отслеживание состояния загрузки и ошибки (повреждение файла, пустота файла). Ранее, в момент подгрузки большого файла отображался белый экран и page: 0/0. Сейчас отображается сообщение "Загрузка...".

- подписи страницы и другие переведены мной на русский язык

- добавлена кнопка "Поделиться файлом" рядом с текстовым полем "Страница: .../..."
