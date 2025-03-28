package com.developerfromjokela.nissanleaftelematics.diag

data class CanDataPacket(
    val messageId: String,          // 1083
    val packetId: String,          // 61
    val dataId: String,            // 19
    val marker: String,            // 01
    val firstChunk: String,        // First 3 bytes
    val dataChunks: Map<Int, String>  // Subsequent chunks with their identifiers
)
