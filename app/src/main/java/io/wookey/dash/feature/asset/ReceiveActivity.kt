package io.wookey.dash.feature.asset

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.dialog.AmountSetDialog
import io.wookey.dash.feature.wallet.AddressSettingActivity
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.copy
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.extensions.toast
import kotlinx.android.synthetic.main.activity_receive.*

class ReceiveActivity : BaseTitleSecondActivity() {

    lateinit var viewModel: ReceiveViewModel
    var assetId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)

        assetId = intent.getIntExtra("assetId", -1)

        if (assetId == -1) {
            finish()
            return
        }
        viewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
        viewModel.setAssetId(assetId)

        divider.background = BackgroundHelper.getDashDrawable(this)
        addressBg.background = BackgroundHelper.getBackground(this, R.color.color_F3F4F6, dp2px(5))

        viewModel.activeAsset.observe(this, Observer { value ->
            value?.let {
                setCenterTitle("Dash ${getString(R.string.account_receive)}")
                prompt.text = getString(R.string.receive_prompt, it.token)
            }
        })

        viewModel.addressDisplay.observe(this, Observer { value ->
            address.text = value ?: ""
        })

        viewModel.visibilityIcon.observe(this, Observer { value ->
            value?.let {
                visible.setImageResource(it)
            }
        })

        visible.setOnClickListener {
            viewModel.setAddressVisible()
        }

        viewModel.QRCodeBitmap.observe(this, Observer {
            QRCode.setImageBitmap(it)
        })

        viewModel.toast.observe(this, Observer { value ->
            value?.let {
                toast(it)
            }
        })

        viewModel.amountShowSet.observe(this, Observer {
            amountSet.setText(R.string.amount_set)
            amount.visibility = View.GONE
            rate.visibility = View.GONE
        })

        viewModel.amountShowClear.observe(this, Observer {
            amountSet.setText(R.string.amount_clear)
            amount.visibility = View.VISIBLE
            rate.visibility = View.VISIBLE
        })

        amountSet.setOnClickListener { viewModel.amountSet() }

        viewModel.amountDialog.observe(this, Observer {
            AmountSetDialog.display(supportFragmentManager) {
                viewModel.setAmount(it)
            }
        })

        viewModel.amount.observe(this, Observer { value ->
            value?.let {
                amount.text = "$it DASH"
            }
        })
        viewModel.rate.observe(this, Observer { value ->
            value?.let {
                rate.text = it
            }
        })

        switchAddress.setOnClickListener {
            startActivity(Intent(this, AddressSettingActivity::class.java))
        }

        copyAddress.setOnClickListener { copy(address.text.toString()) }

    }

    override fun hide(): Boolean {
        return false
    }

    override fun onResume() {
        super.onResume()
        viewModel.setAssetId(assetId)
    }
}