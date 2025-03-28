package com.developerfromjokela.nissanleaftelematics.diag

import java.lang.Exception

data class CanPacket(
    val dataLength: Int,
    val data: ByteArray? = null
)

class CanPayloadParser {
    @OptIn(ExperimentalStdlibApi::class)
    fun parse(hexInput: String, skipIntegrityCheck: Boolean = false): CanPacket {
        val cleanHex = hexInput.replace(Regex("\\s+"), "")
        if (cleanHex.length < 8) throw IllegalArgumentException("Input too short: $cleanHex")

        var dataLength = 0
        var data: ByteArray? = null
        for (chunk in cleanHex.chunked(16)) {
            val packetId = chunk.substring(0, 2).toInt(radix = 16)
            if (packetId == 16) {
                if (chunk.length < 8) {
                    println("Header malformed $chunk")
                    continue
                }
                println(chunk)
                // Header
                dataLength = chunk.substring(2, 4).toInt(16)
                val firstDataChunk = chunk.substring(4).hexToByteArray()
                data = ByteArray(0)
                data += firstDataChunk
            } else if (packetId >= 30 && data != null && dataLength > data.size) {
                // Data
                val newChunk = chunk.substring(2).hexToByteArray()
                println("datasize: ${data.size}, newchunksize: ${newChunk.size}, maxlen: $dataLength")
                data += if (data.size+newChunk.size > dataLength) {
                    println("overflow, cutting down to 0 -> ${dataLength-data.size}")
                    val newSplittedArr = ByteArray(dataLength-data.size)
                    newChunk.copyInto(newSplittedArr, 0, 0, dataLength-data.size)
                    newSplittedArr
                } else {
                    newChunk
                }
            }
        }
        println("DATALEN $dataLength, READDATA:${data?.size}")
        println(data)

        if (dataLength != data?.size && !skipIntegrityCheck) {
            throw Exception("Data length mismatch! Missing ${dataLength-(data?.size ?: 0)} bytes! Please try reading again.")
        }

        return CanPacket(
            dataLength = dataLength,
            data = data
        )
    }

    fun toReadableString(packet: CanPacket): String? {
        return packet.data?.let { String(it).trim('\u0000') }
    }
}
