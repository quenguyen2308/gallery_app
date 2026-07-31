package com.gallery.ui.creation

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Minimal GIF89a encoder: fixed RGB332 (256-color) global palette + standard variable-width
 * LZW packing. Trades palette fidelity for simplicity — no per-image color quantization pass.
 */
object GifEncoder {

    private const val COLOR_BITS = 8

    fun encode(frames: List<Bitmap>, frameDelayMs: Int): ByteArray {
        require(frames.isNotEmpty()) { "Cần ít nhất 1 khung hình" }
        val width = frames[0].width
        val height = frames[0].height
        val out = ByteArrayOutputStream()

        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeLogicalScreenDescriptor(out, width, height)
        writeGlobalColorTable(out)
        writeNetscapeLoopExtension(out)

        frames.forEach { frame ->
            val scaled = if (frame.width == width && frame.height == height) {
                frame
            } else {
                Bitmap.createScaledBitmap(frame, width, height, true)
            }
            writeGraphicControlExtension(out, frameDelayMs)
            writeImageDescriptor(out, width, height)
            out.write(COLOR_BITS)
            lzwEncode(quantize(scaled), COLOR_BITS, out)
        }
        out.write(0x3B)
        return out.toByteArray()
    }

    private fun quantize(bitmap: Bitmap): ByteArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return ByteArray(pixels.size) { i ->
            val p = pixels[i]
            val r = Color.red(p) shr 5
            val g = Color.green(p) shr 5
            val b = Color.blue(p) shr 6
            ((r shl 5) or (g shl 2) or b).toByte()
        }
    }

    private fun writeLogicalScreenDescriptor(out: OutputStream, width: Int, height: Int) {
        writeShortLE(out, width)
        writeShortLE(out, height)
        out.write(0xF7) // global color table, 256 entries
        out.write(0x00) // background color index
        out.write(0x00) // pixel aspect ratio
    }

    private fun writeGlobalColorTable(out: OutputStream) {
        for (i in 0 until 256) {
            val r = (i shr 5) and 0x07
            val g = (i shr 2) and 0x07
            val b = i and 0x03
            out.write(r * 255 / 7)
            out.write(g * 255 / 7)
            out.write(b * 255 / 3)
        }
    }

    private fun writeNetscapeLoopExtension(out: OutputStream) {
        out.write(0x21)
        out.write(0xFF)
        out.write(0x0B)
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(0x03)
        out.write(0x01)
        writeShortLE(out, 0) // infinite loop
        out.write(0x00)
    }

    private fun writeGraphicControlExtension(out: OutputStream, delayMs: Int) {
        out.write(0x21)
        out.write(0xF9)
        out.write(0x04)
        out.write(0x00) // no disposal, no transparency
        writeShortLE(out, (delayMs / 10).coerceAtLeast(1))
        out.write(0x00)
        out.write(0x00)
    }

    private fun writeImageDescriptor(out: OutputStream, width: Int, height: Int) {
        out.write(0x2C)
        writeShortLE(out, 0)
        writeShortLE(out, 0)
        writeShortLE(out, width)
        writeShortLE(out, height)
        out.write(0x00)
    }

    private fun writeShortLE(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun lzwEncode(pixels: ByteArray, colorBits: Int, out: OutputStream) {
        val clearCode = 1 shl colorBits
        val eoiCode = clearCode + 1
        var codeSize = colorBits + 1
        var nextCode = eoiCode + 1
        var table = HashMap<String, Int>()

        fun resetTable() {
            table = HashMap()
            for (i in 0 until clearCode) table[i.toChar().toString()] = i
            nextCode = eoiCode + 1
            codeSize = colorBits + 1
        }
        resetTable()

        val writer = SubBlockBitWriter(out)
        writer.writeCode(clearCode, codeSize)

        var current = (pixels[0].toInt() and 0xFF).toChar().toString()
        for (i in 1 until pixels.size) {
            val symbol = (pixels[i].toInt() and 0xFF).toChar()
            val combined = current + symbol
            if (table.containsKey(combined)) {
                current = combined
            } else {
                writer.writeCode(table.getValue(current), codeSize)
                if (nextCode < 4096) {
                    table[combined] = nextCode
                    nextCode++
                    if (nextCode == (1 shl codeSize) && codeSize < 12) codeSize++
                } else {
                    writer.writeCode(clearCode, codeSize)
                    resetTable()
                }
                current = symbol.toString()
            }
        }
        writer.writeCode(table.getValue(current), codeSize)
        writer.writeCode(eoiCode, codeSize)
        writer.flush()
    }

    private class SubBlockBitWriter(private val out: OutputStream) {
        private var bitBuffer = 0
        private var bitCount = 0
        private val subBlock = ByteArrayOutputStream()

        fun writeCode(code: Int, codeSize: Int) {
            bitBuffer = bitBuffer or (code shl bitCount)
            bitCount += codeSize
            while (bitCount >= 8) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = bitBuffer ushr 8
                bitCount -= 8
                if (subBlock.size() == 255) flushSubBlock()
            }
        }

        fun flush() {
            if (bitCount > 0) {
                subBlock.write(bitBuffer and 0xFF)
                bitBuffer = 0
                bitCount = 0
            }
            flushSubBlock()
            out.write(0x00)
        }

        private fun flushSubBlock() {
            if (subBlock.size() > 0) {
                out.write(subBlock.size())
                subBlock.writeTo(out)
                subBlock.reset()
            }
        }
    }
}
