package info.mqtt.android.extsample.components

import android.widget.RelativeLayout
import android.widget.TextView
import android.view.LayoutInflater
import android.annotation.SuppressLint
import android.content.Context
import info.mqtt.android.extsample.R
import android.widget.EditText
import timber.log.Timber
import android.text.InputType
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import java.util.ArrayList

class TextSelectComponent(context: Context, attr: AttributeSet?) : RelativeLayout(context, attr) {
    private val subLabel: TextView
    private val inputTitle: String?
    private val numberInput: Boolean
    private val registeredCallbacks = ArrayList<ITextSelectCallback>()
    private var setText: String?

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.text_select, this)
        val mainLabel = findViewById<TextView>(R.id.mainLabel)
        subLabel = findViewById(R.id.subLabel)
        val textSelectLayout = findViewById<RelativeLayout>(R.id.container)
        val attributeArray = context.obtainStyledAttributes(attr, R.styleable.TextSelectComponent)
        mainLabel.text = attributeArray.getString(R.styleable.TextSelectComponent_main_label)
        subLabel.text = attributeArray.getString(R.styleable.TextSelectComponent_default_value)
        inputTitle = attributeArray.getString(R.styleable.TextSelectComponent_input_title)
        setText = attributeArray.getString(R.styleable.TextSelectComponent_default_value)
        numberInput = attributeArray.getBoolean(R.styleable.TextSelectComponent_numberTxt, false)
        textSelectLayout.setOnClickListener { showInputDialog() }
        attributeArray.recycle()
    }

    private fun showInputDialog() {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams")
        val promptView = layoutInflater.inflate(R.layout.text_input_dialog, null)
        val promptText = promptView.findViewById<TextView>(R.id.textView)
        promptText.text = inputTitle
        val promptEditText = promptView.findViewById<EditText>(R.id.edittext)
        if (numberInput) {
            Timber.i("NUMBER INPUT")
            promptEditText.inputType = InputType.TYPE_CLASS_NUMBER
        } else {
            Timber.i("NOT A NUMBER INPUT")
            promptEditText.inputType = InputType.TYPE_CLASS_TEXT
        }
        Timber.i("Setting text to: $setText")
        Timber.i("input Type: ${promptEditText.inputType}")
        promptEditText.setText(setText)
        val alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setView(promptView)

        // Set up a dialog window
        alertDialogBuilder.setCancelable(false)
            .setPositiveButton("OK") { dialog, id ->
                val text = promptEditText.text.toString()
                subLabel.text = text
                for (callback in registeredCallbacks) {
                    callback.onTextUpdate(text)
                }
            }.setNegativeButton("Cancel") { dialog, id -> dialog.cancel() }
        alertDialogBuilder.setOnKeyListener { dialog, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                dialog.cancel()
            }
            true
        }

        // Create the alert Dialog
        val alert = alertDialogBuilder.create()
        alert.show()
    }

    fun register(callback: ITextSelectCallback) {
        registeredCallbacks.add(callback)
    }

    fun getSetText(): String? {
        return setText
    }

    fun setSetText(setText: String?) {
        this.setText = setText
        subLabel.text = setText
    }

    var setInt: Int
        get() = setText!!.toInt()
        set(value) {
            setText = value.toString()
        }

}
