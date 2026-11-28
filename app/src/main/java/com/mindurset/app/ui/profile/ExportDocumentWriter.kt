package com.mindurset.app.ui.profile

import android.content.Context
import android.net.Uri
import java.io.IOException

/** Thin Storage Access Framework writer kept out of Compose screens. */
object ExportDocumentWriter {
    fun writeUtf8(context: Context, uri: Uri, content: String): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
            true
        } ?: false
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
