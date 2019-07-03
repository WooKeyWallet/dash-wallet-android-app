package io.wookey.dash.feature.generate.recovery

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import io.wookey.dash.ActivityStackManager
import io.wookey.dash.MainActivity
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.feature.wallet.WalletManagerActivity
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.*
import io.wookey.dash.widget.IOSDialog
import kotlinx.android.synthetic.main.activity_recovery_wallet.*

class RecoveryWalletActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_recovery_wallet)
        setCenterTitle(R.string.recovery_mnemonic)

        val walletName = intent.getStringExtra("walletName")
        val password = intent.getStringExtra("password")
        val passwordPrompt = intent.getStringExtra("passwordPrompt")

        val viewModel = ViewModelProviders.of(this).get(RecoveryMnemonicViewModel::class.java)
        viewModel.initData(walletName, password, passwordPrompt)

        mnemonic.background = BackgroundHelper.getEditBackground(this)
        next.background = BackgroundHelper.getButtonBackground(this)

        mnemonic.afterTextChanged {
            viewModel.mnemonic.value = it
        }

        dateContainer.showTimePicker {
            viewModel.setTransactionDate(it)
        }

        viewModel.transactionDate.observe(this, Observer { value ->
            value?.let {
                transactionDate.editText?.setText(it)
            }
        })

        next.setOnClickListener {
            viewModel.next()
        }

        viewModel.enabled.observe(this, Observer { value ->
            value?.let {
                next.isEnabled = it
            }
        })

        viewModel.navigation.observe(this, Observer {
            navigation()
        })

        viewModel.showLoading.observe(this, Observer { showLoading() })
        viewModel.hideLoading.observe(this, Observer { hideLoading() })
        viewModel.toast.observe(this, Observer { toast(it) })

        viewModel.showDialog.observe(this, Observer {
            IOSDialog(this)
                    .radius(dp2px(5))
                    .titleText("")
                    .contentText(getString(R.string.dialog_block_height_content))
                    .contentTextSize(16)
                    .contentTextBold(true)
                    .leftText(getString(R.string.dialog_block_height_cancel))
                    .rightText(getString(R.string.dialog_block_height_confirm))
                    .setIOSDialogLeftListener { viewModel.create() }
                    .cancelAble(true)
                    .layout()
                    .show()
        })
    }

    private fun navigation() {
        if (ActivityStackManager.getInstance().contain(WalletManagerActivity::class.java)) {
            ActivityStackManager.getInstance().finishToActivity(WalletManagerActivity::class.java)
        } else {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }
    }
}
