package com.developerfromjokela.nissanleaftelematics.bluetooth.ble

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class BleSocketInputStream : InputStream() {
    private val queue: BlockingQueue<Byte> = LinkedBlockingQueue()
    private var closed = false

    // Called from onCharacteristicChanged when data arrives
    internal fun onDataReceived(data: ByteArray) {
        for (byte in data) {
            queue.offer(byte)
        }
    }

    override fun read(): Int {
        if (closed) throw IOException("Stream closed")
        return try {
            val byte = queue.poll(5, TimeUnit.SECONDS) ?: -1
            if (byte.toInt() == -1) -1 else byte.toInt() and 0xFF
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            -1
        }
    }

    override fun available(): Int = queue.size

    override fun close() {
        closed = true
        queue.clear()
        super.close()
    }
}