package com.droidhen.formalautosim.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ExternalStorageController {

    companion object {
        fun saveJffToDownloads(context: Context, jffContent: String, fileName: String) {
            val fileNameWithExtension = "$fileName.jff"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileNameWithExtension)
                    put(MediaStore.Downloads.MIME_TYPE, "text/xml")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jffContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileNameWithExtension)

                try {
                    FileOutputStream(file).use { outputStream ->
                        outputStream.write(jffContent.toByteArray(Charsets.UTF_8))
                        outputStream.flush()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun shareJffFile(context: Context, jffContent: String, fileName: String) {
            val file = File(context.cacheDir, "$fileName.jff")
            file.writeText(jffContent)

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share automata with your friends"))
        }

    }
}