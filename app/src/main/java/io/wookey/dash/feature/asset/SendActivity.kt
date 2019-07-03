package io.wookey.dash.feature.asset

import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.feature.address.AddressBookActivity
import io.wookey.dash.feature.address.ScanActivity
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.REQUEST_SCAN_ADDRESS
import io.wookey.dash.support.REQUEST_SELECT_ADDRESS
import io.wookey.dash.support.extensions.*
import kotlinx.android.synthetic.main.activity_send.*

class SendActivity : BaseTitleSecondActivity() {

    private lateinit var viewModel: SendViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        val assetId = intent.getIntExtra("assetId", -1)
        if (assetId == -1) {
            finish()
            return
        }

        viewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)

        setRightIcon(R.drawable.icon_scan)
        setRightIconClick(View.OnClickListener { scanAddress() })

        addressBook.setOnClickListener { viewModel.clickAddressBook() }
        all.setOnClickListener { viewModel.clickAll() }


        address.editText?.afterTextChanged { viewModel.addressChanged(it) }
        amount.editText?.afterTextChanged { viewModel.amountChanged(it) }

        viewModel.autoFillAddress.observe(this, Observer { value ->
            value?.let {
                address.editText?.setText(it)
                address.editText?.setSelection(it.length)
            }
        })
        viewModel.autoFillAmount.observe(this, Observer { value ->
            value?.let {
                amount.editText?.setText(it)
                amount.editText?.setSelection(it.length)
            }
        })

        viewModel.feeDisplay.observe(this, Observer {
            val s = it?.formatterAmountStrip(defaultStr = "--") ?: "--"
            fee.text = getString(R.string.transaction_fee_placeholder, "$s DASH")
        })

        next.background = BackgroundHelper.getButtonBackground(this)
        next.setOnClickListener {
            viewModel.next()
        }
        viewModel.enabled.observe(this, Observer { value ->
            value?.let {
                next.isEnabled = it
            }
        })

        viewModel.selectAddress.observe(this, Observer { value ->
            value?.let {
                selectAddress(it)
            }
        })

        viewModel.confirmTransfer.observe(this, Observer { value ->
            value?.let {
                confirmTransfer(it)
            }
        })

        viewModel.showLoading.observe(this, Observer {
            showLoading()
        })

        viewModel.hideLoading.observe(this, Observer {
            hideLoading()
        })

        viewModel.toast.observe(this, Observer { toast(it) })

        viewModel.addressError.observe(this, Observer {
            if (it != null && it) {
                address.error = getString(R.string.address_invalid)
            } else {
                address.error = null
            }
        })
        viewModel.amountError.observe(this, Observer {
            if (it != null && it) {
                amountHint.text = getString(viewModel.defaultError)
                amountHint.setTextColor(ContextCompat.getColor(this, R.color.color_FF3A5C))
            } else {
                amountHint.text = viewModel.amountRate.value ?: ""
                amountHint.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
            }
        })
        viewModel.amountRate.observe(this, Observer { value ->
            value?.let {
                amountHint.text = it
                amountHint.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
            }
        })

        AppDatabase.getInstance().walletDao().loadActiveWallet().observe(this, Observer { value ->
            value?.let {
                walletName.text = it.name
                viewModel.activeWallet = it
            }
        })

        var assetBalance: String? = null

        AppDatabase.getInstance().assetDao().loadAssetById(assetId).observe(this, Observer { value ->
            value?.let {
                setCenterTitle("Dash ${getString(R.string.account_send)}")
                icon.setImage(it.token)
                balance.text = "${it.balance.formatterAmountStrip()} ${it.token}"
                rate.text = it.balance.formatRate()
                assetBalance = it.balance
                viewModel.activeAsset = it
            }
        })

        ExchangeRatesHelper.instance.rate.observe(this, Observer {
            assetBalance?.run { rate.text = formatRate() }
            val error = viewModel.amountError.value
            if (error != null && error) {
                return@Observer
            }
            val value = viewModel.amountRate.value
            if (value.isNullOrBlank()) {
                return@Observer
            }
            amountHint.text = value
            amountHint.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleResult(requestCode, resultCode, data)
    }

    private fun scanAddress() {
        startActivityForResult(Intent(this, ScanActivity::class.java), REQUEST_SCAN_ADDRESS)
    }

    private fun selectAddress(symbol: String) {
        startActivityForResult(Intent(this, AddressBookActivity::class.java).apply {
            putExtra("symbol", symbol)
        }, REQUEST_SELECT_ADDRESS)
    }

    private fun confirmTransfer(intent: Intent) {
        startActivity(intent.apply {
            setClass(this@SendActivity, ConfirmTransferActivity::class.java)
        })
    }
}