package io.wookey.dash.feature.generate

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import io.wookey.dash.R
import io.wookey.dash.base.BaseActivity
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.WALLET_CREATE
import io.wookey.dash.support.WALLET_RECOVERY
import io.wookey.dash.support.extensions.putInt
import io.wookey.dash.support.extensions.putString
import io.wookey.dash.support.extensions.sharedPreferences
import io.wookey.dash.support.utils.StatusBarHelper
import kotlinx.android.synthetic.main.activity_wallet.*

class WalletActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        StatusBarHelper.translucent(this, ContextCompat.getColor(this, R.color.color_FFFFFF))
        StatusBarHelper.setStatusBarLightMode(this)

        createWallet.background = BackgroundHelper.getButtonBackground(this, R.color.color_00A761)
        recoveryWallet.background = BackgroundHelper.getButtonBackground(this, R.color.color_002C6D)

        sharedPreferences().putString("symbol", "DASH")
        createWallet.setOnClickListener {
            startActivity(Intent(this, GenerateWalletActivity::class.java).apply {
                sharedPreferences().putInt("type", WALLET_CREATE)
            })
        }
        recoveryWallet.setOnClickListener {
            startActivity(Intent(this, GenerateWalletActivity::class.java).apply {
                sharedPreferences().putInt("type", WALLET_RECOVERY)
            })
        }
    }
}
