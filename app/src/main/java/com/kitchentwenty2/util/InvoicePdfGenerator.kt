package com.kitchentwenty2.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.kitchentwenty2.R
import com.kitchentwenty2.domain.model.OrderDetailUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generate(context: Context, order: OrderDetailUiState): android.net.Uri {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 41, 59) }
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun text(value: String, size: Float = 11f, bold: Boolean = false, color: Int = Color.rgb(30, 41, 59)) {
            paint.textSize = size
            paint.color = color
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(value, MARGIN, y, paint)
            y += size + 6f
        }

        fun rightText(value: String, right: Float, size: Float = 11f, bold: Boolean = false, color: Int = Color.rgb(30, 41, 59)) {
            paint.textSize = size
            paint.color = color
            paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(value, right - paint.measureText(value), y, paint)
        }

        fun divider() {
            paint.color = Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)
            y += 12f
        }

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.DKGRAY
        val invoiceDate = "Invoice Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
        canvas.drawText(
            invoiceDate,
            PAGE_WIDTH - MARGIN - paint.measureText(invoiceDate),
            y,
            paint
        )

        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.company_logo)
            ?: error("Company logo resource could not be loaded")
        canvas.drawBitmap(logo, null, android.graphics.RectF(MARGIN, y, MARGIN + 48f, y + 48f), paint)
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.rgb(234, 88, 12)
        canvas.drawText("Kitchen Twenty2", MARGIN + 62f, y + 22f, paint)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.DKGRAY
        canvas.drawText("Rumia Dey Mondal, Dum Dum Park, Kolkata - 700055", MARGIN + 62f, y + 40f, paint)
        y += 72f
        divider()

        y += 10f
        text("INVOICE", 18f, bold = true)
        text("Order #${order.orderId}")
        text("Event Date: ${order.orderDate}")
        text("Customer: ${order.customerName}")
        text("Contact: ${order.customerPhone}")
        text("Address: ${order.customerAddress}")
        y += 8f
        divider()

        y += 10f
        text("ITEMS", 13f, bold = true)
        val itemRight = 370f
        val quantityRight = 400f
        val priceRight = 480f
        val subtotalRight = PAGE_WIDTH - MARGIN
        val tableTop = y
        val headerHeight = 24f
        val rowHeight = 22f
        val borderColor = Color.rgb(148, 163, 184)
        val headerColor = Color.rgb(219, 234, 254)
        val headerTextColor = Color.rgb(30, 58, 138)

        paint.style = Paint.Style.FILL
        paint.color = headerColor
        canvas.drawRect(MARGIN, tableTop, subtotalRight, tableTop + headerHeight, paint)
        drawTableBorders(canvas, paint, tableTop, headerHeight, MARGIN, itemRight, quantityRight, priceRight, subtotalRight, borderColor)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = headerTextColor
        canvas.drawText("Item", MARGIN + 8f, tableTop + 16f, paint)
        y = tableTop + 16f
        rightText("Qty", quantityRight - 8f, size = 9f, bold = true, color = headerTextColor)
        rightText("Price", priceRight - 8f, size = 9f, bold = true, color = headerTextColor)
        rightText("Subtotal", subtotalRight - 8f, size = 9f, bold = true, color = headerTextColor)
        y = tableTop + headerHeight

        order.items.forEach { item ->
            ensureSpace(rowHeight)
            val rowTop = y
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRect(MARGIN, rowTop, subtotalRight, rowTop + rowHeight, paint)
            drawTableBorders(canvas, paint, rowTop, rowHeight, MARGIN, itemRight, quantityRight, priceRight, subtotalRight, borderColor)
            paint.textSize = 9f
            paint.typeface = Typeface.DEFAULT
            paint.color = Color.rgb(30, 41, 59)
            val itemName = ellipsize(item.itemName, paint, itemRight - MARGIN)
            canvas.drawText(itemName, MARGIN + 8f, rowTop + 15f, paint)
            y = rowTop + 15f
            rightText(item.quantity.toString(), quantityRight - 8f, size = 9f)
            rightText(money(item.unitPrice), priceRight - 8f, size = 9f)
            rightText(money(item.subtotal), subtotalRight - 8f, size = 9f)
            y = rowTop + rowHeight
        }

        y += 6f
        ensureSpace(190f)
        divider()

        y += 10f
        text("FINANCIAL SUMMARY", 13f, bold = true)
        summaryLine(canvas, paint, "Subtotal", money(order.items.sumOf { it.subtotal }), y).also { y = it }
        summaryLine(canvas, paint, "Applied discount", money(order.upfrontDiscount + order.settlementDiscount), y).also { y = it }
        summaryLine(canvas, paint, "Total order value", money(order.totalAmount), y).also { y = it }
        summaryLine(canvas, paint, "Advance payments", money(order.paymentLogs.filter { it.type.contains("advance", true) }.sumOf { it.amount }), y).also { y = it }
        summaryLine(canvas, paint, "Interim payments", money(order.paymentLogs.filter { it.type.contains("intermediate", true) }.sumOf { it.amount }), y).also { y = it }
        summaryLine(canvas, paint, "Remaining balance", money(order.finalDueAfterSettlementDiscount), y, bold = true).also { y = it }

        if (order.status.name == "FULLY_PAID" || order.finalDueAfterSettlementDiscount <= 0.0) {
            y += 8f
            paint.color = Color.rgb(21, 128, 61)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(MARGIN, y - 18f, MARGIN + 125f, y + 12f, 6f, 6f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 11f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("FULLY PAID", MARGIN + 15f, y + 2f, paint)
            y += 25f
        }

        ensureSpace(35f)
        y += 10f
        divider()

        y += 10f
        text("PAYMENT HISTORY", 13f, bold = true)
        order.paymentLogs.forEach { payment ->
            ensureSpace(22f)
            text("${payment.date}  ${payment.type}  ${money(payment.amount)}", 10f)
        }

        document.finishPage(page)
        val directory = File(context.cacheDir, "invoices").apply { mkdirs() }
        val file = File(directory, "invoice_${order.orderId}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun summaryLine(canvas: Canvas, paint: Paint, label: String, value: String, y: Float, bold: Boolean = false): Float {
        paint.textSize = 11f
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText(label, MARGIN, y, paint)
        canvas.drawText(value, PAGE_WIDTH - MARGIN - paint.measureText(value), y, paint)
        return y + 20f
    }

    private fun drawTableBorders(
        canvas: Canvas,
        paint: Paint,
        top: Float,
        height: Float,
        left: Float,
        itemRight: Float,
        quantityRight: Float,
        priceRight: Float,
        right: Float,
        borderColor: Int
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = borderColor
        canvas.drawRect(left, top, right, top + height, paint)
        canvas.drawLine(itemRight, top, itemRight, top + height, paint)
        canvas.drawLine(quantityRight, top, quantityRight, top + height, paint)
        canvas.drawLine(priceRight, top, priceRight, top + height, paint)
        paint.style = Paint.Style.FILL
    }

    private fun pageInfo(number: Int) = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create()

    private fun money(value: Double): String = String.format(Locale.getDefault(), "Rs. %.2f", value)

    private fun ellipsize(value: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        val suffix = "..."
        val availableWidth = (maxWidth - paint.measureText(suffix)).coerceAtLeast(0f)
        val count = paint.breakText(value, true, availableWidth, null)
        return value.take(count) + suffix
    }
}
