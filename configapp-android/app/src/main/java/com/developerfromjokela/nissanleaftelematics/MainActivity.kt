package com.developerfromjokela.nissanleaftelematics

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.app.ProgressDialog.STYLE_HORIZONTAL
import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.nissanleaftelematics.bluetooth.BluetoothService
import com.developerfromjokela.nissanleaftelematics.bluetooth.DeviceSelectActivity
import com.developerfromjokela.nissanleaftelematics.config.TCUConfigAdapter
import com.developerfromjokela.nissanleaftelematics.config.TCUConfigItem
import com.developerfromjokela.nissanleaftelematics.diag.CanPayloadMaker
import com.developerfromjokela.nissanleaftelematics.diag.CanPayloadParser
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pnuema.android.obd.commands.OBDCommand
import com.pnuema.android.obd.models.PID


class MainActivity : AppCompatActivity() {
    companion object {
        const val MESSAGE_STATE_CHANGE = 1
        const val MESSAGE_RESULT = 3
        const val MESSAGE_DEVICE_NAME = 4
        const val DEVICE_NAME = "device_name"
        const val TOAST = "toast"
        const val MESSAGE_TOAST = 5
        const val MESSAGE_RESULT_MULTI = 6
        const val INIT_MSG_1 = 10
        const val ADV_DIAG = "0210C0"
        const val MODULE_RESET = 11
        const val MODULE_ATE1 = 12
        const val MODULE_ATS0 = 13
        const val MODULE_ATH0 = 14
        const val MODULE_ATL0 = 15
        const val MODULE_ATCAF0 = 16
        const val MODULE_TXID = 746
        const val MODULE_RXID = 783
        const val MODULE_ATSH = 17
        const val MODULE_ATCRA = 18
        const val DATAREQ = 996
        const val MODULE_INIT_FINISH = 998
        const val CONN_INIT_FINISH = 997
        const val NORESP = 999

        const val DATAWRITE_OPERATION = 1002
    }

    private var mConnectedDeviceName: String? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null

    // Member object for the chat services
    private var mChatService: BluetoothService? = null

    private var connected = false
    private var dataIntegrity = true
    private var elmInit = false

    private var currentWriteOperationTotalMsgCount = 0

    private lateinit var connectBtn: Button
    private lateinit var selectedDevNameTxt: TextView
    private lateinit var tcuConfigRV: RecyclerView
    private var configItems = mutableListOf(
        TCUConfigItem(configId = 0x81, fieldLength = 17, type = 0, fieldMaxLength = 17, readOnly = false, uiName = R.string.vin),
        TCUConfigItem(configId = 0x10, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_dial),
        TCUConfigItem(configId = 0x11, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_user),
        TCUConfigItem(configId = 0x12, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_pass),
        TCUConfigItem(configId = 0x13, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_name),
        TCUConfigItem(configId = 0x14, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.dns1),
        TCUConfigItem(configId = 0x15, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.dns2),
        TCUConfigItem(configId = 0x16, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.proxy),
        TCUConfigItem(configId = 0x17, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.proxy_port),
        TCUConfigItem(configId = 0x18, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.apn_connection_type),
        TCUConfigItem(configId = 0x19, fieldLength = 128, type = 1, fieldMaxLength = 128, readOnly = false, uiName = R.string.server_hostname),
    )
    private var tcuConfAdapter = TCUConfigAdapter(configItems, {i -> this.onReadClick(i)}, {tcuConfigItem, s ->  this.onWriteClick(tcuConfigItem, s)})

    private var progressDialog: ProgressDialog? = null

    private lateinit var wl: WakeLock
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "nissanleaftelematics:MainActivity")
        wl.acquire(10*60*1000L /*10 minutes*/)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                if (menuItem.itemId == R.id.dataIntegrityCheck) {
                    menuItem.isChecked = !menuItem.isChecked
                    dataIntegrity = menuItem.isChecked
                    Log.e("MA", "Dataintegrity: $dataIntegrity")
                }
                return true
            }


        })

        // UI

        connectBtn = findViewById(R.id.connectBtn)
        selectedDevNameTxt = findViewById(R.id.deviceName)
        tcuConfigRV = findViewById(R.id.tcuConfigRV)
        tcuConfigRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        tcuConfigRV.adapter = tcuConfAdapter
        initUIListeners()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED|| ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 12)
            return
        }
        initBT()
    }

    private fun initBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        if (mChatService != null) {
            if (mChatService!!.state == BluetoothService.STATE_NONE) {
                mChatService!!.start()
            }
        }

        if (!mBluetoothAdapter!!.isEnabled()) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, 3)
        } else {
            if (mChatService == null) setupChat()
        }
    }


    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_STATE_CHANGE -> {
                    when (msg.arg1) {
                        BluetoothService.STATE_CONNECTED -> {
                            connected = true;
                            connectBtn.isEnabled = true
                            connectBtn.setText(R.string.disconnect)
                            selectedDevNameTxt.text = mConnectedDeviceName
                            onConnected()
                        }

                        BluetoothService.STATE_CONNECTING -> {
                            connectBtn.isEnabled = false
                        }

                        BluetoothService.STATE_LISTEN, BluetoothService.STATE_NONE -> {
                            connected = false
                            connectBtn.isEnabled = true
                            selectedDevNameTxt.setText(R.string.no_device_selected)
                        }
                    }
                }

                MESSAGE_RESULT -> commandResult(msg.obj.toString(), msg.arg1)
                MESSAGE_RESULT_MULTI -> {
                    println("MSGResult Multi, ${msg.arg1} ${msg.obj}")
                    if (msg.arg1 == DATAWRITE_OPERATION && progressDialog?.isShowing == true) {
                        runOnUiThread {
                            if (msg.arg2 == -1) {
                                progressDialog?.dismiss()
                                Toast.makeText(applicationContext, R.string.done_writing, Toast.LENGTH_LONG).show()
                                return@runOnUiThread
                            }
                            progressDialog?.progress = (msg.arg2/currentWriteOperationTotalMsgCount)*100
                        }
                    }
                }
                MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Settings.Global.DEVICE_NAME)
                    selectedDevNameTxt.text = mConnectedDeviceName
                    Toast.makeText(
                        applicationContext, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT
                    ).show()
                }

                MESSAGE_TOAST -> Toast.makeText(
                    applicationContext, msg.getData().getString(TOAST),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupChat() {
        mChatService = BluetoothService(this, mHandler)
        connectBtn.isEnabled = true
    }

    private fun onConnected() {
        val initPid = PID()
        val MODE_AT = "AT"

        initPid.mode = MODE_AT
        initPid.PID = "S0"
        val cmd = OBDCommand(initPid).setIgnoreResult(true)
        mChatService?.makeOBDCommand(cmd, INIT_MSG_1)
    }

    private fun commandResult(msg: String, id: Int) {
        Log.e("MainActivity", "ID:$id, result:$msg");
        val initPid = PID()
        val MODE_AT = "AT"
        when (id) {
            INIT_MSG_1 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "Z"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_RESET)
            }
            MODULE_RESET -> {
                initPid.mode = MODE_AT
                initPid.PID = "S0"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATS0)
            }
            MODULE_ATS0 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "E1"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATE1)
            }
            MODULE_ATE1 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "L0"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATL0)
            }
            MODULE_ATL0 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "H0"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATH0)
            }
            MODULE_ATH0 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "AL"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATCAF0)
            }
            MODULE_ATCAF0 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = MODE_AT
                initPid.PID = "CAF0"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_INIT_FINISH)
            }
            MODULE_INIT_FINISH -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT SH"
                initPid.PID = MODULE_TXID.toString()
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATSH)
            }
            MODULE_ATSH -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT CRA"
                initPid.PID = MODULE_RXID.toString()
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATCRA)
            }
            MODULE_ATCRA -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT FC SH"
                initPid.PID = MODULE_TXID.toString()
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATCRA+1)
            }
            MODULE_ATCRA+1 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT FC SD 30 00 00"
                initPid.PID = ""
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATCRA+2)
            }
            MODULE_ATCRA+2 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT FC SM"
                initPid.PID = "1"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, MODULE_ATCRA+3)
            }
            MODULE_ATCRA+3 -> {
                if (!msg.contains("OK")) {
                    Toast.makeText(this, "COMM INIT FAIL; STEP $id", Toast.LENGTH_SHORT).show()
                    return
                }
                initPid.mode = "$MODE_AT SP"
                initPid.PID = "6"
                val cmd = OBDCommand(initPid).setIgnoreResult(true)
                mChatService?.makeOBDCommand(cmd, CONN_INIT_FINISH)
            }
            CONN_INIT_FINISH -> {
                elmInit = true;
                Toast.makeText(this, R.string.connection_init_finish, Toast.LENGTH_SHORT).show()
            }
            DATAREQ -> {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                try {
                    val parser = CanPayloadParser()
                    val packet1 = parser.parse(msg, skipIntegrityCheck = !dataIntegrity)
                    if (packet1.data != null) {
                        when (packet1.data[0].toInt()) {
                            97 -> {
                                // DATA from TCU
                                handleReadFieldData(packet1.data[1].toUByte(), (packet1.data).copyOfRange(2, packet1.data.size-2))
                            }
                            else -> {
                                Toast.makeText(this, "Unknown message type: ${packet1.data[0].toInt()}", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, R.string.data_empty, Toast.LENGTH_LONG).show();
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun handleReadFieldData(fieldId: UByte, data: ByteArray) {
        Log.e("MA", "FIELDID: $fieldId, DATALEN:${data.size}")
        configItems.find { it.configId.toUByte() == fieldId }?.let {
            it.currentReadValue = data
        }
        tcuConfAdapter.notifyItemChanged(configItems.indexOfFirst { it.configId.toUByte() == fieldId })
    }

    private fun initUIListeners() {
        connectBtn.setOnClickListener {
            if (!connected) {
                val serverIntent = Intent(this, DeviceSelectActivity::class.java)
                startActivityForResult(serverIntent, 2)
            } else {
                mChatService?.stop()
                connectBtn.setText(R.string.connect)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            2 ->             // When DeviceListActivity returns with a device to connect
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        connectDevice(data)
                    }
                }

            3 -> if (resultCode == RESULT_OK) {
                if (mChatService == null) setupChat()
            } else {
                Toast.makeText(this, "BT not enabled", Toast.LENGTH_SHORT).show()
                if (mChatService == null) setupChat()
            }
        }
    }

    private fun connectDevice(data: Intent) {
        // Get the device MAC address
        val address = data.extras?.getString(DeviceSelectActivity.EXTRA_DEVICE_ADDRESS)
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        // Attempt to connect to the device
        mChatService!!.connect(device)
    }

    private fun stringHexToOBDCommand(hex: String): OBDCommand {
        val pid = PID()
        pid.mode = hex
        pid.PID = ""
        return OBDCommand(pid)
    }

    private fun onReadClick(item: TCUConfigItem) {
        Log.e("MA", "0221"+("%02x".format(item.configId).uppercase()))
        if (!connected) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }
        // Set diag mode
        val diagPid = PID()
        diagPid.mode = ADV_DIAG
        diagPid.PID = ""
        mChatService?.makeOBDCommand(OBDCommand(diagPid).setIgnoreResult(true), NORESP)

        // Read value
        val readPid = PID()
        readPid.mode = "0221"+("%02x".format(item.configId).uppercase())
        readPid.PID = ""
        mChatService?.makeOBDCommand(OBDCommand(readPid), DATAREQ)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun onWriteClick(item: TCUConfigItem, newVal: String, skipEmptyData: Boolean = false) {
        if (!connected) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }
        val payloadMaker = CanPayloadMaker()
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle(R.string.writing_data)
        progressDialog!!.setMessage(getString(R.string.writing_data_desc))
        progressDialog!!.setProgressStyle(STYLE_HORIZONTAL)

        if (newVal.trim().isEmpty() && !skipEmptyData) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.empty_data)
                .setMessage(R.string.empty_data_warn)
                .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    onWriteClick(item, newVal, true);
                }
                .setNegativeButton(android.R.string.cancel) { d: DialogInterface, _: Int ->
                    d.dismiss()
                }.show()
            return
        }

        if (item.type == 0) {
            // VIN write
            if (newVal.length > 17) {
                Toast.makeText(this, R.string.vin_too_long, Toast.LENGTH_SHORT).show()
                return
            }
            progressDialog!!.show()
            val vinInfo = newVal.toByteArray(charset = Charsets.US_ASCII)
            val vinInfoBuff = ByteArray(17)
            vinInfo.copyInto(vinInfoBuff)
            val hexCMD = "3B81"+vinInfoBuff.toHexString(HexFormat.Default).uppercase()+"0000"
            println("CAN $hexCMD")
            val payloadParts = payloadMaker.processCommandToFrames(hexCMD)
            currentWriteOperationTotalMsgCount = payloadParts.size
            mChatService?.makeOBDMultiCommand(payloadParts.map {
                stringHexToOBDCommand(it)
            }, DATAWRITE_OPERATION)
            return
        } else if (item.type == 1) {
            // data item write
            if (newVal.length > 128) {
                Toast.makeText(this, R.string.data_too_long, Toast.LENGTH_SHORT).show()
                return
            }
            progressDialog!!.show()
            val dataVal = newVal.toByteArray(charset = Charsets.US_ASCII)
            val dataValBuff = ByteArray(128)
            dataVal.copyInto(dataValBuff)
            val hexCMD = "3B"+("%02x".format(item.configId).uppercase())+"01"+dataValBuff.toHexString(HexFormat.Default).uppercase()
            println("CAN $hexCMD")
            val payloadParts = payloadMaker.processCommandToFrames(hexCMD)
            currentWriteOperationTotalMsgCount = payloadParts.size
            mChatService?.makeOBDMultiCommand(payloadParts.map {
                stringHexToOBDCommand(it)
            }, DATAWRITE_OPERATION)
            return
        }
        Toast.makeText(this, "NOT IMPLEMENTED", Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 12 && grantResults.isNotEmpty()) {
            initBT()
        } else {
            Toast.makeText(this, R.string.permissions_not_given, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}