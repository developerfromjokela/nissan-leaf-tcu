package com.developerfromjokela.nissanleaftelematics.diag


class CanPacketParser {
    fun parse(input: Any): CanDataPacket {
        // Convert input to list of lines
        val lines = when (input) {
            is String -> splitIntoLines(input)
            is List<*> -> input.filterIsInstance<String>()
            else -> throw IllegalArgumentException("Input must be String or List<String>")
        }

        if (lines.isEmpty()) throw IllegalArgumentException("Empty input")

        // Parse first line: 1083611301676463
        val firstLine = lines[0]
        if (firstLine.length < 8) throw IllegalArgumentException("Invalid first line format")

        val messageId = firstLine.substring(0, 2).toInt(radix = 16)  // 1083
        val msgLen = firstLine.substring(2, 4).toUInt(radix = 16)  // 1083
        println("mID:$messageId, len $msgLen")
        val packetId = firstLine.substring(4, 6)   // 61
        println("mID:$packetId")
        val dataId = firstLine.substring(6, 8)     // 19 or 13
        val firstChunk = firstLine.substring(8, 16)  // 3 bytes (6 hex chars)

        if (messageId != 16) {
            throw IllegalArgumentException("Invalid incoming msg!")
        }



        // Parse subsequent data chunks
        val dataChunks = lines.drop(1)
            .associate { line ->
                val chunkId = line.substring(0, 2).toInt(16)
                val data = line.substring(2)
                chunkId to data
            }

        return CanDataPacket(
            messageId = messageId.toString(),
            packetId = packetId,
            dataId = dataId,
            marker = "",
            firstChunk = firstChunk,
            dataChunks = dataChunks
        )
    }

    private fun splitIntoLines(input: String): List<String> {
        val cleanInput = input.replace(Regex("\\s+"), "") // Remove all whitespace
        if (cleanInput.length < 16) throw IllegalArgumentException("Input too short")

        val lines = mutableListOf<String>()
        var remaining = cleanInput

        // First line is special (has header + 3 bytes)
        lines.add(remaining.substring(0, 16))  // 10 bytes header + 3 bytes data
        remaining = remaining.substring(16)

        // Split remaining into 18-char chunks (2 bytes ID + 8 bytes data)
        while (remaining.isNotEmpty()) {
            val chunkSize = minOf(18, remaining.length)
            lines.add(remaining.substring(0, chunkSize))
            remaining = if (chunkSize < remaining.length) remaining.substring(chunkSize) else ""
        }

        return lines
    }

    fun toReadableString(packet: CanDataPacket): String {
        val allHex = buildString {
            append(packet.firstChunk)
            packet.dataChunks.toSortedMap().forEach { (_, data) ->
                append(data)
            }
        }

        val bytes = allHex.chunked(2)
            .mapNotNull { it.toIntOrNull(16)?.toByte() }
            .toByteArray()

        return String(bytes).trim('\u0000')
    }
}