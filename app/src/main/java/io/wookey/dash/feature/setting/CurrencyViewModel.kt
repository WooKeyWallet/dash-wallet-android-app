package io.wookey.dash.feature.setting

import android.arch.lifecycle.MutableLiveData
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.ExchangeRatesHelper
import io.wookey.dash.support.extensions.putString
import io.wookey.dash.support.extensions.sharedPreferences

class CurrencyViewModel : BaseViewModel() {

    val dataChanged = MutableLiveData<Boolean>()

    fun onItemClick(currency: String) {
        val ratesHelper = ExchangeRatesHelper.instance
        if (ratesHelper.currency == currency) {
            return
        }
        sharedPreferences().putString("currency", currency)
        ratesHelper.currency = currency
        ratesHelper.rate.postValue(Pair(currency, ratesHelper.rates?.optString(currency) ?: ""))
        dataChanged.value = true
    }
}