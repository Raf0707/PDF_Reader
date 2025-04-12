package raf.console.pdfreader.data

import android.net.Uri
import java.util.Date

data class PdfFile(
    val name: String,
    val date: Date,
    val size: Long,
    val uri: Uri
)
