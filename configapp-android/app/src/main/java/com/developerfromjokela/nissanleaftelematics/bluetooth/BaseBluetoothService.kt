package com.developerfromjokela.nissanleaftelematics.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.os.Handler
import com.pnuema.android.obd.commands.BaseObdCommand

open class BaseBluetoothService(
    protected val context: Context,
    protected val mHandler: Handler?
): BluetoothGattCallback() {
    companion object {
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3

    }

    var mState: Int = STATE_NONE


    open fun connect(device: BluetoothDevice) {
        throw Exception("please override!")
    }
    open fun stop() {
        throw Exception("please override!")
    }

    open fun makeOBDCommand(command: BaseObdCommand, id: Int) {
        throw Exception("please override!")
    }

    open fun makeOBDMultiCommand(commands: MutableList<BaseObdCommand>, id: Int) {
        throw Exception("please override!")
    }
}