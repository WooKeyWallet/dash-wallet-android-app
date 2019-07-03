package io.wookey.dash.feature.generate.recovery

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.support.extensions.formatterDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.util.*

class RecoveryMnemonicViewModel : BaseViewModel() {

    val enabled = MediatorLiveData<Boolean>()
    val navigation = MutableLiveData<Boolean>()
    val mnemonic = MutableLiveData<String>()
    val transactionDate = MutableLiveData<String>()

    val showLoading = MutableLiveData<Boolean>()
    val hideLoading = MutableLiveData<Boolean>()
    val toast = MutableLiveData<Int>()

    val showDialog = MutableLiveData<Boolean>()

    private val repository = Repository()

    private lateinit var walletName: String
    private lateinit var password: String
    private var passwordPrompt: String? = null

    private var creationTimeSeconds = 0L

    init {
        enabled.addSource(mnemonic) {
            enabled.value = checkValid()
        }
    }

    private fun checkValid(): Boolean {
        val value = mnemonic.value
        if (value.isNullOrBlank()) {
            return false
        }
        return true
    }

    fun initData(walletName: String, password: String, passwordPrompt: String?) {
        this.walletName = walletName
        this.password = password
        this.passwordPrompt = passwordPrompt
    }

    fun setTransactionDate(date: Date) {
        val value = date.formatterDate()
        transactionDate.value = value
        try {
            creationTimeSeconds = date.time / 1000
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun next() {
        enabled.value = false
        if (creationTimeSeconds <= 0L) {
            showDialog.value = true
            enabled.value = true
            return
        }
        create()
    }

    fun create() {
        enabled.value = false
        val value = mnemonic.value
        if (value.isNullOrBlank()) {
            toast.postValue(R.string.exception_mnemonic)
            enabled.postValue(true)
            return
        }
        showLoading.postValue(true)
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.check(value)
                    // 1390095618L 2014-01-19T01:40:18Z 创世区块
                    creationTimeSeconds = Math.max(1390095618L + 86400 * 7 + 1, creationTimeSeconds)
                    val wallet = repository.recoveryWallet(walletName, password, value, creationTimeSeconds)
                    repository.openWallet(walletName)
                    wallet.passwordPrompt = passwordPrompt ?: ""
                    repository.saveWallet(wallet)
                    hideLoading.postValue(true)
                    navigation.postValue(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                repository.deleteWallet(walletName)
                hideLoading.postValue(true)
                toast.postValue(R.string.exception_mnemonic)
            } finally {
                enabled.postValue(true)
            }
        }
    }

}