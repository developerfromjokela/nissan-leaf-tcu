package com.developerfromjokela.nissanleaftelematics.bluetooth.ble

import java.io.IOException
import java.io.OutputStream

class BleSocketOutputStream(val bleSocket: BleSocket) : OutputStream() {
    private var closed = false

    @Throws(IOException::class)
    override fun write(b: Int) {
        if (closed) throw IOException("Stream closed")
        write(byteArrayOf(b.toByte()))
    }

    @Throws(IOException::class)
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (closed) throw IOException("Stream closed")
        val data = ByteArray(len)
        System.arraycopy(b, off, data, 0, len)
        bleSocket.write(data)
    }

    override fun close() {
        closed = true
        super.close()
    }
}