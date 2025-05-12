package com.droidhen.formalautosim.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.core.content.FileProvider
import com.droidhen.formalautosim.core.entities.states.State
import com.droidhen.formalautosim.core.entities.transitions.Transition
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.FileOutputStream
import javax.xml.parsers.DocumentBuilderFactory

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

    fun parseJff(jffXml: String): Pair<List<State>, List<Transition>> {
        val states = mutableListOf<State>()
        val transitions = mutableListOf<Transition>()

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val inputStream = jffXml.byteInputStream()
        val doc = docBuilder.parse(inputStream)
        val automaton = doc.getElementsByTagName("automaton").item(0)

        val nodeList = automaton.childNodes
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)

            if (node.nodeType != Node.ELEMENT_NODE) continue
            val element = node as Element

            when (element.tagName) {
                "state" -> {
                    val id = element.getAttribute("id").toInt()
                    val name = element.getAttribute("name")
                    val x = element.getElementsByTagName("x").item(0).textContent.toFloatOrNull() ?: 0f
                    val y = element.getElementsByTagName("y").item(0).textContent.toFloatOrNull() ?: 0f
                    val isInitial = element.getElementsByTagName("initial").length > 0
                    val isFinal = element.getElementsByTagName("final").length > 0

                    states.add(
                        State(
                            finite = isFinal,
                            initial = isInitial,
                            index = id,
                            name = name,
                            isCurrent = false,
                            position = Offset(x, y)
                        )
                    )
                }

                "transition" -> {
                    val from = element.getElementsByTagName("from").item(0).textContent.toInt()
                    val to = element.getElementsByTagName("to").item(0).textContent.toInt()
                    val read = element.getElementsByTagName("read").item(0)?.textContent ?: ""

                    transitions.add(Transition(read, from, to))
                }
            }
        }

        return Pair(states, transitions)
    }

}