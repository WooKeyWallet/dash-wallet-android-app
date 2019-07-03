package io.wookey.dash.feature.wallet

import android.arch.lifecycle.MutableLiveData
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.SubAddress
import io.wookey.dash.data.entity.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddressSettingViewModel : BaseViewModel() {

    private val repository = Repository()

    var walletId = -1
    var currentAddress: String? = null
    var wallet: Wallet? = null

    val updateAddress = MutableLiveData<SubAddress>()
    val subAddresses = MutableLiveData<List<SubAddress>>()
    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()
    val toast = MutableLiveData<String>()
    val toastRes = MutableLiveData<Int>()

    val copy = MutableLiveData<String>()

    val dataChanged = MutableLiveData<String>()


    fun loadSubAddresses() {
        showLoading.postValue(true)
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (walletId < 0) {
                        loadActiveWallet()
                    } else {
                        loadWalletById()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast.postValue(e.message)
            } finally {
                hideLoading.postValue(true)
            }
        }
    }

    private fun loadActiveWallet() {
        val wallet = AppDatabase.getInstance().walletDao().getActiveWallet()
        if (wallet == null) {
            toastRes.postValue(R.string.data_exception)
            return
        }
        this.wallet = wallet
        walletId = wallet.id
        currentAddress = wallet.address

        subAddresses.postValue(repository.getAddresses(wallet))
    }

    private fun loadWalletById() {
        val wallet = AppDatabase.getInstance().walletDao().getWalletById(walletId)
        if (wallet == null) {
            toastRes.postValue(R.string.data_exception)
            return
        }
        this.wallet = wallet
        currentAddress = wallet.address
        subAddresses.postValue(repository.getAddresses(wallet))
    }

    fun refreshSubAddresses() {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    subAddresses.postValue(repository.getAddresses(wallet!!))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onAddressClick(subAddress: SubAddress) {
        copy.value = subAddress.address
    }

    fun onItemClick(subAddress: SubAddress) {
        if (walletId < 0) {
            toastRes.postValue(R.string.data_exception)
            return
        }
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val walletDao = AppDatabase.getInstance().walletDao()
                    val wallet = walletDao.getWalletById(walletId)
                    if (wallet == null) {
                        toastRes.postValue(R.string.data_exception)
                    } else {
                        walletDao.updateWallets(wallet.apply { address = subAddress.address })
                        currentAddress = subAddress.address
                        dataChanged.postValue(currentAddress)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast.postValue(e.message)
            }
        }
    }

    fun onLabelClick(subAddress: SubAddress) {
        updateAddress.value = subAddress
    }
}