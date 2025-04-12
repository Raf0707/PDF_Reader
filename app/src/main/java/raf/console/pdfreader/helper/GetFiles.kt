package raf.console.pdfreader.helper

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import raf.console.pdfreader.data.PdfFile
import java.util.Date

fun getPdfFiles(contentResolver: ContentResolver): List<PdfFile> {
    val pdfFiles = mutableListOf<PdfFile>()
    val uri: Uri = MediaStore.Files.getContentUri("external")
    val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.SIZE
    )
    val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
    val selectionArgs = arrayOf("application/pdf")
    val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

    contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn)
            val date = cursor.getLong(dateColumn)
            val size = cursor.getLong(sizeColumn)
            val contentUri = ContentUris.withAppendedId(uri, id)
            pdfFiles.add(PdfFile(name, Date(date * 1000), size, contentUri))
        }
    }
    return pdfFiles
}