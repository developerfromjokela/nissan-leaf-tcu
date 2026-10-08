package com.developerfromjokela.nissanleaftelematics.utils


class DtcUtils {

    class ParseException(message: String) : Exception(message)

    data class ParsedResponse(
        val statusAvailabilityMask: Int,
        val dtcs: List<DtcRecord>
    )

    companion object {

        fun parseResponse(bytes: ByteArray): ParsedResponse {
            require(bytes.isNotEmpty()) { "Empty response" }
            val u = bytes.map { it.toInt() and 0xFF }

            if (u[0] == 0x7F) {
                val nrc = if (u.size > 2) u[2] else -1
                throw ParseException("Negative response, NRC=0x%02X (%s)".format(nrc, nrcDescription(nrc)))
            }
            if (u[0] != 0x59) throw ParseException("Unexpected SID 0x%02X, expected 0x59".format(u[0]))
            if (u.size < 3) throw ParseException("Response too short")
            if (u[1] != 0x02) throw ParseException("Unexpected sub-function 0x%02X, expected 0x02".format(u[1]))

            val availabilityMask = u[2]
            val dtcBytes = u.drop(3)
            if (dtcBytes.size % 4 != 0) {
                throw ParseException("DTC record data length (${dtcBytes.size}) is not a multiple of 4")
            }

            val records = dtcBytes.chunked(4).map { chunk ->
                DtcRecord(
                    byteHigh = chunk[0],
                    byteMid = chunk[1],
                    byteLow = chunk[2],
                    status = DtcStatus(chunk[3])
                )
            }
            return ParsedResponse(availabilityMask, records)
        }

        private fun nrcDescription(nrc: Int): String = when (nrc) {
            0x10 -> "generalReject"
            0x11 -> "serviceNotSupported"
            0x12 -> "subFunctionNotSupported"
            0x13 -> "incorrectMessageLengthOrInvalidFormat"
            0x22 -> "conditionsNotCorrect"
            0x31 -> "requestOutOfRange"
            0x33 -> "securityAccessDenied"
            else -> "unknown"
        }

        private fun buildReport(parsed: ParsedResponse): String = buildString {
            if (parsed.dtcs.isEmpty()) {
                appendLine("No DTCs reported.")
            } else {
                parsed.dtcs.forEachIndexed { index, dtc ->
                    appendLine("[${index + 1}] DTC ${dtc.code}")
                    appendLine("CODE: 0x%06X".format(dtc.rawCode))
                    appendLine("STATUS:  0x%02X".format(dtc.status.raw))
                    val flags = dtc.status.activeFlags()
                    if (flags.isEmpty()) {
                        appendLine("== Status flags: none set")
                    } else {
                        appendLine("== Status flags:")
                        flags.forEach { appendLine("        - $it") }
                    }
                    appendLine()
                }
            }
        }

        fun generateReport(bytes: ByteArray): String = try {
            buildReport(parseResponse(bytes))
        } catch (e: ParseException) {
            "UDS DTC ERR: ${e.message}"
        }
    }

}