package io.wookey.dash.feature.generate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.data.entity.MnemonicLang
import io.wookey.dash.support.BackgroundHelper
import io.wookey.dash.support.CURRENT_MNEMONIC_LANGUAGE
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.mnemonicLangList
import io.wookey.dash.widget.DividerItemDecoration
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_mnemonic_language.*
import kotlinx.android.synthetic.main.item_selector.*

class MnemonicLanguageActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mnemonic_language)
        setCenterTitle(R.string.select_mnemonic_language)

        dot.setImageDrawable(BackgroundHelper.getDotDrawable(this, R.color.color_FFFFFF, dp2px(6)))

        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = MnemonicLanguageAdapter(mnemonicLangList) {
            CURRENT_MNEMONIC_LANGUAGE = it
            setResult(Activity.RESULT_OK, Intent().apply { putExtra("lang", it) })
            finish()
        }
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration().apply {
            setOrientation(DividerItemDecoration.VERTICAL)
            setMarginStart(dp2px(25))
        })
    }

    class MnemonicLanguageAdapter(val data: List<MnemonicLang>, val listener: (MnemonicLang) -> Unit) : RecyclerView.Adapter<MnemonicLanguageAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selector, parent, false)
            return ViewHolder(view, listener)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindViewHolder(data[position])
        }

        class ViewHolder(override val containerView: View, val listener: (MnemonicLang) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
            fun bindViewHolder(mnemonicLang: MnemonicLang) {
                content.text = mnemonicLang.lang
                if (CURRENT_MNEMONIC_LANGUAGE.lang == mnemonicLang.lang) {
                    selected.setImageResource(R.drawable.icon_selected)
                } else {
                    selected.setImageResource(R.drawable.icon_unselected)
                }
                itemView.setOnClickListener { listener(mnemonicLang) }
            }
        }
    }
}