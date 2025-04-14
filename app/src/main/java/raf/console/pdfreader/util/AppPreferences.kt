package raf.console.pdfreader.util

import android.content.Context

class AppPreferences(context: Context) {
    private val sharedPref = context.getSharedPreferences("pdf_reader_prefs", Context.MODE_PRIVATE)

    fun savePdfUri(uri: String) {
        sharedPref.edit().apply {
            putString("last_pdf_uri", uri)
            apply()
        }
    }

    fun getLastPdfUri(): String? {
        return try {
            sharedPref.getString("last_pdf_uri", null)
        } catch (e: Exception) {
            null
        }
    }

    fun saveReadingMode(isHorizontal: Boolean) {
        sharedPref.edit().apply {
            putBoolean("reading_mode", isHorizontal)
            apply()
        }
    }

    fun getReadingMode(): Boolean {
        return sharedPref.getBoolean("reading_mode", false)
    }

    fun saveCurrentPage(page: Int) {
        sharedPref.edit().apply {
            putInt("current_page", page)
            apply()
        }
    }

    fun getCurrentPage(): Int {
        return sharedPref.getInt("current_page", 0)
    }

    fun clearSavedData() {
        sharedPref.edit().clear().apply()
    }
}