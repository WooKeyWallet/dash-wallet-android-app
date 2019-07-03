package io.wookey.dash.feature.asset

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.wookey.dash.R
import io.wookey.dash.base.BaseFragment
import io.wookey.dash.data.entity.TransactionInfo
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.extensions.formatterAmountStrip
import io.wookey.dash.support.extensions.formatterDate
import io.wookey.dash.widget.DividerItemDecoration
import io.wookey.dash.widget.StatusAdapterWrapper
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_transfet.*
import kotlinx.android.synthetic.main.item_transaction.*
import java.math.BigDecimal

class TransferFragment : BaseFragment() {

    lateinit var token: String
    val list = mutableListOf<TransactionInfo>()
    val adapter = TransferAdapter(list) {
        startActivity(Intent(context, TransactionDetailActivity::class.java).apply {
            putExtra("transaction", it)
        })
    }
    val wrapper = object : StatusAdapterWrapper(adapter) {
        override fun getEmptyImageResource(): Int {
            return R.drawable.icon_no_transaction
        }

        override fun getEmptyStringResource(): Int {
            return R.string.no_transaction
        }

        override fun getTopMargin(): Int {
            return dp2px(60)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_transfet, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(context)

        wrapper.setLoadingViewNone()
        recyclerView.adapter = wrapper
        wrapper.setEmptyView()

        recyclerView.addItemDecoration(DividerItemDecoration().apply {
            setOrientation(DividerItemDecoration.VERTICAL)
            setMarginStart(dp2px(25))
        })
    }

    fun notifyDataSetChanged(transfers: List<TransactionInfo>) {
        list.clear()
        list.addAll(transfers)
        adapter.notifyDataSetChanged()
        wrapper.setSuccessView()
    }

    fun synchronizeFailed() {
        if (list.isNullOrEmpty()) {
            wrapper.setErrorView()
        }
    }

    class TransferAdapter(val data: List<TransactionInfo>, private val listener: (TransactionInfo) -> Unit) :
            RecyclerView.Adapter<TransferAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            return ViewHolder(view, listener)
        }

        override fun getItemCount() = data.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bindViewHolder(data[position])
        }

        class ViewHolder(override val containerView: View, private val listener: (TransactionInfo) -> Unit) :
                RecyclerView.ViewHolder(containerView), LayoutContainer {
            fun bindViewHolder(info: TransactionInfo) {
                val prefix: String
                var value = ""
                if (info.direction == 1) {
                    icon.setImageResource(R.drawable.icon_send)
                    direction.text = direction.context.getString(R.string.send)
                    prefix = "-"
                    try {
                        value = BigDecimal(info.amount).add(BigDecimal(info.fee)).toPlainString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    amount.text = "$prefix${value.formatterAmountStrip()} ${info.token}"
                } else {
                    icon.setImageResource(R.drawable.icon_receive)
                    direction.text = direction.context.getString(R.string.receive)
                    prefix = "+"
                    amount.text = "$prefix${info.amount?.formatterAmountStrip() ?: "--"} ${info.token}"
                }

                time.text = info.timestamp.formatterDate()

                when {
                    info.isFailed -> {
                        state.text = state.context.getString(R.string.failed)
                        state.setTextColor(ContextCompat.getColor(state.context, R.color.color_FF3A5C))
                    }
                    info.isPending -> {
                        state.text = state.context.getString(R.string.pending)
                        state.setTextColor(ContextCompat.getColor(state.context, R.color.color_2179FF))
                    }
                    else -> {
                        state.text = state.context.getString(R.string.success)
                        state.setTextColor(ContextCompat.getColor(state.context, R.color.color_00A761))
                    }
                }

                itemView.setOnClickListener { listener(info) }
            }
        }
    }
}