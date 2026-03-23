package com.developerfromjokela.nissanleaftelematics.config

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.developerfromjokela.nissanleaftelematics.R

class TCUConfigAdapter(
    private val configItems: List<TCUConfigItem>,
    private val onReadClick: (TCUConfigItem) -> Unit,
    private val onWriteClick: (TCUConfigItem, ByteArray) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private val APN_FIELD_SIZE = 32
    }

    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fieldName: TextView = itemView.findViewById(R.id.fieldName)
        val valueField: TextInputEditText = itemView.findViewById(R.id.valueField)
        val readBtn: Button = itemView.findViewById(R.id.readBtn)
        val writeBtn: Button = itemView.findViewById(R.id.writeBtn)
    }

    class FICOSA_APNViewHolder(itemView: View) : ViewHolder(itemView) {
        val password: TextInputEditText = itemView.findViewById(R.id.password)
        val user: TextInputEditText = itemView.findViewById(R.id.user)
        val dns1: TextInputEditText = itemView.findViewById(R.id.dns1)
        val dns2: TextInputEditText = itemView.findViewById(R.id.dns2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 4) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.config_item_ficosa_apn, parent, false) // Replace with your actual layout file name
            return FICOSA_APNViewHolder(view)
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

        if (configItem.type == 4) {
            val apnHolder = rvHolder as FICOSA_APNViewHolder

            if (configItem.currentReadValue != null) {
                apnHolder.valueField.setText(String((configItem.currentReadValue!!).copyOf( APN_FIELD_SIZE)).trim { it <= ' ' })
                apnHolder.user.setText(String((configItem.currentReadValue!!).copyOfRange(APN_FIELD_SIZE, APN_FIELD_SIZE*2)).trim { it <= ' ' })
                apnHolder.password.setText(String((configItem.currentReadValue!!).copyOfRange(APN_FIELD_SIZE*2, APN_FIELD_SIZE*3)).trim { it <= ' ' })
                apnHolder.dns1.setText(String((configItem.currentReadValue!!).copyOfRange(APN_FIELD_SIZE*3, APN_FIELD_SIZE*4)).trim { it <= ' ' })
                apnHolder.dns2.setText(String((configItem.currentReadValue!!).copyOfRange(APN_FIELD_SIZE*4, APN_FIELD_SIZE*5)).trim { it <= ' ' })
            }

            holder.writeBtn.apply {
                isEnabled = !configItem.readOnly
                visibility = if (configItem.readOnly) View.GONE else View.VISIBLE
                setOnClickListener {
                    val dataBuff = ByteArray(configItem.fieldMaxLength) {0x20}

                    apnHolder.valueField.text.toString().toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff, 0)
                    apnHolder.user.text.toString().toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff, APN_FIELD_SIZE)
                    apnHolder.password.text.toString().toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff, APN_FIELD_SIZE * 2)
                    apnHolder.dns1.text.toString().toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff, APN_FIELD_SIZE * 3)
                    apnHolder.dns2.text.toString().toByteArray(charset = Charsets.US_ASCII).copyInto(dataBuff, APN_FIELD_SIZE * 4)
                    Log.e("AAA", dataBuff.toHexString())
                    //onWriteClick(configItem, dataBuff)
                }
            }
        } else {

            // Configure value field
            holder.valueField.apply {
                isEnabled = !configItem.readOnly
                maxEms = configItem.fieldMaxLength.takeIf { it > 0 } ?: Int.MAX_VALUE
            }

            if (configItem.currentReadValue != null) {
                if (configItem.type == 0 || configItem.type == 5) {
                    holder.valueField.setText(String(configItem.currentReadValue!!))
                } else if (configItem.type == 1) {
                    holder.valueField.setText(String((configItem.currentReadValue!!).copyOfRange(1, configItem.currentReadValue!!.size-1)).trim { it <= ' ' })
                } else if (configItem.type == 2) {
                    holder.valueField.setText(configItem.currentReadValue!![0].toInt().toString())
                } else {
                    val telAntLevel = configItem.currentReadValue!![0].toInt()
                    val receptionPower = configItem.currentReadValue!![1].toInt()
                    val errorRate = configItem.currentReadValue!![2].toInt()
                    holder.valueField.setText(
                        "ANT:${telAntLevel},RECEPTION:${receptionPower},ERRRATE:${errorRate}",
                    )
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
}
