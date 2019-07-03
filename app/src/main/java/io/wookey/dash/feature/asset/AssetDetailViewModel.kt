package io.wookey.dash.feature.asset

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.core.WalletHelper
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Asset
import io.wookey.dash.data.entity.Node
import io.wookey.dash.data.entity.TransactionInfo
import io.wookey.dash.data.entity.Wallet
import io.wookey.dash.support.REQUEST_SELECT_NODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction

class AssetDetailViewModel : BaseViewModel() {

    private val repository = Repository()

    val activeWallet = MutableLiveData<Wallet>()
    val activeAsset = MutableLiveData<Asset>()

    val synchronizing = MutableLiveData<Long>()
    val synchronizeProgress = MutableLiveData<Int>()
    val synchronizeFailed = MutableLiveData<Int>()
    val synchronized = MutableLiveData<Int>()

    val receiveEnabled = MutableLiveData<Boolean>()
    val sendEnabled = MutableLiveData<Boolean>()

    val allTransfers = MutableLiveData<List<TransactionInfo>>()
    val inTransfers = MutableLiveData<List<TransactionInfo>>()
    val outTransfers = MutableLiveData<List<TransactionInfo>>()

    val openSend = MutableLiveData<Boolean>()
    val openReceive = MutableLiveData<Boolean>()

    init {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (WalletHelper.instance.exception != null) {
                        val activeWallet = AppDatabase.getInstance().walletDao().getActiveWallet()
                            ?: return@withContext
                        repository.openWallet(activeWallet.name)
                    }
                    receiveEnabled.postValue(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                WalletHelper.instance.exception = e
                failed()
            }
        }
    }

    fun loadAsset(assetId: Int) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val asset = AppDatabase.getInstance().assetDao().getAssetById(assetId)
                    activeAsset.postValue(asset)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadTransactions(assetId: Int) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance().transactionInfoDao()
                    val list = dao.getTransactionInfoByAssetId(assetId)
                    convertData(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleTransactions(list: List<Transaction>) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val wallet = WalletHelper.instance.wallet ?: return@withContext
                    val transactions = WalletHelper.convertTransaction(wallet, list)
                    val activeWallet = AppDatabase.getInstance().walletDao().getActiveWallet()
                        ?: return@withContext
                    val assets = AppDatabase.getInstance().assetDao().getAssetsByWalletId(activeWallet.id)
                    if (assets.isNullOrEmpty()) return@withContext
                    val asset = assets[0]
                    transactions.forEach {
                        it.token = asset.token
                        it.assetId = asset.id
                        it.walletId = asset.walletId
                    }
                    convertData(transactions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun convertData(list: List<TransactionInfo>) {
        val allInfo = mutableListOf<TransactionInfo>()
        val inInfo = mutableListOf<TransactionInfo>()
        val outInfo = mutableListOf<TransactionInfo>()
        allInfo.addAll(list)
        allTransfers.postValue(allInfo)
        inInfo.addAll(list.filter { it.direction == 0 })
        inTransfers.postValue(inInfo)
        outInfo.addAll(list.filter { it.direction == 1 })
        outTransfers.postValue(outInfo)
    }

    private fun failed() {
        receiveEnabled.postValue(false)
        sendEnabled.postValue(false)
        synchronizeFailed.postValue(R.string.block_synchronize_failed)
        synchronizeProgress.postValue(0)
    }

    fun send() {
        openSend.value = true
    }

    fun receive() {
        openReceive.value = true
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_SELECT_NODE -> {
                val node = data?.getParcelableExtra<Node>("node") ?: return
            }
        }
    }

}