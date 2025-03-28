package com.developerfromjokela.nissanleaftelematics

import android.widget.Toast
import com.developerfromjokela.nissanleaftelematics.diag.CanPacketParser
import com.developerfromjokela.nissanleaftelematics.diag.CanPayloadMaker
import com.developerfromjokela.nissanleaftelematics.diag.CanPayloadParser
import org.junit.Test

import org.junit.Assert.*
import java.util.Arrays

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun payloadMaker() {
        println(CanPayloadMaker().processCommandToFrames("3B 13 01 69 6E 74 65 72 6E 65 74 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00"))
    }


    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun makeDataRequest() {
        println("3B 13 01 69 6E 74 65 72 6E 65 74 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00".trim().replace(" ", "")) // from ddt4all
        val dataVal = "internet".toByteArray(charset = Charsets.US_ASCII)
        val dataValBuff = ByteArray(128)
        dataVal.copyInto(dataValBuff)
        println("3B"+("%02x".format(0x13).uppercase())+"01"+dataValBuff.toHexString(HexFormat.Default).uppercase())
    }
}