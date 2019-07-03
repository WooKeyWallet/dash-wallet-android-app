package io.wookey.dash.feature.asset

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.dialog.PasswordDialog
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.formatRate
import io.wookey.dash.support.extensions.formatterAmountStrip
import io.wookey.dash.support.extensions.setImage
import io.wookey.dash.support.extensions.toast
import kotlinx.android.synthetic.main.activity_confirm_transfer.*

class ConfirmTransferActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_transfer)
        setCenterTitle(R.string.confirm_transfer)

        val token = intent.getStringExtra("token")
        val addressValue = intent.getStringExtra("address")
        val amountValue = intent.getStringExtra("amount")
        val feeValue = intent.getStringExtra("fee") ?: ""

        if (token.isNullOrBlank()
                || addressValue.isNullOrBlank()
                || amountValue.isNullOrBlank()) {
            finish()
            return
        }

        val viewModel = ViewModelProviders.of(this).get(ConfirmTransferViewModel::class.java)

        icon.setImage(token)
        address.text = addressValue
        amount.text = "${amountValue.formatterAmountStrip()} $token"
        rate.text = amountValue.formatRate()
        fee.text = "${feeValue.formatterAmountStrip()}"

        next.background = BackgroundHelper.getButtonBackground(this)

        next.setOnClickListener {
            val id = viewModel.activeWallet?.id ?: return@setOnClickListener
            PasswordDialog.display(supportFragmentManager, id) {
                viewModel.next(addressValue, amountValue, it)
            }
        }

        viewModel.enabled.observe(this, Observer { value ->
            value?.let {
                next.isEnabled = it
            }
        })

        ExchangeRatesHelper.instance.rate.observe(this, Observer {
            rate.text = amountValue.formatRate()
            fee.text = "${feeValue.formatterAmountStrip()}"
        })

        viewModel.toast.observe(this, Observer { toast(it) })
        viewModel.toastInt.observe(this, Observer { toast(it) })
        viewModel.showLoading.observe(this, Observer { showLoading() })
        viewModel.hideLoading.observe(this, Observer { hideLoading() })

    }
}