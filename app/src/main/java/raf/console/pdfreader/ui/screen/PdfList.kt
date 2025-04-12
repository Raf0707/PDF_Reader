package raf.console.pdfreader.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import raf.console.pdfreader.data.PdfFile
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PdfList(pdfFiles: List<PdfFile>, context: Context) {
    LazyColumn {
        items(pdfFiles) { pdfFile ->
            PdfListItem(pdfFile = pdfFile, context = context)
        }
    }
}

@Composable
fun PdfListItem(pdfFile: PdfFile, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { openPdf(pdfFile.uri, context) }
    ) {
        Image(
            bitmap = getPdfThumbnail(pdfFile.uri, context).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = pdfFile.name, fontSize = 20.sp)
            Text(
                text = "${SimpleDateFormat("d MMMM, yyyy", Locale.getDefault()).format(pdfFile.date)} - ${pdfFile.size / 1024} KB",
                fontSize = 17.sp,
                color = Color.Gray
            )
        }
    }
}

fun getPdfThumbnail(uri: Uri, context: Context): Bitmap {
    val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
    val pdfRenderer = parcelFileDescriptor?.let { PdfRenderer(it) }
    val page = pdfRenderer?.openPage(0)
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    page?.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page?.close()
    pdfRenderer?.close()
    parcelFileDescriptor?.close()
    return bitmap
}

private fun openPdf(uri: Uri, context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}