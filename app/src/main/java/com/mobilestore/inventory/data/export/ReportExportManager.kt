package com.mobilestore.inventory.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.mobilestore.inventory.ui.viewmodel.BrandTally
import com.mobilestore.inventory.ui.viewmodel.ReportsUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Export reports to PDF and Excel" per the spec. PDF is built with
 * Android's built-in android.graphics.pdf.PdfDocument (no extra dependency).
 * "Excel" is written as CSV — Excel, Google Sheets, and every spreadsheet
 * app open CSV natively — rather than pulling in a large third-party
 * library (e.g. Apache POI) to author a binary .xlsx, which would add
 * significant APK size and dependency risk for a used-phone shop's simple
 * tabular reports.
 */
@Singleton
class ReportExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val reportsDir: File
        get() = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())

    private fun shareUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    suspend fun exportPdf(
        storeName: String,
        periodLabel: String,
        state: ReportsUiState,
        currencyFormat: NumberFormat
    ): Uri = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 48f
        val left = 40f

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val mutedPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.DKGRAY }

        fun newPageIfNeeded(needed: Float) {
            if (y + needed > 800f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 48f
            }
        }

        canvas.drawText(storeName, left, y, titlePaint); y += 22f
        canvas.drawText("$periodLabel Report", left, y, bodyPaint); y += 14f
        canvas.drawText("Generated ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(java.util.Date())}", left, y, mutedPaint)
        y += 28f

        fun row(label: String, value: String) {
            newPageIfNeeded(20f)
            canvas.drawText(label, left, y, bodyPaint)
            canvas.drawText(value, left + 260f, y, bodyPaint)
            y += 20f
        }

        canvas.drawText("Summary", left, y, headerPaint); y += 20f
        row("Purchases", "${state.purchaseCount} phones • ${currencyFormat.format(state.purchaseValue)}")
        row("Sales", "${state.saleCount} phones • ${currencyFormat.format(state.saleValue)}")
        row("Profit", currencyFormat.format(state.profit))
        row("Current Stock", "${state.currentStockCount} phones • ${currencyFormat.format(state.currentStockValue)}")
        y += 12f

        fun tallyTable(title: String, tallies: List<BrandTally>) {
            newPageIfNeeded(30f)
            canvas.drawText(title, left, y, headerPaint); y += 20f
            if (tallies.isEmpty()) {
                canvas.drawText("No data for this period.", left, y, mutedPaint); y += 18f
            } else {
                tallies.forEach {
                    newPageIfNeeded(18f)
                    canvas.drawText("${it.brand} — ${it.unitsSold} sold — ${currencyFormat.format(it.revenue)}", left, y, bodyPaint)
                    y += 18f
                }
            }
            y += 10f
        }

        tallyTable("Top Selling Brands", state.topBrands)
        tallyTable("Top Selling Models", state.topModels)

        document.finishPage(page)

        val file = File(reportsDir, "Report_${timestamp()}.pdf")
        document.writeTo(file.outputStream())
        document.close()

        shareUri(file)
    }

    suspend fun exportCsv(
        storeName: String,
        periodLabel: String,
        state: ReportsUiState,
        currencyFormat: NumberFormat
    ): Uri = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        fun csvLine(vararg cells: String) {
            sb.append(cells.joinToString(",") { cell -> "\"${cell.replace("\"", "\"\"")}\"" })
            sb.append("\r\n")
        }

        csvLine("Store", storeName)
        csvLine("Period", periodLabel)
        csvLine("Generated", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(java.util.Date()))
        csvLine()
        csvLine("Metric", "Value")
        csvLine("Purchases (count)", state.purchaseCount.toString())
        csvLine("Purchase Value", currencyFormat.format(state.purchaseValue))
        csvLine("Sales (count)", state.saleCount.toString())
        csvLine("Sales Value", currencyFormat.format(state.saleValue))
        csvLine("Profit", currencyFormat.format(state.profit))
        csvLine("Current Stock (count)", state.currentStockCount.toString())
        csvLine("Current Stock Value", currencyFormat.format(state.currentStockValue))
        csvLine()
        csvLine("Top Selling Brands", "Units Sold", "Revenue")
        state.topBrands.forEach { csvLine(it.brand, it.unitsSold.toString(), currencyFormat.format(it.revenue)) }
        csvLine()
        csvLine("Top Selling Models", "Units Sold", "Revenue")
        state.topModels.forEach { csvLine(it.brand, it.unitsSold.toString(), currencyFormat.format(it.revenue)) }

        val file = File(reportsDir, "Report_${timestamp()}.csv")
        file.writeText(sb.toString())

        shareUri(file)
    }
}
