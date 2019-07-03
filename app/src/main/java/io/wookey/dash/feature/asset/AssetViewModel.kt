package io.wookey.dash.feature.asset

import android.content.Intent
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.Repository
import io.wookey.dash.core.WalletHelper
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Asset
import io.wookey.dash.support.extensions.putBoolean
import io.wookey.dash.support.extensions.sharedPreferences
import io.wookey.dash.support.viewmodel.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.core.Transaction

class AssetViewModel : BaseViewModel() {

    val openAssetDetail = SingleLiveEvent<Intent>()

    private val repository = Repository()

    val assetVisible = SingleLiveEvent<Unit>()
    val assetInvisible = SingleLiveEvent<Unit>()

    private var asset: Asset? = null

    init {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val activeWallet = AppDatabase.getInstance().walletDao().getActiveWallet()
                            ?: return@withContext
                    val walletName = activeWallet.name
                    if (walletName != WalletHelper.instance.walletName || WalletHelper.instance.exception != null) {
                        repository.openWallet(walletName)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                WalletHelper.instance.exception = e
            }
        }
    }

    fun initVisible() {
        val visible = sharedPreferences().getBoolean("assetVisible", true)
        if (visible) {
            assetVisible.call()
        } else {
            assetInvisible.call()
        }
    }

    fun onItemClick(value: Asset) {
        asset = value
        next()
    }

    fun next() {
        if (asset != null) {
            openAssetDetail.value = Intent().apply {
                putExtra("assetId", asset!!.id)
            }
            asset = null
        }
    }

    fun assetVisibleChanged() {
        val visible = sharedPreferences().getBoolean("assetVisible", true)
        sharedPreferences().putBoolean("assetVisible", !visible)
        if (!visible) {
            assetVisible.call()
        } else {
            assetInvisible.call()
        }
    }

    fun updateBalance(value: String) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val walletDao = AppDatabase.getInstance().walletDao()
                    val activeWallet = walletDao.getActiveWallet()
                            ?: return@withContext
                    walletDao.updateWallets(activeWallet.apply { balance = value })
                    val assetDao = AppDatabase.getInstance().assetDao()
                    val assets = assetDao.getAssetsByWalletId(activeWallet.id)
                    val filter = assets.filter { it.token == "DASH" }
                    if (filter.size == 1) {
                        assetDao.updateAsset(filter[0].apply {
                            balance = value
                        })
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun initData() {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val wallet = WalletHelper.instance.wallet ?: return@withContext
                    WalletHelper.instance.getBalance(wallet)
                    WalletHelper.instance.getTransactions(wallet)
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
                    val infos = AppDatabase.getInstance().transactionInfoDao().getTransactionInfoByAssetId(asset.id)
                    if (infos.isNotEmpty()) {
                        AppDatabase.getInstance().transactionInfoDao().deleteTransactionInfo(*infos.toTypedArray())
                    }
                    AppDatabase.getInstance().transactionInfoDao().insertTransactionInfo(*transactions.toTypedArray())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}