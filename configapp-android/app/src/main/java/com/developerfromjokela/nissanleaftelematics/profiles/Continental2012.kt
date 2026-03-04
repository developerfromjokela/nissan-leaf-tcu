package com.developerfromjokela.nissanleaftelematics.profiles

import com.developerfromjokela.nissanleaftelematics.R
import com.developerfromjokela.nissanleaftelematics.config.TCUConfigItem

class Continental2012(
    override var nameRes: Int = R.string.continental20212,
    override var canRX: Int = 783,
    override var canTX: Int = 746,
    override var initSeq: List<String> = listOf("0210C0"),
    override var configItems: List<TCUConfigItem> = listOf(
        TCUConfigItem(configId = 0x04, fieldLength = 1, type = 2, fieldMaxLength = 1, readOnly = false, uiName = R.string.activation),
        TCUConfigItem(configId = 0x09, fieldLength = 20, type = 3, fieldMaxLength = 20, readOnly = true, uiName = R.string.signal_level),
        TCUConfigItem(configId = 0x81, fieldLength = 17, type = 0, fieldMaxLength = 17, readOnly = false, uiName = R.string.vin),
        TCUConfigItem(configId = 0x10, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_dial),
        TCUConfigItem(configId = 0x11, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_user),
        TCUConfigItem(configId = 0x12, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_pass),
        TCUConfigItem(configId = 0x13, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_name),
        TCUConfigItem(configId = 0x14, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.dns1),
        TCUConfigItem(configId = 0x15, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.dns2),
        TCUConfigItem(configId = 0x16, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.proxy),
        TCUConfigItem(configId = 0x17, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.proxy_port),
        TCUConfigItem(configId = 0x18, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_connection_type),
        TCUConfigItem(configId = 0x19, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.server_hostname),
    )
) : AbstractTCUProfile() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun makeOBDWrite(item: TCUConfigItem, data: ByteArray): String {
        when (item.type) {
            0 -> {
                // VIN write
                return "3B81" + data.toHexString(HexFormat.Default).uppercase() + "0000"
            }
            1 -> {
                // Normal write
                return "3B" + ("%02x".format(item.configId)
                    .uppercase()) + "01" + data.toHexString(HexFormat.Default).uppercase()
            }
            2 -> {
                // TCU activation write
                val actState = data[0].toInt()
                return "3B" + ("%02x".format(item.configId)
                    .uppercase()) + (if (actState > 0) "01" else "00").uppercase()
            }
        }
        return ""
    }

    override fun makeOBDRead(item: TCUConfigItem): String {
        return "0221"+("%02x".format(item.configId).uppercase())
    }
}