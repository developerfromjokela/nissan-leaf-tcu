package com.developerfromjokela.nissanleaftelematics.config

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.developerfromjokela.nissanleaftelematics.R
import com.developerfromjokela.nissanleaftelematics.utils.VinChecksum
import com.developerfromjokela.nissanleaftelematics.utils.DtcUtils
import com.google.android.material.materialswitch.MaterialSwitch

fun Boolean.toInt() = if (this) 1 else 0

class TCUConfigAdapter(
    private val configItems: List<TCUConfigItem>,
    private val onReadClick: (TCUConfigItem) -> Unit,
    private val onWriteClick: (TCUConfigItem, ByteArray) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val COLLATION_ITM_LENGTH = 20
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fieldName: TextView = itemView.findViewById(R.id.fieldName)
        val valueField: TextInputEditText = itemView.findViewById(R.id.valueField)
        val readBtn: Button = itemView.findViewById(R.id.readBtn)
        val writeBtn: Button = itemView.findViewById(R.id.writeBtn)
    }

    class FICOSA_CollationViewHolder(itemView: View) : ViewHolder(itemView) {
        val naviId: TextInputEditText = itemView.findViewById(R.id.naviId)
        val tcuId: TextInputEditText = itemView.findViewById(R.id.tcuId)
        val simId: TextInputEditText = itemView.findViewById(R.id.simId)
        val msn: TextInputEditText = itemView.findViewById(R.id.msn)
    }

    class FICOSA_ServicesViewHolder(itemView: View) : ViewHolder(itemView) {
        val svtb: MaterialSwitch = itemView.findViewById(R.id.svtb)
        val rlu: MaterialSwitch = itemView.findViewById(R.id.rlu)
        val horn: MaterialSwitch = itemView.findViewById(R.id.horn)
        val remotestart: MaterialSwitch = itemView.findViewById(R.id.remotestart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 6) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.config_item_ficosa_svc, parent, false) // Replace with your actual layout file name
            return FICOSA_ServicesViewHolder(view)
        }
        if (viewType == 4) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.config_item_collation, parent, false) // Replace with your actual layout file name
            return FICOSA_CollationViewHolder(view)
        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.config_item, parent, false) // Replace with your actual layout file name
        return ViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return configItems[position].type
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onBindViewHolder(rvHolder: RecyclerView.ViewHolder, position: Int) {
        val configItem = configItems[position]
        val holder = rvHolder as ViewHolder

        // Set field name from resource ID
        holder.fieldName.text = holder.itemView.context.getString(configItem.uiName)

        holder.readBtn.setOnClickListener {
            onReadClick(configItem)
        }

        if (configItem.type == 6) {
            val svcHolder = rvHolder as FICOSA_ServicesViewHolder
            val validReadValue = configItem.currentReadValue != null && configItem.currentReadValue!!.isNotEmpty()
            svcHolder.svtb.isEnabled = validReadValue
            svcHolder.rlu.isEnabled = validReadValue
            svcHolder.horn.isEnabled = validReadValue
            svcHolder.remotestart.isEnabled = validReadValue

            if (validReadValue) {
                svcHolder.svtb.isChecked = configItem.currentReadValue!![8] > 0
                svcHolder.rlu.isChecked = configItem.currentReadValue!![9] > 0
                svcHolder.horn.isChecked = configItem.currentReadValue!![10] > 0
                svcHolder.remotestart.isChecked = configItem.currentReadValue!![11] > 0
            } else {
                svcHolder.svtb.isChecked = false
                svcHolder.rlu.isChecked = false
                svcHolder.horn.isChecked = false
                svcHolder.remotestart.isChecked = false
            }


            holder.writeBtn.apply {
                isEnabled = validReadValue
                visibility = if (configItem.readOnly) View.GONE else View.VISIBLE
                setOnClickListener {
                    val dataBuff = configItem.currentReadValue!!.copyOf()

                    dataBuff[8] = svcHolder.svtb.isChecked.toInt().toByte()
                    dataBuff[9] = svcHolder.rlu.isChecked.toInt().toByte()
                    dataBuff[10] = svcHolder.horn.isChecked.toInt().toByte()
                    dataBuff[11] = svcHolder.remotestart.isChecked.toInt().toByte()

                    onWriteClick(configItem, dataBuff)
                }
            }
        } else if (configItem.type == 4) {
            val apnHolder = rvHolder as FICOSA_CollationViewHolder
            apnHolder.writeBtn.isEnabled = false

            if (configItem.currentReadValue != null && configItem.currentReadValue!!.size == 100) {
                apnHolder.valueField.setText(String((configItem.currentReadValue!!).copyOf( COLLATION_ITM_LENGTH)).trim { it <= ' ' })
                apnHolder.naviId.setText(String((configItem.currentReadValue!!).copyOfRange(COLLATION_ITM_LENGTH, COLLATION_ITM_LENGTH*2)).trim { it <= ' ' })
                apnHolder.tcuId.setText(String((configItem.currentReadValue!!).copyOfRange(COLLATION_ITM_LENGTH*2, COLLATION_ITM_LENGTH*3)).trim { it <= ' ' })
                apnHolder.simId.setText(String((configItem.currentReadValue!!).copyOfRange(COLLATION_ITM_LENGTH*3, COLLATION_ITM_LENGTH*4)).trim { it <= ' ' })
                apnHolder.msn.setText(String((configItem.currentReadValue!!).copyOfRange(COLLATION_ITM_LENGTH*4, COLLATION_ITM_LENGTH*5)).trim { it <= ' ' })
            } else {
                apnHolder.valueField.setText("")
                apnHolder.naviId.setText("")
                apnHolder.tcuId.setText("")
                apnHolder.simId.setText("")
                apnHolder.msn.setText("")
            }
        } else {

            // Configure value field
            holder.valueField.apply {
                isEnabled = (!configItem.readOnly && configItem.type != 7 && configItem.type != 8)
                maxEms = configItem.fieldMaxLength.takeIf { it > 0 } ?: Int.MAX_VALUE
            }
            holder.valueField.setTextIsSelectable(true)
            holder.valueField.setText("")
            holder.writeBtn.setText(when (configItem.type) {
                7 -> R.string.erase_dtc
                8 -> R.string.run
                else -> {R.string.write}
            })
            holder.readBtn.setText(when (configItem.type) {
                8 -> R.string.refresh
                else -> {R.string.read}
            })

            if (configItem.currentReadValue != null) {
                if (configItem.type == 0) {
                    val vinBytes = configItem.currentReadValue!!.copyOfRange(0, configItem.currentReadValue!!.size-2)
                    val vin = String(vinBytes)
                    val crc = configItem.currentReadValue!!.copyOfRange(configItem.currentReadValue!!.size-2, configItem.currentReadValue!!.size)
                    val calcCRC = VinChecksum().calculateBytesBigEndian(vinBytes)
                    holder.fieldName.text = "${holder.itemView.context.getString(configItem.uiName)}, CRC: ${crc.toHexString()}, expected: ${calcCRC.toHexString()}"
                    holder.valueField.setText(vin)
                } else if (configItem.type == 1) {
                    holder.valueField.setText(String((configItem.currentReadValue!!).copyOfRange(1, configItem.currentReadValue!!.size-1)).trim { it <= ' ' })
                } else if (configItem.type == 2) {
                    holder.valueField.setText(configItem.currentReadValue!![0].toInt().toString())
                } else if (configItem.type == 7) {
                    val report = DtcUtils.generateReport(configItem.currentReadValue!!)
                    holder.valueField.setText(report)
                } else if (configItem.type == 8) {
                    val statusByte = configItem.currentReadValue!![0].toInt()
                    holder.valueField.setText(when (statusByte) {
                        3 -> "FAILED!"
                        1 -> "Running..."
                        2 -> {
                            if (configItem.currentReadValue!!.size > 1) {
                                when (configItem.configId) {
                                    4 -> {
                                        val simStatus = when (configItem.currentReadValue!![1].toInt()) {
                                            1 -> "SIM Removed"
                                            3 -> "SIM Initialized"
                                            4, 7 -> "SIM PIN Error"
                                            else -> "Unknown SIM Status"
                                        }
                                        val rawData = String(configItem.currentReadValue!!.copyOfRange(2, configItem.currentReadValue!!.size-2))
                                        "Status: $simStatus\nICCID: $rawData"
                                    }
                                    else -> {
                                        String(configItem.currentReadValue!!.copyOfRange(1, configItem.currentReadValue!!.size-1))
                                    }
                                }
                            } else {
                                "Success!"
                            }
                        }
                        else -> "Unknown (${statusByte})"
                    })
                } else {
                    val telAntLevel = configItem.currentReadValue!![0].toInt()
                    val receptionPower = configItem.currentReadValue!![1].toInt()
                    val errorRate = configItem.currentReadValue!![2].toInt()
                    holder.valueField.setText(
                        "ANT:${telAntLevel},RECEPTION:${receptionPower},ERRRATE:${errorRate}",
                    )
                }
            } else {
                if (configItem.type == 0) {
                    holder.fieldName.text = holder.itemView.context.getString(configItem.uiName)
                }
            }

            // Configure buttons visibility and click listeners

            holder.writeBtn.apply {
                isEnabled = !configItem.readOnly
                visibility = if (configItem.readOnly) View.GONE else View.VISIBLE
                setOnClickListener {
                    val value = holder.valueField.text.toString()
                    val dataBuff = ByteArray(configItem.fieldMaxLength)
                    if (configItem.type == 2) {
                        dataBuff[0] = value.toInt().toByte()
                    } else {
                        value.toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff)
                    }
                    onWriteClick(configItem, dataBuff)
                }
            }
        }



    }

    override fun getItemCount(): Int = configItems.size

    override fun getItemId(pos: Int): Long = pos.toLong()
}
