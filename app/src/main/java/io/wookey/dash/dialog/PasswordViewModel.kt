package io.wookey.dash.dialog

import android.arch.lifecycle.MutableLiveData
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PasswordViewModel : BaseViewModel() {

    private val repository = Repository()

    val verifyPassed = MutableLiveData<String>()
    val verifyFailed = MutableLiveData<Boolean>()

    fun verify(password: String, walletId: Int) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val wallet = AppDatabase.getInstance().walletDao().getWalletById(walletId)
                            ?: throw IllegalStateException()
                    val name = wallet.name
                    val verify = repository.verifyWalletPassword(name, password)
                    if (verify) {
                        verifyPassed.postValue(password)
                    } else {
                        verifyFailed.postValue(true)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    verifyFailed.postValue(true)
                }
            }
        }
    }
}