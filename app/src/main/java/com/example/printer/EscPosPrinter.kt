package com.example.printer

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosPrinter {
    private val buffer = ByteArrayOutputStream()

    fun reset(): EscPosPrinter {
        buffer.write(byteArrayOf(0x1B, 0x40))
        return this
    }

    fun alignLeft(): EscPosPrinter {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x00))
        return this
    }

    fun alignCenter(): EscPosPrinter {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x01))
        return this
    }

    fun alignRight(): EscPosPrinter {
        buffer.write(byteArrayOf(0x1B, 0x61, 0x02))
        return this
    }

    fun bold(enable: Boolean): EscPosPrinter {
        buffer.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00))
        return this
    }

    fun textSize(widthMult: Int, heightMult: Int): EscPosPrinter {
        val w = (widthMult - 1).coerceIn(0, 7)
        val h = (heightMult - 1).coerceIn(0, 7)
        val n = (w shl 4) or h
        buffer.write(byteArrayOf(0x1D, 0x21, n.toByte()))
        return this
    }

    fun text(str: String): EscPosPrinter {
        buffer.write(str.toByteArray(Charset.forName("GBK")))
        return this
    }

    fun line(str: String = ""): EscPosPrinter {
        text(str)
        buffer.write(0x0A)
        return this
    }

    fun feed(lines: Int = 1): EscPosPrinter {
        repeat(lines) { buffer.write(0x0A) }
        return this
    }

    fun divider(char: Char = '-', length: Int = 32): EscPosPrinter {
        line(char.toString().repeat(length))
        return this
    }

    fun cut(): EscPosPrinter {
        buffer.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return this
    }

    fun build(): ByteArray = buffer.toByteArray()
}
