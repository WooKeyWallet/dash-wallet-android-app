package io.wookey.dash

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.view.View
import io.wookey.dash.base.BaseActivity
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.feature.asset.AssetFragment
import io.wookey.dash.feature.setting.LanguageActivity
import io.wookey.dash.feature.setting.SettingFragment
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.utils.StatusBarHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    private val TAG_ASSET_FRAGMENT = "tag_asset_fragment"
    private val TAG_SETTING_FRAGMENT = "tag_setting_fragment"

    private var assetFragment: Fragment? = null
    private var settingFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        StatusBarHelper.setStatusBarLightMode(this)

        assetFragment = supportFragmentManager.findFragmentByTag(TAG_ASSET_FRAGMENT)
        settingFragment = supportFragmentManager.findFragmentByTag(TAG_SETTING_FRAGMENT)

        assetIcon.setImageDrawable(BackgroundHelper.getSelectorDrawable(this, R.drawable.icon_asset_unselected, R.drawable.icon_asset_selected))
        settingIcon.setImageDrawable(BackgroundHelper.getSelectorDrawable(this, R.drawable.icon_setting_unselected, R.drawable.icon_setting_selected))

        assetText.setTextColor(BackgroundHelper.getSelectorText(this))
        settingText.setTextColor(BackgroundHelper.getSelectorText(this))

        asset.setOnClickListener {
            switchFragment(0)
        }
        setting.setOnClickListener {
            switchFragment(1)
        }
        if (ActivityStackManager.getInstance().contain(LanguageActivity::class.java)) {
            switchFragment(1)
        } else {
            switchFragment(0)
        }

        ExchangeRatesHelper.instance.stopInterval()
        ExchangeRatesHelper.instance.startInterval()
    }

    override fun onResume() {
        super.onResume()
        if (!ExchangeRatesHelper.instance.isRunning()) {
            ExchangeRatesHelper.instance.stopInterval()
            ExchangeRatesHelper.instance.startInterval()
        }

        if (App.newVersion) {
            dot.visibility = View.VISIBLE
            dot.setImageDrawable(BackgroundHelper.getRedDotDrawable(this))
        } else {
            dot.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ExchangeRatesHelper.instance.stopInterval()
    }

    private fun switchFragment(position: Int) {
        initSelected()
        val transaction = supportFragmentManager.beginTransaction()
        hideAllFragment(transaction)
        when (position) {
            0 -> {
                assetIcon.isSelected = true
                assetText.isSelected = true
                if (assetFragment == null) {
                    assetFragment = AssetFragment()
                    transaction.add(R.id.fragmentContainer, assetFragment!!, TAG_ASSET_FRAGMENT)
                } else {
                    transaction.show(assetFragment!!)
                }
            }
            1 -> {
                settingIcon.isSelected = true
                settingText.isSelected = true
                if (settingFragment == null) {
                    settingFragment = SettingFragment()
                    transaction.add(R.id.fragmentContainer, settingFragment!!, TAG_SETTING_FRAGMENT)
                } else {
                    transaction.show(settingFragment!!)
                }
            }
        }
        transaction.commitAllowingStateLoss()
    }

    private fun initSelected() {
        assetIcon.isSelected = false
        assetText.isSelected = false

        settingIcon.isSelected = false
        settingText.isSelected = false

    }

    private fun hideAllFragment(transaction: FragmentTransaction) {
        assetFragment?.let {
            transaction.hide(it)
        }
        settingFragment?.let {
            transaction.hide(it)
        }
    }
}