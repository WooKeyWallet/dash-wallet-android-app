package io.wookey.dash.feature.asset

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.View
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.data.entity.TransactionInfo
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.copy
import io.wookey.dash.support.extensions.formatterAmountStrip
import io.wookey.dash.support.extensions.formatterDate
import io.wookey.dash.support.extensions.openBrowser
import io.wookey.dash.widget.IOSActionSheet
import kotlinx.android.synthetic.main.activity_transaction_detail.*

class TransactionDetailActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        val transaction = intent.getParcelableExtra("transaction")as? TransactionInfo
        if (transaction == null) {
            finish()
            return
        }

        val viewModel = ViewModelProviders.of(this).get(TransactionDetailViewModel::class.java)

        setCenterTitle("${transaction.token} ${getString(R.string.transaction_detail)}")
        divider.background = BackgroundHelper.getDashDrawable(this)

        when {
            transaction.isFailed -> {
                icon.setImageResource(R.drawable.icon_failed)
                status.text = getString(R.string.transfer_failed)
            }
            transaction.isPending -> {
                icon.setImageResource(R.drawable.icon_pending)
                status.text = getString(R.string.pending)
            }
            else -> {
                icon.setImageResource(R.drawable.icon_success)
                status.text = getString(R.string.transfer_success)
            }
        }
        time.text = transaction.timestamp.formatterDate()
        if (transaction.direction == 1) {
            direction.text = getString(R.string.send)

        } else {
            direction.text = getString(R.string.receive)
        }
        amount.text = "${transaction.amount?.formatterAmountStrip() ?: "--"}"
        fee.text = "${transaction.fee?.formatterAmountStrip() ?: "--"}"
        txId.text = transaction.hash ?: "--"

        if (transaction.address.isNullOrBlank()) {
            addressRow.visibility = View.GONE
        } else {
            addressRow.visibility = View.VISIBLE
        }

        addressTitle.text = getString(R.string.received_address)
        address.text = transaction.address

        txId.setOnClickListener { copy(txId.text.toString()) }

        viewModel.setTxId(transaction.hash)

        blockExplorer.setOnClickListener {
            IOSActionSheet.Builder(this)
                .styleId(R.style.IOSActionSheetStyleCustom)
                .otherButtonTitlesSimple("https://explorer.dash.org/tx/", "http://insight.dash.org/insight/tx/")
                .itemClickListener { actionSheet, itemPosition, itemModel ->
                    openBrowser("${itemModel.itemTitle}${transaction.hash}")
                }
                .show()

        }

        viewModel.QRCodeBitmap.observe(this, Observer { value ->
            value?.let {
                QRCode.setImageBitmap(it)
            }
        })
    }
}