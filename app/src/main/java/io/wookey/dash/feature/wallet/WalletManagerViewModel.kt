package io.wookey.dash.feature.wallet

import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletManagerViewModel : BaseViewModel() {

    val switchWallet = MutableLiveData<Boolean>()
    val walletDetail = MutableLiveData<Intent>()

    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()
    val toast = MutableLiveData<String>()

    private val repository = Repository()

    fun activeWallet(wallet: Wallet) {
        showLoading.postValue(true)
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val walletDao = AppDatabase.getInstance().walletDao()
                    val activeWallet = walletDao.getActiveWallet()
                    repository.openWallet(wallet.name)
                    if (activeWallet != null) {
                        walletDao.updateWallets(activeWallet.apply { isActive = false }, wallet.apply { isActive = true })
                    } else {
                        walletDao.updateWallets(wallet.apply { isActive = true })
                    }
                    delay(300)
                    hideLoading.postValue(true)
                    switchWallet.postValue(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast.postValue(e.message)
                hideLoading.postValue(true)
            }
        }
    }

    fun onItemClick(wallet: Wallet) {
        walletDetail.value = Intent().apply {
            putExtra("walletId", wallet.id)
        }
    }
}