package com.developerfromjokela.nissanleaftelematics.config

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
    private val onWriteClick: (TCUConfigItem, String) -> Unit
) : RecyclerView.Adapter<TCUConfigAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fieldName: TextView = itemView.findViewById(R.id.fieldName)
        val valueField: TextInputEditText = itemView.findViewById(R.id.valueField)
        val readBtn: Button = itemView.findViewById(R.id.readBtn)
        val writeBtn: Button = itemView.findViewById(R.id.writeBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.config_item, parent, false) // Replace with your actual layout file name
        return ViewHolder(view)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val configItem = configItems[position]

        // Set field name from resource ID
        holder.fieldName.text = holder.itemView.context.getString(configItem.uiName)

        // Configure value field
        holder.valueField.apply {
            isEnabled = !configItem.readOnly
            maxEms = configItem.fieldMaxLength.takeIf { it > 0 } ?: Int.MAX_VALUE
        }

        if (configItem.currentReadValue != null) {
            if (configItem.type == 0) {
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
        holder.readBtn.setOnClickListener {
            onReadClick(configItem)
        }

        holder.writeBtn.apply {
            isEnabled = !configItem.readOnly
            visibility = if (configItem.readOnly) View.GONE else View.VISIBLE
            setOnClickListener {
                val value = holder.valueField.text.toString()
                onWriteClick(configItem, value)
            }
        }
    }

    override fun getItemCount(): Int = configItems.size
}
