package com.cvmmk

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatSpinner
import java.util.*

class MultiSpinner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatSpinner(context, attrs, defStyle) {

    private var items: List<Worker> = emptyList()
    private var selected: BooleanArray = booleanArrayOf()
    private var listener: OnMultiChoiceListener? = null
    private var defaultText: String = context.getString(R.string.select_workers)

    fun setItems(items: List<Worker>, listener: OnMultiChoiceListener) {
        this.items = items
        this.selected = BooleanArray(items.size)
        this.listener = listener

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listOf(defaultText))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        this.adapter = adapter
    }

    override fun performClick(): Boolean {
        showMultiChoiceDialog()
        return true // Indicate the click was handled
    }

    private fun showMultiChoiceDialog() {
        val names = items.map { it.name }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle(R.string.select_workers)
            .setMultiChoiceItems(names, selected, DialogInterface.OnMultiChoiceClickListener { _, which, isChecked ->
                selected[which] = isChecked
            })
            .setPositiveButton(android.R.string.ok, DialogInterface.OnClickListener { _, _ ->
                listener?.onItemsSelected(items.filterIndexed { index, _ -> selected[index] })
                updateSpinnerText()
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateSpinnerText() {
        val selectedItems = items.filterIndexed { index, _ -> selected[index] }.map { it.name }
        val displayText = if (selectedItems.isEmpty()) defaultText else selectedItems.joinToString(", ")
        (adapter as ArrayAdapter<String>).clear()
        (adapter as ArrayAdapter<String>).add(displayText)
        (adapter as ArrayAdapter<String>).notifyDataSetChanged()
    }

    interface OnMultiChoiceListener {
        fun onItemsSelected(selectedItems: List<Worker>)
    }
}