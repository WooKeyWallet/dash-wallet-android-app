package io.wookey.dash.feature.asset

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.graphics.Bitmap
import cn.bingoogolapple.qrcode.zxing.QRCodeEncoder
import io.wookey.dash.R
import io.wookey.dash.base.BaseViewModel
import io.wookey.dash.core.WalletHelper
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Asset
import io.wookey.dash.data.entity.Wallet
import io.wookey.dash.support.NETWORK_PARAMETERS
import io.wookey.dash.support.REQUEST_SELECT_SUB_ADDRESS
import io.wookey.dash.support.extensions.dp2px
import io.wookey.dash.support.extensions.formatRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bitcoinj.uri.BitcoinURI

class ReceiveViewModel : BaseViewModel() {

    val activeAsset = MutableLiveData<Asset>()
    val activeWallet = MutableLiveData<Wallet>()

    var address: String = ""
    val addressDisplay = MutableLiveData<String>()
    val visibilityIcon = MutableLiveData<Int>()
    var addressVisibility = true

    val QRCodeBitmap = MutableLiveData<Bitmap>()
    val toast = MutableLiveData<Int>()

    var amountSet = false
    val amountShowSet = MutableLiveData<Boolean>()
    val amountShowClear = MutableLiveData<Boolean>()

    val amountDialog = MutableLiveData<Boolean>()

    val amount = MutableLiveData<String>()
    val rate = MutableLiveData<String>()

    init {
        amountShowSet.value = true
    }

    fun setAssetId(assetId: Int) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val asset = AppDatabase.getInstance().assetDao().getAssetById(assetId)
                        ?: throw IllegalStateException()
                    activeAsset.postValue(asset)
                    val wallet = AppDatabase.getInstance().walletDao().getActiveWallet()
                        ?: throw IllegalStateException()
                    activeWallet.postValue(wallet)
                    address = wallet.address
                    addressDisplay.postValue(wallet.address)
                    if (WalletHelper.isAddressValid(wallet.address)) {
                        QRCodeBitmap.postValue(QRCodeEncoder.syncEncodeQRCode(wallet.address, dp2px(115)))
                    } else {
                        QRCodeBitmap.postValue(null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast.postValue(R.string.data_exception)
            }
        }
    }

    fun setAddressVisible() {
        if (address.isBlank()) {
            return
        }
        addressVisibility = !addressVisibility
        if (addressVisibility) {
            addressDisplay.value = address
            visibilityIcon.value = R.drawable.icon_visible_space
        } else {
            visibilityIcon.value = R.drawable.icon_invisible_space
            val str = StringBuilder()
            address.forEach {
                str.append("*")
            }
            addressDisplay.value = str.toString()
        }
    }

    fun generateQRCode(address: String) {
        uiScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    QRCodeBitmap.postValue(QRCodeEncoder.syncEncodeQRCode(address, dp2px(115)))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                QRCodeBitmap.postValue(null)
            }
        }
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_SELECT_SUB_ADDRESS -> {
                val subAddress = data?.getStringExtra("subAddress") ?: return
                address = subAddress
                addressDisplay.value = subAddress
                generateQRCode(subAddress)
            }
        }
    }

    fun amountSet() {
        if (!amountSet) {
            amountDialog.value = true
        } else {
            amountSet = !amountSet
            amountShowSet.value = true
            amount.value = ""
            rate.value = ""
            generateQRCode(address)
        }
    }

    fun setAmount(value: String) {
        amountSet = !amountSet
        if (amountSet) {
            amountShowClear.value = true
        } else {
            amountShowSet.value = true
        }
        amount.value = value
        rate.value = value.formatRate()
        val coin = WalletHelper.instance.formatAmount(value)
        val uri = BitcoinURI.convertToBitcoinURI(NETWORK_PARAMETERS, address, coin, "", null)
        generateQRCode(uri)
    }

}