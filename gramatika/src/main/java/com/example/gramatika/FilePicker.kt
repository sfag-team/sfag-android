package com.example.gramatika

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import org.w3c.dom.Document
import java.io.OutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Composable
fun FilePicker(grammarViewModel: Grammar, navController: NavController) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            grammarViewModel.loadFromXmlUri(context, it)
        }
        navController.navigate("grammarScreen") // Navigate back to grammar screen after file is loaded
    }
    LaunchedEffect(Unit) {
        filePickerLauncher.launch("*/*")
    }
}

@Composable
fun FileSave(grammarViewModel: Grammar, navController: NavController) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
    ) { uri: Uri? ->
        uri?.let {
            saveToXml(grammarViewModel.getIndividualRules(), context, it)
        }
        navController.navigate("grammarScreen")
    }

    LaunchedEffect(Unit) {
        launcher.launch("grammar")
    }
}


fun saveToXml(rules: List<GrammarRule> ,context: Context, uri: Uri) {
    try {
        val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
        outputStream?.use { stream ->
            val dbFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = dbFactory.newDocumentBuilder()
            val doc: Document = docBuilder.newDocument()

            // Root structure
            val structureElement = doc.createElement("structure")
            doc.appendChild(structureElement)

            // Type
            val typeElement = doc.createElement("type")
            typeElement.appendChild(doc.createTextNode("grammar"))
            structureElement.appendChild(typeElement)

            // Productions
            rules.forEach { rule ->
                val productionElement = doc.createElement("production")

                val leftElement = doc.createElement("left")
                leftElement.appendChild(doc.createTextNode(rule.left))
                productionElement.appendChild(leftElement)

                val rightElement = doc.createElement("right")
                // Treat epsilon ("ε") as an empty <right/> tag
                if (rule.right == "ε") {
                    productionElement.appendChild(rightElement) // Empty right
                } else {
                    rightElement.appendChild(doc.createTextNode(rule.right))
                    productionElement.appendChild(rightElement)
                }

                structureElement.appendChild(productionElement)
            }

            // Write the content to output
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val source = DOMSource(doc)
            val result = StreamResult(stream)
            transformer.transform(source, result)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
