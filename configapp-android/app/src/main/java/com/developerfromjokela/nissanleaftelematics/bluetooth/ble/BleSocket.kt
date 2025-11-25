package com.developerfromjokela.nissanleaftelematics.bluetooth.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import com.developerfromjokela.nissanleaftelematics.MainActivity
import com.developerfromjokela.nissanleaftelematics.R
import com.developerfromjokela.nissanleaftelematics.bluetooth.BaseBluetoothService
import com.pnuema.android.obd.commands.BaseObdCommand
import java.io.IOException
import java.io.OutputStream
import java.security.InvalidParameterException
import java.util.Arrays
import java.util.UUID
import kotlin.math.min

// Code borrowed from: https://github.com/kai-morich/SimpleBluetoothLeTerminal/blob/master/app/src/main/java/de/kai_morich/simple_bluetooth_le_terminal/SerialSocket.java

/**
 * wrap BLE communication into socket like class
 * - connect, disconnect and write as methods,
 * - read + status is returned by SerialListener
 */
@SuppressLint("MissingPermission")
class BleSocket(context: Context, callback: Handler?) : BaseBluetoothService(context, callback) {
    /**
     * delegate device specific behaviour to inner class
     */
    private open class DeviceDelegate {
        open fun connectCharacteristics(s: BluetoothGattService?): Boolean {
            return true
        }

        // following methods only overwritten for Telit devices
        open fun onDescriptorWrite(
            g: BluetoothGatt?,
            d: BluetoothGattDescriptor?,
            status: Int
        ) { /*nop*/
        }

        open fun onCharacteristicChanged(
            g: BluetoothGatt?,
            c: BluetoothGattCharacteristic?
        ) { /*nop*/
        }

        open fun onCharacteristicWrite(
            g: BluetoothGatt?,
            c: BluetoothGattCharacteristic?,
            status: Int
        ) { /*nop*/
        }

        open fun canWrite(): Boolean {
            return true
        }

        open fun disconnect() { /*nop*/
        }
    }

    private val writeBuffer: ArrayList<ByteArray?>

    private var delegate: DeviceDelegate? = null
    private var device: BluetoothDevice? = null
    private var gatt: BluetoothGatt? = null
    private var pairingIntentFilter: IntentFilter

    private var pairingBroadcastReceiver: BroadcastReceiver
    private var readCharacteristic: BluetoothGattCharacteristic? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var inputStream: BleSocketInputStream = BleSocketInputStream()
    private var outputStream: BleSocketOutputStream? = null

    private var writePending = false
    private var canceled = false
    private var connected = false
    private var payloadSize = DEFAULT_MTU - 3

    init {
        if (context is Activity) throw InvalidParameterException("expected non UI context")
        writeBuffer = ArrayList<ByteArray?>()
        pairingIntentFilter = IntentFilter()
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        pairingIntentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
        pairingBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                onPairingBroadcastReceive(context!!, intent!!)
            }
        }
    }

    override fun stop() {
        Log.d(TAG, "disconnect")
        device = null
        canceled = true
        sendState(BaseBluetoothService.STATE_NONE)
        synchronized(writeBuffer) {
            writePending = false
            writeBuffer.clear()
        }
        readCharacteristic = null
        writeCharacteristic = null
        if (delegate != null) delegate!!.disconnect()
        if (gatt != null) {
            Log.d(TAG, "gatt.disconnect")
            gatt!!.disconnect()
            Log.d(TAG, "gatt.close")
            try {
                gatt!!.close()
            } catch (ignored: Exception) {
            }
            gatt = null
            connected = false
        }
        try {
            context.unregisterReceiver(pairingBroadcastReceiver)
        } catch (ignored: java.lang.Exception) {
        }
    }

    private fun sendState(state: Int) {
        mState = state
        // Give the new state to the Handler so the UI Activity can update
        mHandler?.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)?.sendToTarget()
    }

    private fun makeToast(content: String) {
        if (mHandler == null) return
        val msg: Message = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(MainActivity.TOAST, content)
        msg.setData(bundle)
        mHandler.sendMessage(msg)
    }

    private fun getOutputStream(): OutputStream {
        if (outputStream == null) {
            outputStream = BleSocketOutputStream(this)
        }
        return outputStream!!
    }

    override fun makeOBDCommand(command: BaseObdCommand, id: Int) {
        try {
            command.run(inputStream, getOutputStream())
            // Share the sent message back to the UI Activity
            mHandler?.obtainMessage(MainActivity.MESSAGE_RESULT, id, -1, command.rawResult)
                ?.sendToTarget()
        } catch (e: IOException) {
            Log.e(TAG, "Exception during write", e)
            onSerialConnectError(e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Exception during write", e)
            onSerialConnectError(e)
        }
    }

    override fun makeOBDMultiCommand(commands: MutableList<BaseObdCommand>, id: Int) {
        try {
            for (i in commands.indices) {
                val command = commands.get(i)
                command.run(inputStream, getOutputStream())
                // Share the sent message back to the UI Activity
                mHandler?.obtainMessage(MainActivity.MESSAGE_RESULT_MULTI, id, i, command.rawResult)
                    ?.sendToTarget()
            }
            mHandler?.obtainMessage(MainActivity.MESSAGE_RESULT_MULTI, id, -1, null)
                ?.sendToTarget()
        } catch (e: IOException) {
            Log.e(TAG, "Exception during write", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Exception during write", e)
        }
    }


    /**
     * connect-success and most connect-errors are returned asynchronously to listener
     */
    @Throws(IOException::class)
    override fun connect(device: BluetoothDevice) {
        if (connected || gatt != null) throw IOException("already connected")
        this.device = device;
        canceled = false
        Log.d(TAG, "connect " + device)
        sendState(BaseBluetoothService.STATE_CONNECTING)
        context.registerReceiver(pairingBroadcastReceiver, pairingIntentFilter);
        if (Build.VERSION.SDK_INT < 23) {
            Log.d(TAG, "connectGatt")
            gatt = device.connectGatt(context, false, this)
        } else {
            Log.d(TAG, "connectGatt,LE")
            gatt = device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        }
        if (gatt == null) throw IOException("connectGatt failed")
    }

    private fun onPairingBroadcastReceive(context: Context, intent: Intent) {
        // for ARM Mbed, Microbit, ... use pairing from Android bluetooth settings
        // for HM10-clone, ... pairing is initiated here
        val device = intent.getParcelableExtra<BluetoothDevice?>(BluetoothDevice.EXTRA_DEVICE)
        if (device == null || device != this.device) return
        when (intent.getAction()) {
            BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                val pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                Log.d(TAG, "pairing request " + pairingVariant)
                onSerialConnectError(IOException(context.getString(R.string.pairing_request)))
            }

            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                val previousBondState =
                    intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                Log.d(TAG, "bond state " + previousBondState + "->" + bondState)
            }

            else -> Log.d(TAG, "unknown broadcast " + intent.getAction())
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        // status directly taken from gat_api.h, e.g. 133=0x85=GATT_ERROR ~= timeout
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "connect status " + status + ", discoverServices")
            if (!gatt.discoverServices()) onSerialConnectError(IOException("discoverServices failed"))
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            if (connected) onSerialIoError(IOException("gatt status " + status))
            else onSerialConnectError(IOException("gatt status " + status))
        } else {
            Log.d(TAG, "unknown connect state " + newState + " " + status)
        }
        // continues asynchronously in onServicesDiscovered()
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        Log.d(TAG, "servicesDiscovered, status " + status)
        if (canceled) return
        connectCharacteristics1(gatt)
    }

    private fun connectCharacteristics1(gatt: BluetoothGatt) {
        var sync = true
        writePending = false
        for (gattService in gatt.getServices()) {
            if (gattService.getUuid() == BLUETOOTH_LE_OBD) delegate =
                this.ObdService()

            if (delegate != null) {
                sync = delegate!!.connectCharacteristics(gattService)
                break
            }
        }
        if (canceled) return
        if (delegate == null || readCharacteristic == null || writeCharacteristic == null) {
            for (gattService in gatt.getServices()) {
                Log.d(TAG, "service " + gattService.getUuid())
                for (characteristic in gattService.getCharacteristics()) Log.d(
                    TAG,
                    "characteristic " + characteristic.getUuid()
                )
            }
            onSerialConnectError(IOException("no serial profile found"))
            return
        }
        if (sync) connectCharacteristics2(gatt)
    }

    private fun connectCharacteristics2(gatt: BluetoothGatt?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "request max MTU")
            gatt?.requestMtu(MAX_MTU)?.let { if (!it) onSerialConnectError(IOException("request MTU failed")) }
            // continues asynchronously in onMtuChanged
        } else {
            connectCharacteristics3(gatt)
        }
    }

    override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
        Log.d(TAG, "mtu size " + mtu + ", status=" + status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            payloadSize = mtu - 3
            Log.d(TAG, "payload size " + payloadSize)
        }
        connectCharacteristics3(gatt)
    }

    private fun connectCharacteristics3(gatt: BluetoothGatt?) {
        val writeProperties = writeCharacteristic!!.getProperties()
        if ((writeProperties and (BluetoothGattCharacteristic.PROPERTY_WRITE or  // Microbit,HM10-clone have WRITE
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0
        ) { // HM10,TI uart,Telit have only WRITE_NO_RESPONSE
            onSerialConnectError(IOException("write characteristic not writable"))
            return
        }
        gatt?.setCharacteristicNotification(readCharacteristic, true)?.let {
            if (!it) {
                onSerialConnectError(IOException("no notification for read characteristic"))
                return
            }
        }
        val readDescriptor = readCharacteristic!!.getDescriptor(BLUETOOTH_LE_CCCD)
        if (readDescriptor == null) {
            onSerialConnectError(IOException("no CCCD descriptor for read characteristic"))
            return
        }
        val readProperties = readCharacteristic!!.getProperties()
        if ((readProperties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
            Log.d(TAG, "enable read indication")
            readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
        } else if ((readProperties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
            Log.d(TAG, "enable read notification")
            readDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        } else {
            onSerialConnectError(IOException("no indication/notification for read characteristic (" + readProperties + ")"))
            return
        }
        Log.d(TAG, "writing read characteristic descriptor")
        gatt?.writeDescriptor(readDescriptor)?.let {
            if (!it) {
                onSerialConnectError(IOException("read characteristic CCCD descriptor not writable"))
            }
        }
        // continues asynchronously in onDescriptorWrite()
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor,
        status: Int
    ) {
        delegate!!.onDescriptorWrite(gatt, descriptor, status)
        if (canceled) return
        if (descriptor.getCharacteristic() === readCharacteristic) {
            Log.d(TAG, "writing read characteristic descriptor finished, status=" + status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onSerialConnectError(IOException("write descriptor failed"))
            } else {
                // onCharacteristicChanged with incoming data can happen after writeDescriptor(ENABLE_INDICATION/NOTIFICATION)
                // before confirmed by this method, so receive data can be shown before device is shown as 'Connected'.
                onSerialConnect()
                connected = true
                Log.d(TAG, "connected")
            }
        }
    }

    /*
     * read
     */
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (canceled) return
        delegate!!.onCharacteristicChanged(gatt, characteristic)
        if (canceled) return
        if (characteristic === readCharacteristic) { // NOPMD - test object identity
            val data = readCharacteristic!!.getValue()
            inputStream.onDataReceived(data)
            Log.d(TAG, "read, len=" + data.size)
        }
    }

    /*
     * write
     */
    @Throws(IOException::class)
    fun write(data: ByteArray) {
        if (canceled || !connected || writeCharacteristic == null) throw IOException("not connected")
        var data0: ByteArray?
        synchronized(writeBuffer) {
            if (data.size <= payloadSize) {
                data0 = data
            } else {
                data0 = Arrays.copyOfRange(data, 0, payloadSize)
            }
            if (!writePending && writeBuffer.isEmpty() && delegate!!.canWrite()) {
                writePending = true
            } else {
                writeBuffer.add(data0)
                Log.d(TAG, "write queued")
                data0 = null
            }
            if (data.size > payloadSize) {
                for (i in 1..<(data.size + payloadSize - 1) / payloadSize) {
                    val from = i * payloadSize
                    val to = min(from + payloadSize, data.size)
                    writeBuffer.add(Arrays.copyOfRange(data, from, to))
                    Log.d(TAG, "write queued, len=" + (to - from))
                }
            }
        }
        if (data0 != null) {
            writeCharacteristic!!.setValue(data0)
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                onSerialIoError(IOException("write failed"))
            } else {
                Log.d(TAG, "write started")
            }
        }
        // continues asynchronously in onCharacteristicWrite()
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (canceled || !connected || writeCharacteristic == null) return
        if (status != BluetoothGatt.GATT_SUCCESS) {
            onSerialIoError(IOException("write failed"))
            return
        }
        delegate!!.onCharacteristicWrite(gatt, characteristic, status)
        if (canceled) return
        if (characteristic === writeCharacteristic) { // NOPMD - test object identity
            Log.d(TAG, "write finished, status=" + status)
            writeNext()
        }
    }

    private fun writeNext() {
        val data: ByteArray?
        synchronized(writeBuffer) {
            if (!writeBuffer.isEmpty() && delegate!!.canWrite()) {
                writePending = true
                data = writeBuffer.removeAt(0)
            } else {
                writePending = false
                data = null
            }
        }
        if (data != null) {
            writeCharacteristic!!.setValue(data)
            if (!gatt!!.writeCharacteristic(writeCharacteristic)) {
                onSerialIoError(IOException("write failed"))
            } else {
                Log.d(TAG, "write started, len=" + data.size)
            }
        }
    }

    /**
     * SerialListener
     */
    private fun onSerialConnect() {
        sendState(BaseBluetoothService.STATE_CONNECTED)
        if (mHandler == null) return
        // Send the name of the connected device back to the UI Activity
        val msg: Message = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(MainActivity.DEVICE_NAME, device?.name ?: device?.address ?: "Unknown device")
        msg.setData(bundle)
        mHandler.sendMessage(msg)
    }

    private fun onSerialConnectError(e: Exception?) {
        canceled = true
        makeToast(e?.toString() ?: "Unknown connect error!")
        sendState(BaseBluetoothService.STATE_LISTEN)
    }


    private fun onSerialIoError(e: Exception?) {
        writePending = false
        canceled = true
        makeToast("SERIAL ERR: ${e?.message}")
    }

    /**
     * device delegates
     */
    private inner class ObdService() : DeviceDelegate() {
        override fun connectCharacteristics(gattService: BluetoothGattService?): Boolean {
            Log.d(TAG, "service obd uart")
            readCharacteristic = gattService?.getCharacteristic(BLUETOOTH_LE_OBD)
            writeCharacteristic = gattService?.getCharacteristic(BLUETOOTH_LE_OBD)
            return true
        }
    }


    companion object {
        private val BLUETOOTH_LE_OBD: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        private val BLUETOOTH_LE_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


        private const val MAX_MTU =
            512 // BLE standard does not limit, some BLE 4.2 devices support 251, various source say that Android has max 512
        private const val DEFAULT_MTU = 23
        private const val TAG = "SerialSocket"
    }
}
