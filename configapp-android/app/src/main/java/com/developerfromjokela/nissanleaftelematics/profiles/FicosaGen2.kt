package com.developerfromjokela.nissanleaftelematics.profiles

import com.developerfromjokela.nissanleaftelematics.R
import com.developerfromjokela.nissanleaftelematics.config.TCUConfigItem

class FicosaGen2(
    override var nameRes: Int = R.string.ficosa_gen2,
    override var canRX: Int = 783,
    override var canTX: Int = 746,
    override var initSeq: List<String> = listOf("0210FA", "02311000", "02711001"),
    override var configItems: List<TCUConfigItem> = listOf(
        TCUConfigItem(configId = 0x6c71, fieldLength = 1, type = 2, fieldMaxLength = 1, readOnly = false, uiName = R.string.activation),
        TCUConfigItem(configId = 0xfd1d, fieldLength = 20, type = 3, fieldMaxLength = 20, readOnly = true, uiName = R.string.signal_level_rssi),
        TCUConfigItem(configId = 0x81, fieldLength = 17, type = 0, fieldMaxLength = 17, readOnly = false, uiName = R.string.vin),
        TCUConfigItem(configId = 0xfd1c, fieldLength = 15, type = 5, fieldMaxLength = 15, readOnly = false, uiName = R.string.imei),
        TCUConfigItem(configId = 0xfd30, fieldLength = 160, type = 4, fieldMaxLength = 160, readOnly = false, uiName = R.string.apn_settings),
        TCUConfigItem(configId = 0xfe9a, fieldLength = 128, type = 5, fieldMaxLength = 128, readOnly = true, uiName = R.string.unique_id),
        TCUConfigItem(configId = 0x6c10, fieldLength = 128, type = 5, fieldMaxLength = 128, readOnly = false, uiName = R.string.obs_server_url),
        TCUConfigItem(configId = 0x6c12, fieldLength = 32, type = 5, fieldMaxLength = 32, readOnly = false, uiName = R.string.obs_server_port),

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
                return "2E" + ("%02x".format(item.configId)
                    .uppercase()) + "01" + data.toHexString(HexFormat.Default).uppercase()
            }
            2 -> {
                // TCU activation write
                val actState = data[0].toInt()
                return "2E" + ("%02x".format(item.configId)
                    .uppercase()) + (if (actState > 0) "01" else "02").uppercase()
            }
        }
        return ""
    }

    override fun makeOBDRead(item: TCUConfigItem): String {
        return (if (item.type == 0) "0221" else "0322")+("%02x".format(item.configId).uppercase())
    }
}