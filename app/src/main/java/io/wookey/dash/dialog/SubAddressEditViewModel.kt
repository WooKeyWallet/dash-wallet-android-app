package io.wookey.dash.dialog

import android.arch.lifecycle.MutableLiveData
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.SubAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SubAddressEditViewModel : BaseViewModel() {

    val repository = Repository()
    val success = MutableLiveData<Boolean>()

    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()
    val toastRes = MutableLiveData<Int>()

    fun addOrUpdateSubAddress(walletId: Int, address: SubAddress?, label: String) {
        showLoading.postValue(true)
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val wallet = if (walletId < 0) {
                        AppDatabase.getInstance().walletDao().getActiveWallet()
                    } else {
                        AppDatabase.getInstance().walletDao().getWalletById(walletId)
                    } ?: throw IllegalStateException()
                    if (address == null) {
                        repository.addAddress(wallet, label)
                    } else {
                        AppDatabase.getInstance().subAddressDao().updateSubAddress(address.apply {
                            this.label = label
                        })
                    }
                }
                hideLoading.postValue(true)
                success.postValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
                hideLoading.postValue(true)
                toastRes.postValue(R.string.data_exception)
            }
        }
    }
}