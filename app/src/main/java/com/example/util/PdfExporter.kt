package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.db.MessageEntity
import com.example.data.db.MessageSender
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun shareTextConversation(context: Context, messages: List<MessageEntity>) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "No messages to share", Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        sb.append("=== OmniAI Chat Transcript ===\n\n")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sb.append("Date: ${dateFormat.format(Date())}\n\n")

        messages.forEach { msg ->
            val sender = if (msg.sender == MessageSender.USER) "User" else "OmniAI"
            sb.append("[$sender]:\n${msg.content}\n")
            if (!msg.translatedText.isNullOrBlank()) {
                sb.append("Translation (${msg.translatedLanguage}):\n${msg.translatedText}\n")
            }
            sb.append("\n-----------------------------------\n\n")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "OmniAI Chat Transcript")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Chat Transcript via"))
    }

    fun generateAndSharePdf(context: Context, messages: List<MessageEntity>) {
        if (messages.isEmpty()) {
            Toast.makeText(context, "No messages to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points
            val margin = 40f
            val contentWidth = (pageWidth - (margin * 2)).toInt()

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val headerPaint = TextPaint().apply {
                color = Color.rgb(24, 43, 73)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subHeaderPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val userHeaderPaint = TextPaint().apply {
                color = Color.rgb(0, 102, 204)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val aiHeaderPaint = TextPaint().apply {
                color = Color.rgb(103, 58, 183)
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.rgb(30, 30, 30)
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var yPos = margin

            // Draw Header
            canvas.drawText("OmniAI Chat Session Transcript", margin, yPos + 18f, headerPaint)
            yPos += 28f

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = "Exported: ${dateFormat.format(Date())} | Total Messages: ${messages.size}"
            canvas.drawText(dateStr, margin, yPos, subHeaderPaint)
            yPos += 14f

            canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
            yPos += 20f

            for (msg in messages) {
                val isUser = msg.sender == MessageSender.USER
                val senderLabel = if (isUser) "USER" else "OMNIAI ASSISTANT"
                val senderPaint = if (isUser) userHeaderPaint else aiHeaderPaint

                if (yPos + 50f > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin
                }

                canvas.drawText(senderLabel, margin, yPos, senderPaint)
                yPos += 16f

                val textContent = msg.content
                val staticLayout = StaticLayout.Builder.obtain(textContent, 0, textContent.length, bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.15f)
                    .setIncludePad(true)
                    .build()

                if (yPos + staticLayout.height > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin
                }

                canvas.save()
                canvas.translate(margin, yPos)
                staticLayout.draw(canvas)
                canvas.restore()

                yPos += staticLayout.height + 12f

                if (!msg.translatedText.isNullOrBlank()) {
                    val transLabel = "Translation (${msg.translatedLanguage}):"
                    canvas.drawText(transLabel, margin, yPos, subHeaderPaint)
                    yPos += 14f

                    val transText = msg.translatedText!!
                    val transLayout = StaticLayout.Builder.obtain(transText, 0, transText.length, bodyPaint, contentWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.15f)
                        .setIncludePad(true)
                        .build()

                    if (yPos + transLayout.height > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        yPos = margin
                    }

                    canvas.save()
                    canvas.translate(margin, yPos)
                    transLayout.draw(canvas)
                    canvas.restore()

                    yPos += transLayout.height + 12f
                }

                canvas.drawLine(margin, yPos, pageWidth - margin, yPos, linePaint)
                yPos += 16f
            }

            pdfDocument.finishPage(page)

            val fileName = "OmniAI_Chat_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(context.cacheDir, fileName)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "OmniAI Conversation PDF")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Export Chat PDF via"))
            Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
