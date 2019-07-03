package io.wookey.dash.dialog

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import io.wookey.dash.R
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.PointLengthFilter
import io.wookey.dash.support.extensions.*
import kotlinx.android.synthetic.main.dialog_amount_set.*

class AmountSetDialog : DialogFragment() {

    private var cancelListener: (() -> Unit)? = null
    private var confirmListener: ((String) -> Unit)? = null
    private var supportCancel = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.CustomDialog)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_amount_set, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val layoutParams = editContainer.layoutParams
        layoutParams.width = (screenWidth() * 0.85).toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

        editContainer.background = BackgroundHelper.getBackground(context, R.color.color_FFFFFF, dp2px(5))
        symbol.text = "DASH"
        amount.filters = arrayOf(PointLengthFilter(8))
        amount.background = BackgroundHelper.getEditBackground(context)
        currencyType.text = ExchangeRatesHelper.instance.currency
        currency.filters = arrayOf(PointLengthFilter(2))
        currency.background = BackgroundHelper.getEditBackground(context)

        amount.afterTextChanged {
            var value = it
            if (value.startsWith(".")) {
                value = "0$value"
            }
            if (amount.isFocused) {
                currency.setText(value.formatRateWithOutSymbol(""))
            }
        }
        currency.afterTextChanged {
            var value = it
            if (value.startsWith(".")) {
                value = "0$value"
            }
            if (currency.isFocused) {
                amount.setText(value.formatRateReverse(""))
            }
        }

        confirm.background = BackgroundHelper.getButtonBackground(context)

        confirm.setOnClickListener {
            var value = amount.text.toString()
            if (value.isBlank()) {
                toast(R.string.amount_not_set)
                return@setOnClickListener
            }
            if (value.startsWith(".")) {
                value = "0$value"
            } else if (value.startsWith("0")) {
                val indexOf = value.indexOf(".")
                if (indexOf == -1) {
                    toast(R.string.amount_not_set)
                } else {
                    value = value.substring(indexOf - 1, value.length)
                }
            }
            confirmListener?.invoke(value)
            hide()
        }

        if (supportCancel) {
            cancel.visibility = View.VISIBLE
        } else {
            cancel.visibility = View.GONE
        }

        cancel.setOnClickListener {
            cancelListener?.invoke()
            hide()
        }

    }

    private fun hide() {
        val activity = activity
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            amount?.hideKeyboard()
            dismiss()
        }
    }

    companion object {
        private const val TAG = "AmountSetDialog"
        fun newInstance(): AmountSetDialog {
            val fragment = AmountSetDialog()
            return fragment
        }

        fun display(
                fm: FragmentManager,
                supportCancel: Boolean = true,
                cancelListener: (() -> Unit)? = null,
                confirmListener: ((String) -> Unit)?
        ) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            ft.addToBackStack(null)
            newInstance().apply {
                this.supportCancel = supportCancel
                this.cancelListener = cancelListener
                this.confirmListener = confirmListener
            }.show(ft, TAG)
        }
    }
}