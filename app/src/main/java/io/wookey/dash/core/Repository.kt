package io.wookey.dash.core

import android.app.Application
import io.wookey.dash.App
import io.wookey.dash.data.AppDatabase
import io.wookey.dash.data.entity.Asset
import io.wookey.dash.data.entity.SubAddress
import io.wookey.dash.data.entity.Wallet
import io.wookey.dash.support.CURRENT_MNEMONIC_LANGUAGE
import io.wookey.dash.support.NETWORK_PARAMETERS
import org.bitcoinj.core.Context
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

class Repository(val context: Application = App.instance) {

    val path = "${context.filesDir.absolutePath}${File.separator}wallet${File.separator}dash"

    fun getWalletPath(name: String): String {
        return "$path${File.separator}$name"
    }

    fun getWalletFilePath(name: String): String {
        return "$path${File.separator}$name${File.separator}$name"
    }

    private fun generateWalletFile(name: String): File {
        val dir = getWalletPath(name)
        val walletFolder = File(dir)
        if (!walletFolder.exists()) {
            walletFolder.mkdirs()
        }
        val cacheFile = File(dir, name)
        if (cacheFile.exists()) {
            throw RuntimeException("Some wallet files already exist for $dir")
        }
        return cacheFile
    }

    fun createWallet(walletName: String, password: String): Wallet {
        WalletHelper.instance.closeWallet()
        val walletFile = generateWalletFile(walletName)
        val wordStream = context.assets.open(CURRENT_MNEMONIC_LANGUAGE.fileName)
        MnemonicCode.INSTANCE = MnemonicCode(wordStream, null)

        val wallet = WalletHelper.instance.createWallet(walletFile, password)
        return wallet.apply {
            symbol = "DASH"
            name = walletName
        }
    }

    fun check(words: String) {
        val wordStream = context.assets.open(CURRENT_MNEMONIC_LANGUAGE.fileName)
        MnemonicCode.INSTANCE = MnemonicCode(wordStream, null)
        MnemonicCodeHelper.check(words.split(" "), MnemonicCode.INSTANCE.wordList)
    }

    fun recoveryWallet(walletName: String, password: String, mnemonic: String, creationTimeSeconds: Long): Wallet {
        WalletHelper.instance.closeWallet()
        val walletFile = generateWalletFile(walletName)
        // creationTimeSeconds: Long = 1427610960L
        val wallet = WalletHelper.instance.recoveryWallet(walletFile, password, mnemonic, creationTimeSeconds)
        return wallet.apply {
            symbol = "DASH"
            name = walletName
        }
    }

    fun saveWallet(wallet: Wallet): Wallet? {
        val database = AppDatabase.getInstance()

        val actWallets = database.walletDao().getActiveWallets()
        if (!actWallets.isNullOrEmpty()) {
            actWallets.forEach {
                it.isActive = false
            }
            database.walletDao().updateWallets(*actWallets.toTypedArray())
        }
        wallet.isActive = true
        database.walletDao().insertWallet(wallet)
        val insert = database.walletDao().getWalletsByName(wallet.symbol, wallet.name)
        if (insert != null) {
            database.assetDao().insertAsset(Asset(walletId = insert.id, token = insert.symbol))
        }
        return insert
    }

    fun deleteWallet(name: String): Boolean {
        var success = false
        val dir = getWalletPath(name)
        val walletFolder = File(dir)
        if (walletFolder.exists() && walletFolder.isDirectory) {
            success = walletFolder.deleteRecursively()
        }
        return success
    }

    fun verifyWalletPassword(walletName: String, password: String): Boolean {
        val wallet = getWallet(walletName)
        return wallet.checkPassword(password)
    }

    fun getMnemonic(walletName: String, password: String): List<String> {
        val wallet = getWallet(walletName)
        return WalletHelper.instance.getMnemonic(wallet, password)
    }

    fun getWallet(walletName: String): org.bitcoinj.wallet.Wallet {
        Context.propagate(Context(NETWORK_PARAMETERS))
        val walletFile = File(getWalletFilePath(walletName))
        return WalletHelper.instance.getWallet(walletFile)
    }

    fun getAddresses(wallet: Wallet): List<SubAddress> {
        Context.propagate(Context(NETWORK_PARAMETERS))
        val walletFile = File(getWalletFilePath(wallet.name))
        val realWallet = WalletHelper.instance.getWallet(walletFile)
        if (realWallet.issuedReceiveAddresses.isNullOrEmpty()) {
            realWallet.freshReceiveAddress()
            realWallet.saveToFile(walletFile)
        }
        val addresses = realWallet.issuedReceiveAddresses
                ?: emptyList()
        val list = mutableListOf<SubAddress>()
        addresses.forEachIndexed { index, address ->
            list.add(SubAddress(walletId = wallet.id, address = address.toBase58()))
        }
        AppDatabase.getInstance().subAddressDao().insertSubAddress(*list.toTypedArray())
        return AppDatabase.getInstance().subAddressDao().loadSubAddressByWalletId(wallet.id)
    }

    fun addAddress(wallet: Wallet, label: String) {
        Context.propagate(Context(NETWORK_PARAMETERS))
        val walletFile = File(getWalletFilePath(wallet.name))
        val realWallet = WalletHelper.instance.getWallet(walletFile)
        val address = realWallet.freshReceiveAddress()
        realWallet.saveToFile(walletFile)
        AppDatabase.getInstance().subAddressDao().insertSubAddress(SubAddress(walletId = wallet.id, address = address.toBase58(), label = label))
    }

    fun openWallet(walletName: String) {
        WalletHelper.instance.closeWallet()
        val walletFile = File(getWalletFilePath(walletName))
        val dir = getWalletPath(walletName)
        WalletHelper.instance.openWallet(walletFile, File(dir, "blockchain"), dir, context.assets.open("checkpoints.txt"))
        WalletHelper.instance.walletName = walletName
    }

    fun calculateFee(amount: String, address: String?): String {
        val walletHelper = WalletHelper.instance
        val wallet = walletHelper.wallet ?: return "--"
        val coin = walletHelper.formatAmount(amount)
        val addr = if (address.isNullOrBlank()) {
            wallet.currentReceiveAddress()
        } else {
            walletHelper.getAddress(address)
        }
        val fee = walletHelper.calculateFee(coin, addr, wallet)
        return walletHelper.formatCoin(fee)
    }

    fun sendTransaction(amount: String, address: String, password: String) {
        val walletHelper = WalletHelper.instance
        val wallet = walletHelper.wallet ?: return
        val coin = walletHelper.formatAmount(amount)
        val addr = walletHelper.getAddress(address)
        walletHelper.sendTransaction(coin, addr, wallet, password)
    }

}