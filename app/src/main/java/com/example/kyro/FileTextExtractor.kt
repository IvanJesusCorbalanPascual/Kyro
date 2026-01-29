package com.example.kyro

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.BufferedReader
import java.io.InputStreamReader

object FileTextExtractor {

    // Función que detecta si es un PDF o TXT para sacar el texto
    fun leerContenidoArchivo(context: Context, uri: Uri): String {
        val tipoMime = context.contentResolver.getType(uri) ?: ""

        return try {
            if (tipoMime.contains("pdf")) {
                leerPdf(context, uri)
            } else if (tipoMime.contains("text") || tipoMime.contains("plain")) {
                leerTxt(context, uri)
            } else {
                "" // Si no es compatible, devolvemos vacío
            }
        } catch (e: Exception) {
            Log.e("FileExtractor", "Error leyendo archivo", e)
            ""
        }
    }

    // Lee el PDF con la libreria PDFBox
    private fun leerPdf(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        var document: PDDocument? = null
        return try {
            document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            stripper.getText(document).trim()
        } finally {
            document?.close()
            inputStream?.close()
        }
    }

    // Lee archivos .txt
    private fun leerTxt(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val builder = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            builder.append(line).append("\n")
        }
        reader.close()
        return builder.toString().trim()
    }

    fun leerDesdeUrl(urlString: String): String {
        return try {
            val inputStream = java.net.URL(urlString).openStream()

            // Detecta si es PDF o Texto por la extensión del enlace
            if (urlString.contains(".pdf", ignoreCase = true)) {
                var document: PDDocument? = null
                try {
                    document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    stripper.getText(document).trim()
                } finally {
                    document?.close()
                    inputStream.close()
                }
            } else {
                // Asumimos texto plano
                val texto = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()
                texto
            }
        } catch (e: Exception) {
            Log.e("FileExtractor", "Error descargando archivo: $urlString", e)
            ""
        }
    }
}
