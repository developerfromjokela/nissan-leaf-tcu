package com.developerfromjokela.nissanleaftelematics.utils

class UDSError private constructor(
    val sid: Int,
    val nrc: Int,
    val extra: ByteArray
) {
    val serviceName: String get() = SERVICES[sid] ?: "Unknown"
    val nrcName: String get() = NRCS[nrc] ?: "Unknown"

    override fun toString(): String {
        val base = "Response to 0x%02X (%s): NRC 0x%02X - %s"
            .format(sid, serviceName, nrc, nrcName)
        return if (extra.isEmpty()) base
        else "$base [extra: ${extra.joinToString(" ") { "%02X".format(it) }}]"
    }

    companion object {
        const val NEGATIVE_RESPONSE_SID = 0x7F

        fun parse(data: ByteArray): UDSError {
            require(data.size >= 3) {
                "Too short for a negative response"
            }
            val first = data[0].toInt() and 0xFF
            require(first == NEGATIVE_RESPONSE_SID) {
                "Not a negative response, expected 0x7F".format(first)
            }
            return UDSError(
                sid = data[1].toInt() and 0xFF,
                nrc = data[2].toInt() and 0xFF,
                extra = data.copyOfRange(3, data.size)
            )
        }

        fun parseOrNull(data: ByteArray): UDSError? =
            runCatching { parse(data) }.getOrNull()

        private val NRCS = mapOf(
            0x10 to "generalReject",
            0x11 to "serviceNotSupported",
            0x12 to "subFunctionNotSupported",
            0x13 to "incorrectMessageLengthOrInvalidFormat",
            0x21 to "busyRepeatRequest",
            0x22 to "conditionsNotCorrectOrRequestSequenceError",
            0x23 to "routineNotComplete",
            0x24 to "requestSequenceError",
            0x31 to "requestOutOfRange",
            0x33 to "securityAccessDenied-SecurityAccessRequested",
            0x35 to "invalidKey",
            0x36 to "exceedNumberOfAttempts",
            0x37 to "requiredTimeDelayNotExpired",
            0x40 to "downloadNotAccepted",
            0x41 to "improperDownloadType",
            0x42 to "cannotDownloadToSpecifiedAddress",
            0x43 to "cannotDownloadNumberOfBytesRequested",
            0x50 to "uploadNotAccepted",
            0x51 to "improperUploadType",
            0x52 to "cannotUploadFromSpecifiedAddress",
            0x53 to "cannotUploadNumberOfBytesRequested",
            0x70 to "uploadDownloadNotAccepted",
            0x71 to "transferDataSuspended",
            0x72 to "generalProgrammingFailure",
            0x73 to "wrongBlockSequenceCounter",
            0x74 to "illegalAddressInBlockTransfer",
            0x75 to "illegalByteCountInBlockTransfer",
            0x76 to "illegalBlockTransferType",
            0x77 to "blockTransferDataChecksumError",
            0x78 to "requestCorrectlyReceived-ResponsePending",
            0x79 to "incorrectByteCountDuringBlockTransfer",
            0x7E to "subFunctionNotSupportedInActiveSession",
            0x7F to "serviceNotSupportedInActiveSession",
            0x80 to "serviceNotSupportedInActiveDiagnosticMode",
            0x81 to "rpmTooHigh",
            0x82 to "rpmTooLow",
            0x83 to "engineIsRunning",
            0x84 to "engineIsNotRunning",
            0x85 to "engineRunTimeTooLow",
            0x86 to "temperatureTooHigh",
            0x87 to "temperatureTooLow",
            0x88 to "vehicleSpeedTooHigh",
            0x89 to "vehicleSpeedTooLow",
            0x8A to "throttle/PedalTooHigh",
            0x8B to "throttle/PedalTooLow",
            0x8C to "transmissionRangeInNeutral",
            0x8D to "transmissionRangeInGear",
            0x8F to "brakeSwitchNotClosed",
            0x90 to "shifterLeverNotInPark",
            0x91 to "torqueConverterClutchLocked",
            0x92 to "voltageTooHigh",
            0x93 to "voltageTooLow"
        )

        private val SERVICES = mapOf(
            0x10 to "DiagnosticSessionControl",
            0x11 to "ECUReset",
            0x14 to "ClearDiagnosticInformation",
            0x19 to "ReadDTCInformation",
            0x21 to "ReadDataByNissanIdentifier",
            0x22 to "ReadDataByIdentifier",
            0x23 to "ReadMemoryByAddress",
            0x24 to "ReadScalingDataByIdentifier",
            0x27 to "SecurityAccess",
            0x28 to "CommunicationControl",
            0x29 to "Authentication",
            0x2A to "ReadDataByPeriodicIdentifier",
            0x2C to "DynamicallyDefineDataIdentifier",
            0x2E to "WriteDataByIdentifier",
            0x2F to "InputOutputControlByIdentifier",
            0x31 to "RoutineControl",
            0x34 to "RequestDownload",
            0x35 to "RequestUpload",
            0x36 to "TransferData",
            0x37 to "RequestTransferExit",
            0x3B to "WriteDataByNissanIdentifier",
            0x38 to "RequestFileTransfer",
            0x3D to "WriteMemoryByAddress",
            0x3E to "TesterPresent",
            0x85 to "ControlDTCSetting",
            0x86 to "ResponseOnEvent",
            0x87 to "LinkControl"
        )
    }
}