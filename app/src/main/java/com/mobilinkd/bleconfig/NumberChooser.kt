package com.mobilinkd.bleconfig

import android.content.BroadcastReceiver
import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton

class NumberChooser(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {

    private var _value: Int = 0
    private var _minimum: Int = 0
    private var _maximum: Int = 255
    private var incrementButton: MaterialButton
    private var decrementButton: MaterialButton
    private var textView: TextView
    private var _listener: NumberChooserListener? = null

    init {
        if (D) Log.d(TAG, "init")
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater;
        inflater.inflate(R.layout.number_chooser, this, true)
        if (D) Log.d(TAG, "inflated")
        incrementButton = findViewById(R.id.imageButtonUp)
        decrementButton = findViewById(R.id.imageButtonDown)
        textView = findViewById(R.id.number)
        val a = context.obtainStyledAttributes(attrs, R.styleable.NumberChooser)
        if (a.hasValue(R.styleable.NumberChooser_chooserMinimum)) {
            _minimum = a.getInt(R.styleable.NumberChooser_chooserMinimum, _minimum)
        }
        if (a.hasValue(R.styleable.NumberChooser_chooserMaximum)) {
            _maximum = a.getInt(R.styleable.NumberChooser_chooserMaximum, _maximum)
        }
        if (a.hasValue(R.styleable.NumberChooser_chooserValue)) {
            _value = a.getInt(R.styleable.NumberChooser_chooserValue, _value)
        }
        a.recycle()
        if (D) Log.d(TAG, "min = $_minimum, max = $_maximum, value = $_value")
        textView.inputType = InputType.TYPE_CLASS_NUMBER
        textView.text = _value.toString()
        incrementButton.setOnClickListener { onIncrement() }
        decrementButton.setOnClickListener { onDecrement() }
        textView.doAfterTextChanged {
            var v = 0
            try {
                if (textView.text.isNotEmpty()) {
                    v = Integer.parseInt(textView.text.toString())
                }
                value = v
                if (_value != v) {
                    textView.text = _value.toString()
                }
            } catch (_: NumberFormatException) {
                textView.text = _value.toString()
            }
        }

        textView.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) { // Lost focus
                val tmp = _value.toString()
                if (textView.text.toString() != tmp) textView.text = tmp
                _listener?.onValueChanged(this@NumberChooser)
            }
        }
    }

    override fun getBaseline(): Int {
        return textView.getBaseline()
    }

    var value: Int
        get() = _value
        set(value) {
            if (value < _minimum) {
                _value = _minimum
            } else if (value > _maximum) {
                _value = _maximum
            } else {
                _value = value
            }
        }

    var minimumValue: Int
        get() = _minimum
        set(value) {
            _minimum = value
            if (_value < _minimum) {
                _value = _minimum
                textView.text = _value.toString()
            }

        }

    var maximumValue: Int
        get() = _maximum
        set(value) {
            _maximum = value
            if (_value > _maximum) {
                _value = _maximum
                textView.text = _value.toString()
            }
        }

    var listener: NumberChooserListener?
        get() = _listener
        set(value) {
            _listener = value
        }

    // parent activity will implement this method to respond to click events
    abstract class NumberChooserListener {
        abstract fun onValueChanged(view: View)
    }

    fun setOnValueChangedListener(callback: () -> Unit) {
        _listener = object : NumberChooserListener()  {
            override fun onValueChanged(view: View) { callback() }
        }
    }

    private fun onIncrement() {
        if (!decrementButton.isEnabled) {
            decrementButton.isEnabled = true
        }

        if (_value < _maximum) {
            _value += 1
            textView.text = _value.toString()
        }

        if (_value >= _maximum) {
            incrementButton.isEnabled = false
        }
    }


    private fun onDecrement() {
        if (!incrementButton.isEnabled) {
            incrementButton.isEnabled = true
        }

        if (_value > _minimum) {
            _value -= 1
            textView.text = _value.toString()
        }

        if (_value <= _minimum) {
            decrementButton.isEnabled = false
        }
    }

    companion object {
        private val TAG = NumberChooser::class.java.name
        private const val D = true
    }
}