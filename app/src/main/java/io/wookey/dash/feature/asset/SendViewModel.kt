package io.wookey.dash.feature.asset

import android.app.Activity
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.core.WalletHelper
import io.wookey.dash.data.entity.Asset
import io.wookey.dash.data.entity.Wallet
import io.wookey.dash.support.REQUEST_SCAN_ADDRESS
import io.wookey.dash.support.REQUEST_SELECT_ADDRESS
import io.wookey.dash.support.extensions.formatRate
import io.wookey.dash.support.extensions.formatterAmountStrip
import io.wookey.dash.support.extensions.parseURI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.InsufficientMoneyException

class SendViewModel : BaseViewModel() {

    val receiveAddress = MutableLiveData<String>()
    val receiveAmount = MutableLiveData<String>()
    val feeDisplay = MutableLiveData<String>()
    val enabled = MediatorLiveData<Boolean>()

    val autoFillAddress = MutableLiveData<String>()
    val autoFillAmount = MutableLiveData<String>()

    var activeWallet: Wallet? = null
    var activeAsset: Asset? = null

    val selectAddress = MutableLiveData<String>()
    var isAll = false

    var confirmTransfer = MutableLiveData<Intent>()

    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()
    val toast = MutableLiveData<String>()

    val addressError = MutableLiveData<Boolean>()
    val amountError = MutableLiveData<Boolean>()
    val amountRate = MutableLiveData<String>()

    val repository = Repository()
    var address = ""
    var amount = ""
    var fee = ""
    var defaultError = R.string.data_exception

    init {
        feeDisplay.value = "--"
        enabled.addSource(receiveAddress) {
            enabled.value = checkValid()
        }
        enabled.addSource(receiveAmount) {
            enabled.value = checkValid()
        }
    }

    private fun checkValid(): Boolean {
        if (address.isBlank()) {
            return false
        }
        val error1 = addressError.value
        if (error1 != null && error1) {
            return false
        }
        if (amount.isBlank()) {
            return false
        }
        val error2 = amountError.value
        if (error2 != null && error2) {
            return false
        }
        return true
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        if (requestCode == REQUEST_SCAN_ADDRESS || requestCode == REQUEST_SELECT_ADDRESS) {
            data?.getStringExtra("result")?.let {
                if (it.isNotBlank()) {
                    val uri = it.parseURI()
                    if (uri == null) {
                        address = it
                        autoFillAddress.value = it
                    } else {
                        address = uri.first
                        autoFillAddress.value = uri.first
                        val second = uri.second
                        if (!second.isBlank()) {
                            amount = second
                            autoFillAmount.value = second
                        }
                    }
                }
            }
        }
    }

    fun clickAddressBook() {
        activeWallet?.let {
            selectAddress.value = it.symbol
        }
    }

    fun addressChanged(it: String) {
        if (it.isBlank()) {
            addressError.value = null
        } else {
            addressError.value = !WalletHelper.isAddressValid(it)
        }
        address = it
        receiveAddress.value = it
        calculateFee()
    }

    fun clickAll() {
        activeAsset?.let {
            autoFillAmount.value = it.balance.formatterAmountStrip()
        }
    }

    fun amountChanged(it: String) {
        amountError.value = null
        amount = it
        amountRate.value = it.formatRate()
        receiveAmount.value = it
        isAll = autoFillAmount.value == it
        calculateFee()
    }

    private fun calculateFee() {
        val value = addressError.value
        if (amount.isBlank() || (value != null && value)) {
            feeDisplay.value = "--"
            return
        }
        uiScope.launch {
            try {
                fee = withContext(Dispatchers.IO) {
                    repository.calculateFee(amount, address)
                }
                feeDisplay.postValue(fee)
            } catch (e: org.bitcoinj.wallet.Wallet.DustySendRequested) {
                e.printStackTrace()
                defaultError = R.string.exception_dusty_send
                feeDisplay.postValue("--")
                amountError.postValue(true)
            } catch (e: InsufficientMoneyException) {
                e.printStackTrace()
                defaultError = R.string.exception_insufficient_money
                feeDisplay.postValue("--")
                amountError.postValue(true)
            } catch (e: org.bitcoinj.wallet.Wallet.CouldNotAdjustDownwards) {
                e.printStackTrace()
                defaultError = R.string.exception_insufficient_money
                feeDisplay.postValue("--")
                amountError.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                defaultError = R.string.data_exception
                feeDisplay.postValue("--")
                amountError.postValue(true)
            }
        }
    }

    fun next() {
        confirmTransfer.value = Intent().apply {
            putExtra("token", activeAsset?.token)
            putExtra("address", address)
            putExtra("amount", amount)
            putExtra("fee", fee)
        }
    }

}