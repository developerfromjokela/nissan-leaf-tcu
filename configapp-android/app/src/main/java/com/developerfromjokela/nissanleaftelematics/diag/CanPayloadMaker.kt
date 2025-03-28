package com.developerfromjokela.nissanleaftelematics.diag

class CanPayloadMaker {

    fun processCommandToFrames(command: String): List<String> {
        // Remove spaces and whitespace
        val cleanedCommand = command.trim().replace(" ", "")

        // Check length conditions
        if (cleanedCommand.length % 2 != 0 || cleanedCommand.isEmpty()) {
            throw IllegalArgumentException("ODD ERROR")
        }

        // Check if all characters are valid hex digits
        val hexDigits = "0123456789ABCDEFabcdef"
        if (!cleanedCommand.all { it in hexDigits }) {
            throw IllegalArgumentException("HEX Error")
        }

        // Do framing
        val rawCommands = mutableListOf<String>()
        val cmdLen = cleanedCommand.length / 2

        if (cmdLen < 8) {  // single frame
            val formattedLen = "%02X".format(cmdLen)
            rawCommands.add("$formattedLen$cleanedCommand")
        } else {
            // first frame
            val formattedLen = "%03X".format(cmdLen).takeLast(3)
            rawCommands.add("1$formattedLen${cleanedCommand.take(12)}")
            var remainingCommand = cleanedCommand.drop(12)

            // consecutive frames
            var frameNumber = 1
            while (remainingCommand.isNotEmpty()) {
                val frameNumHex = "%X".format(frameNumber).takeLast(1)
                rawCommands.add("2$frameNumHex${remainingCommand.take(14)}")
                frameNumber++
                remainingCommand = remainingCommand.drop(14)
            }
        }

        return rawCommands
    }
}