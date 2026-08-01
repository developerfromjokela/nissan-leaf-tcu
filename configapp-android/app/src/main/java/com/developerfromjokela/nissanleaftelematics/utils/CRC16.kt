package com.developerfromjokela.nissanleaftelematics.utils

class CRC16 {

    companion object {
        private const val POLY = 0x8408
        private const val INIT = 0xFFFF
        private const val XOR_OUT = 0xFFFF

        // Precomputed lookup table for speed
        private val table: IntArray = IntArray(256).also { t ->
            for (i in 0 until 256) {
                var crc = i
                repeat(8) {
                    crc = if (crc and 1 != 0) {
                        (crc ushr 1) xor POLY
                    } else {
                        crc ushr 1
                    }
                }
                t[i] = crc and 0xFFFF
            }
        }
    }

    fun calculate(data: ByteArray): Int {
        var crc = INIT
        for (b in data) {
            val index = (crc xor (b.toInt() and 0xFF)) and 0xFF
            crc = (crc ushr 8) xor table[index]
        }
        return (crc xor XOR_OUT) and 0xFFFF
    }


    fun calculate(text: String): Int = calculate(text.toByteArray(Charsets.UTF_8))

    fun calculateBytesLittleEndian(data: ByteArray): ByteArray {
        val crc = calculate(data)
        return byteArrayOf((crc and 0xFF).toByte(), ((crc ushr 8) and 0xFF).toByte())
    }

    fun calculateBytesBigEndian(data: ByteArray): ByteArray {
        val crc = calculate(data)
        return byteArrayOf(((crc ushr 8) and 0xFF).toByte(), (crc and 0xFF).toByte())
    }
}