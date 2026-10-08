package com.developerfromjokela.nissanleaftelematics.config

class TCUConfigItem {
    var configId: Int = -1
    var fieldLength: Int = -1
    var uiName: Int = -1
    var readOnly: Boolean = true
    var type = 0
    var fieldMaxLength: Int = -1
    var currentReadValue: ByteArray? = null

    var customInitSeq: List<String>? = null

    constructor(
        configId: Int,
        fieldLength: Int,
        type: Int,
        uiName: Int,
        readOnly: Boolean,
        fieldMaxLength: Int,
        customInitSeq: List<String>? = null
    ) {
        this.configId = configId
        this.fieldLength = fieldLength
        this.type = type
        this.uiName = uiName
        this.readOnly = readOnly
        this.fieldMaxLength = fieldMaxLength
        this.customInitSeq = customInitSeq
    }
}