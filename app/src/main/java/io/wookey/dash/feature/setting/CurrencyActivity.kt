package io.wookey.dash.feature.setting

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.wookey.dash.R
import io.wookey.dash.base.BaseTitleSecondActivity
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.extensions.formatMoney
import io.wookey.dash.widget.DividerItemDecoration
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_currency.*
import kotlinx.android.synthetic.main.item_selector.*

class CurrencyActivity : BaseTitleSecondActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency)
        setCenterTitle(R.string.currency)

        val viewModel = ViewModelProviders.of(this).get(CurrencyViewModel::class.java)

        rate.text = ExchangeRatesHelper.instance.getRate().formatMoney()

        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        val list = listOf("CNY", "USD")
        val adapter = CurrencyAdapter(list, viewModel)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration().apply {
            setOrientation(DividerItemDecoration.VERTICAL)
            setMarginStart(dp2px(25))
        })

        ExchangeRatesHelper.instance.rate.observe(this, Observer { value ->
            value?.let {
                rate.text = "1 DASH ${it.formatMoney()}"
            }
        })

        viewModel.dataChanged.observe(this, Observer {
            adapter.notifyDataSetChanged()
        })
    }

    class CurrencyAdapter(val data: List<String>, val viewModel: CurrencyViewModel) : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selector, parent, false)
            return ViewHolder(view, viewModel)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindViewHolder(data[position])
        }

        class ViewHolder(override val containerView: View, val viewModel: CurrencyViewModel) : RecyclerView.ViewHolder(containerView), LayoutContainer {
            fun bindViewHolder(currency: String) {
                content.text = currency
                if (ExchangeRatesHelper.instance.currency == currency) {
                    selected.setImageResource(R.drawable.icon_selected)
                } else {
                    selected.setImageResource(R.drawable.icon_unselected)
                }
                itemView.setOnClickListener {
                    viewModel.onItemClick(currency)
                }
            }
        }
    }
}