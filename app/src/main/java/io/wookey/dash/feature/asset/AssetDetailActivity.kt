package io.wookey.dash.feature.asset

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.core.WalletHelper
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.*
import kotlinx.android.synthetic.main.activity_asset_detail.*

class AssetDetailActivity : BaseTitleSecondActivity() {

    private lateinit var viewModel: AssetDetailViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_detail)

        viewModel = ViewModelProviders.of(this).get(AssetDetailViewModel::class.java)

        val assetId = intent.getIntExtra("assetId", -1)

        if (assetId == -1) {
            finish()
            return
        }

        viewModel.loadAsset(assetId)
        viewModel.loadTransactions(assetId)

//        setRightIcon(R.drawable.icon_switch_node)
//        setRightIconClick(View.OnClickListener {
//            val symbol = viewModel.activeWallet.value?.symbol ?: return@OnClickListener
//            startActivityForResult(Intent(this, NodeListActivity::class.java).apply {
//                putExtra("symbol", symbol)
//                putExtra("canDelete", false)
//            }, REQUEST_SELECT_NODE)
//        })

        addressBg.background = BackgroundHelper.getEditBackground(this, dp2px(3))

        var balance: String? = null

        viewModel.activeAsset.observe(this, Observer { value ->
            value?.let {
                setCenterTitle("Dash")
                icon.setImage(it.token)
                asset.text = it.balance.formatterAmountStrip()
                rate.text = it.balance.formatRate()
                balance = it.balance
            }
        })

        WalletHelper.instance.unSignedBalance.observe(this, Observer { value ->
            value?.let {
                asset.text = it.formatterAmountStrip()
                rate.text = it.formatRate()
                balance = it
            }
        })
        WalletHelper.instance.transactions.observe(this, Observer { value ->
            value?.let {
                viewModel.handleTransactions(it)
            }
        })
        ExchangeRatesHelper.instance.rate.observe(this, Observer {
            balance?.run { rate.text = formatRate() }
        })

        viewModel.activeWallet.observe(this, Observer { value ->
            value?.let {
                address.text = it.address
                asset.text = it.balance.formatterAmountStrip()
                rate.text = it.balance.formatRate()
                balance = it.balance
            }
        })

        AppDatabase.getInstance().walletDao().loadActiveWallet().observe(this, Observer { value ->
            value?.let {
                address.text = it.address
            }
        })

        addressBg.setOnClickListener { copy(address.text.toString()) }

        send.background = BackgroundHelper.getButtonBackground(this)
        receive.background = BackgroundHelper.getButtonBackground(this, R.color.color_AEB6C1, R.color.color_00A761)

        viewModel.sendEnabled.observe(this, Observer { value ->
            value?.let {
                send.isEnabled = it
            }
        })

        viewModel.receiveEnabled.observe(this, Observer { value ->
            value?.let {
                receive.isEnabled = it
            }
        })

        send.setOnClickListener { viewModel.send() }
        viewModel.openSend.observe(this, Observer { openSend(assetId) })

        receive.setOnClickListener { viewModel.receive() }
        viewModel.openReceive.observe(this, Observer { openReceive(assetId) })

        val titles =
            arrayOf(getString(R.string.transfer_all), getString(R.string.receive), getString(R.string.send))
        val allTransfer = TransferFragment()
        val inTransfer = TransferFragment()
        val outTransfer = TransferFragment()
        val fragments = arrayOf(allTransfer, inTransfer, outTransfer)

        viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {

            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount() = titles.size

            override fun getPageTitle(position: Int): CharSequence? {
                return titles[position]
            }
        }
        viewPager.offscreenPageLimit = 2
        tabLayout.setupWithViewPager(viewPager)

//        viewModel.connecting.observe(this, Observer { value ->
//            value?.let {
//                state.text = getString(it)
//                state.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
//            }
//        })

//        viewModel.indeterminate.observe(this, Observer {
//            progress.isIndeterminate = true
//            val lp = progress.layoutParams
//            lp.height = dp2px(16)
//            progress.layoutParams = lp
//        })

        viewModel.synchronizing.observe(this, Observer { value ->
            value?.let {
                state.text = getString(R.string.block_synchronizing, value.toString())
                state.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
            }

        })

        viewModel.synchronizeProgress.observe(this, Observer { value ->
            value?.let {
                progress.isIndeterminate = false
                val lp = progress.layoutParams
                lp.height = dp2px(4)
                progress.layoutParams = lp
                progress.progress = it
            }
        })

        viewModel.synchronized.observe(this, Observer { value ->
            value?.let {
                state.text = getString(it)
                state.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
            }
        })

        viewModel.synchronizeFailed.observe(this, Observer { value ->
            value?.let {
                state.text = getString(it)
                state.setTextColor(ContextCompat.getColor(this, R.color.color_FF3A5C))
                fragments.forEach { fragment ->
                    fragment.synchronizeFailed()
                }
            }
        })

        WalletHelper.instance.connecting.observe(this, Observer {
            if (it == true) {
                state.text = getString(R.string.block_connecting)
                state.setTextColor(ContextCompat.getColor(this, R.color.color_9E9E9E))
            }
        })

        WalletHelper.instance.blocksLeft.observe(this, Observer {
            viewModel.synchronizing.value = it?.toLong()
            if (it == null) {
                viewModel.sendEnabled.value = false
            }
            if (it == 0) {
                viewModel.synchronized.value = R.string.block_synchronized
                viewModel.sendEnabled.value = true
            }
        })

        WalletHelper.instance.progress.observe(this, Observer {
            viewModel.synchronizeProgress.value = it?.toInt() ?: 0
            if (it == null) {
                viewModel.sendEnabled.value = false
            }
            if (it == 100.0) {
                viewModel.synchronized.value = R.string.block_synchronized
                viewModel.sendEnabled.value = true
            } else {
                viewModel.sendEnabled.value = false
            }
        })

        viewModel.allTransfers.observe(this, Observer { value ->
            value?.let {
                allTransfer.notifyDataSetChanged(it)
            }
        })
        viewModel.inTransfers.observe(this, Observer { value ->
            value?.let {
                inTransfer.notifyDataSetChanged(it)
            }
        })
        viewModel.outTransfers.observe(this, Observer { value ->
            value?.let {
                outTransfer.notifyDataSetChanged(it)
            }
        })
    }

    private fun openReceive(assetId: Int) {
        startActivity(Intent(this, ReceiveActivity::class.java).apply {
            putExtra("assetId", assetId)
        })
    }

    private fun openSend(assetId: Int) {
        startActivity(Intent(this, SendActivity::class.java).apply {
            putExtra("assetId", assetId)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleResult(requestCode, resultCode, data)
    }
}