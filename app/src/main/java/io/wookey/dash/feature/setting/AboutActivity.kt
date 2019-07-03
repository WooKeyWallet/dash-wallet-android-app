package io.wookey.dash.feature.setting

import android.content.Intent
import android.os.Bundle
import android.view.View
import io.wookey.dash.App
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.extensions.openBrowser
import io.wookey.dash.support.extensions.versionName
import kotlinx.android.synthetic.main.activity_about.*

class AboutActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setCenterTitle(R.string.about_us)

        version.setLeftString(getString(R.string.version_placeholder, versionName()))
        version.setOnClickListener {
            openBrowser("https://wallet.wookey.io")
        }

        agreement.setOnClickListener {
            startActivity(Intent(this, WebViewActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (App.newVersion) {
            version.rightTextView.visibility = View.VISIBLE
            version.setRightString(getString(R.string.find_new_version))
            version.rightTextView.compoundDrawablePadding = dp2px(5)
            version.rightTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
                    BackgroundHelper.getRedDotDrawable(this), null)
        } else {
            version.rightTextView.visibility = View.INVISIBLE
        }
    }
}