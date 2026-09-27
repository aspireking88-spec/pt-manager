package com.example.printer

import com.example.data.model.Voucher
import com.example.data.model.createdAt
import com.example.data.model.maxDevices
import com.example.data.model.password
import com.example.data.model.price
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintRenderer {

    fun renderVoucher(
        voucher: Voucher,
        settings: PrintDesignSettings
    ): android.graphics.Bitmap = android.graphics.Bitmap.createBitmap(384, 400, android.graphics.Bitmap.Config.ARGB_8888)

    fun renderVoucher(
        code: String = "c4p3j2x",
        profileName: String = "1Hour",
        validPeriod: String = "1 Hours",
        quota: String? = null,
        status: String = "Not Used",
        settings: PrintDesignSettings = PrintDesignSettings(),
        paperConfig: PaperConfiguration = PaperConfiguration(),
        siteName: String? = null,
        printDateTime: String = "2026-09-09 15:30",
        isThermalOutput: Boolean = false
    ): android.graphics.Bitmap = android.graphics.Bitmap.createBitmap(384, 400, android.graphics.Bitmap.Config.ARGB_8888)

    fun renderVoucherReceipt(
        voucher: Voucher,
        settings: PrintDesignSettings
    ): ByteArray {
        val printer = EscPosPrinter()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val createdStr = dateFormat.format(Date(voucher.createdAt))

        val lineWidth = if (settings.paperConfig.paperWidth == PaperWidth.MM_80) 48 else 32

        printer.reset()
            .alignCenter()
            .bold(true)
            .textSize(2, 2)
            .line(settings.headerTitle)
            .textSize(1, 1)
            .bold(false)
            .line(settings.subHeader)
            .divider('=', lineWidth)
            .alignLeft()
            .line("Package : ${voucher.packageName ?: "Standard"}")
            .line("Duration: ${voucher.duration ?: "Unlimited"}")
            .line("Devices : ${voucher.maxDevices} device(s)")
            .line("Created : $createdStr")

        val priceVal = voucher.price
        if (priceVal != null && priceVal > 0) {
            printer.line("Price   : $${String.format(Locale.US, "%.2f", priceVal)}")
        }

        printer.divider('-', lineWidth)
            .alignCenter()
            .bold(true)
            .line("VOUCHER CODE")
            .textSize(2, 2)
            .line(voucher.effectiveCode)
            .textSize(1, 1)

        if (!voucher.password.isNullOrBlank()) {
            printer.line("Password: ${voucher.password}")
        }

        printer.bold(false)
            .divider('-', lineWidth)

        if (settings.showSupportContact) {
            printer.alignCenter().line(settings.supportContact)
        }

        if (settings.showFooter && settings.footerText.isNotBlank()) {
            printer.alignCenter().line(settings.footerText)
        }

        printer.feed(settings.paperConfig.feedLinesAfterPrint)

        if (settings.paperConfig.cutPaper) {
            printer.cut()
        }

        return printer.build()
    }

    fun renderPlainText(
        voucher: Voucher,
        settings: PrintDesignSettings
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val createdStr = dateFormat.format(Date(voucher.createdAt))
        val lineWidth = if (settings.paperConfig.paperWidth == PaperWidth.MM_80) 48 else 32
        val border = "=".repeat(lineWidth)
        val div = "-".repeat(lineWidth)

        sb.appendLine(settings.headerTitle.center(lineWidth))
        sb.appendLine(settings.subHeader.center(lineWidth))
        sb.appendLine(border)
        sb.appendLine("Package : ${voucher.packageName ?: "Standard"}")
        sb.appendLine("Duration: ${voucher.duration ?: "Unlimited"}")
        sb.appendLine("Devices : ${voucher.maxDevices} device(s)")
        sb.appendLine("Created : $createdStr")
        val priceVal2 = voucher.price
        if (priceVal2 != null && priceVal2 > 0) {
            sb.appendLine("Price   : $${String.format(Locale.US, "%.2f", priceVal2)}")
        }
        sb.appendLine(div)
        sb.appendLine("VOUCHER CODE".center(lineWidth))
        sb.appendLine(voucher.effectiveCode.center(lineWidth))
        if (!voucher.password.isNullOrBlank()) {
            sb.appendLine("Password: ${voucher.password}".center(lineWidth))
        }
        sb.appendLine(div)
        if (settings.showSupportContact) {
            sb.appendLine(settings.supportContact.center(lineWidth))
        }
        if (settings.showFooter && settings.footerText.isNotBlank()) {
            sb.appendLine(settings.footerText.center(lineWidth))
        }
        return sb.toString()
    }

    private fun String.center(width: Int): String {
        if (length >= width) return this
        val pad = (width - length) / 2
        return " ".repeat(pad) + this
    }
}
