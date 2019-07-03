package io.wookey.dash.feature.asset

import android.arch.lifecycle.MutableLiveData
import android.os.SystemClock
import io.wookey.dash.ActivityStackManager
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmTransferViewModel : BaseViewModel() {

    val enabled = MutableLiveData<Boolean>()
    var activeWallet: Wallet? = null
    val repository = Repository()

    val toast = MutableLiveData<String>()
    val toastInt = MutableLiveData<Int>()
    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()

    init {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                activeWallet = AppDatabase.getInstance().walletDao().getActiveWallet()
                if (activeWallet == null) {
                    toastInt.postValue(R.string.data_exception)
                    enabled.postValue(false)
                } else {
                    enabled.postValue(true)
                }
            }
        }
    }

    fun next(address: String, amount: String, password: String) {
        enabled.postValue(false)
        showLoading.value = true
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.sendTransaction(amount, address, password)
                    enabled.postValue(true)
                    hideLoading.postValue(true)
                    SystemClock.sleep(300)
                    if (ActivityStackManager.getInstance().contain(AssetDetailActivity::class.java)) {
                        ActivityStackManager.getInstance().finishToActivity(AssetDetailActivity::class.java)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast.postValue(e.message)
                enabled.postValue(true)
                hideLoading.postValue(true)
            }
        }
    }
}