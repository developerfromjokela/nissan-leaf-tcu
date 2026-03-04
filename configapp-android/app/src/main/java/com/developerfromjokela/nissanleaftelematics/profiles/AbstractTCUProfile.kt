package com.developerfromjokela.nissanleaftelematics.profiles

import com.developerfromjokela.nissanleaftelematics.config.TCUConfigItem

abstract class AbstractTCUProfile {
    abstract var configItems: List<TCUConfigItem>
    abstract var nameRes: Int
    abstract var canRX: Int
    abstract var canTX: Int
    abstract var initSeq: List<String>

    abstract fun makeOBDWrite(item: TCUConfigItem, data: ByteArray): String
    abstract fun makeOBDRead(item: TCUConfigItem): String
}