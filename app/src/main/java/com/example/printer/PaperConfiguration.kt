package com.example.printer

enum class PaperWidth(val mm: Int, val dotsPerLine: Int, val label: String) {
    MM_58(58, 384, "58mm Standard Receipt"),
    MM_80(80, 576, "80mm Wide Receipt")
}

data class PaperConfiguration(
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    val feedLinesAfterPrint: Int = 3,
    val cutPaper: Boolean = true
) {
    val paperWidthMm: Int
        get() = paperWidth.mm
}
