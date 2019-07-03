package io.wookey.dash.core

import android.arch.lifecycle.MutableLiveData
import android.util.Log
import io.wookey.dash.data.entity.TransactionInfo
import io.wookey.dash.support.BIP44_PATH
import io.wookey.dash.support.NETWORK_PARAMETERS
import io.wookey.dash.support.SCRYPT_ITERATIONS_TARGET
import org.bitcoinj.core.*
import org.bitcoinj.core.listeners.PeerConnectedEventListener
import org.bitcoinj.core.listeners.PeerDataEventListener
import org.bitcoinj.core.listeners.PeerDisconnectedEventListener
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener
import org.bitcoinj.crypto.KeyCrypter
import org.bitcoinj.crypto.KeyCrypterException
import org.bitcoinj.crypto.KeyCrypterScrypt
import org.bitcoinj.net.discovery.*
import org.bitcoinj.script.ScriptBuilder
import org.bitcoinj.store.BlockStore
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.utils.MonetaryFormat
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.*
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener
import org.spongycastle.crypto.params.KeyParameter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.TimeUnit

class WalletHelper private constructor() {

    companion object {
        val instance = SingletonHolder.holder

        fun isAddressValid(address: String): Boolean {
            return try {
                Address.fromBase58(NETWORK_PARAMETERS, address) != null
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun convertTransaction(wallet: Wallet, data: List<Transaction>): List<TransactionInfo> {
            val list = mutableListOf<TransactionInfo>()
            data.forEach {
                val tx = TransactionInfo()
                tx.hash = it.hashAsString
                tx.timestamp = it.updateTime.time
                val value = it.getValue(wallet)
                val sent = value.signum() < 0
                if (sent) {
                    tx.direction = 1
                    tx.address = getToAddressOfSent(it, wallet)?.toBase58() ?: ""
                } else {
                    tx.direction = 0
                    tx.address = getWalletAddressOfReceived(it, wallet)?.toBase58() ?: ""
                }
                val confidence = it.confidence
                val isLocked = confidence.isTransactionLocked
                val isCoinBase = it.isCoinBase
                when (confidence.confidenceType) {
                    TransactionConfidence.ConfidenceType.PENDING -> {
                        tx.isPending = isLocked
                        tx.isFailed = false
                    }
                    TransactionConfidence.ConfidenceType.BUILDING -> {
                        val progress = if (isLocked) {
                            confidence.depthInBlocks + 5
                        } else {
                            confidence.depthInBlocks
                        }
                        val max = if (isCoinBase) {
                            NETWORK_PARAMETERS.spendableCoinbaseDepth
                        } else {
                            6
                        }
                        tx.isPending = progress < max
                        tx.isFailed = false
                    }
                    else -> {
                        tx.isPending = false
                        tx.isFailed = true
                    }
                }
                val fee = it.fee
                if (fee == null) {
                    tx.fee = ""
                    tx.amount = instance.formatCoin(value)
                } else {
                    if (it.purpose == Transaction.Purpose.RAISE_FEE) {
                        tx.amount = instance.formatCoin(fee.negate())
                    } else {
                        if (sent) {
                            tx.amount = instance.formatCoin(value.minus(fee.negate()))
                        } else {
                            tx.amount = instance.formatCoin(value)
                        }
                    }
                    tx.fee = instance.formatCoin(fee.negate())
                }
                list.add(tx)
            }
            return list
        }

        fun getToAddressOfSent(tx: Transaction, wallet: Wallet): Address? {
            for (output in tx.outputs) {
                try {
                    if (!output.isMine(wallet)) {
                        val script = output.scriptPubKey
                        return script.getToAddress(NETWORK_PARAMETERS, true)
                    }
                } catch (e: ScriptException) {
                    // swallow
                    e.printStackTrace()
                }
            }
            return null
        }

        fun getWalletAddressOfReceived(tx: Transaction, wallet: Wallet): Address? {
            for (output in tx.outputs) {
                try {
                    if (output.isMine(wallet)) {
                        val script = output.scriptPubKey
                        return script.getToAddress(NETWORK_PARAMETERS, true)
                    }
                } catch (e: ScriptException) {
                    // swallow
                    e.printStackTrace()
                }
            }

            return null
        }
    }

    private object SingletonHolder {
        val holder = WalletHelper()
    }

    var exception: Exception? = null

    val context = Context(NETWORK_PARAMETERS)

    var walletName: String? = null
    var wallet: Wallet? = null
    var walletFile: File? = null
    var blockStore: BlockStore? = null
    var blockChain: BlockChain? = null
    var peerGroup: PeerGroup? = null

    val peerCount = MutableLiveData<Int>()
    val unSignedBalance = MutableLiveData<String>()
    val transactions = MutableLiveData<List<Transaction>>()

    val connecting = MutableLiveData<Boolean>()
    val progress = MutableLiveData<Double>()
    val blocksLeft = MutableLiveData<Int>()

    private var coinsReceivedEventListener: WalletCoinsReceivedEventListener? = null
    private var coinsSentEventListener: WalletCoinsSentEventListener? = null
    private var transactionConfidenceEventListener: TransactionConfidenceEventListener? = null

    private var peerConnectivityListener: PeerConnectedEventListener? = null
    private var peerDisConnectivityListener: PeerDisconnectedEventListener? = null
    private var peerDataEventListener: PeerDataEventListener? = null

    fun closeWallet() {
        try {
            wallet?.run {
                transactionConfidenceEventListener?.let { removeTransactionConfidenceEventListener(it) }
                coinsSentEventListener?.let { removeCoinsSentEventListener(it) }
                coinsReceivedEventListener?.let { removeCoinsReceivedEventListener(it) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            peerGroup?.run {
                peerDisConnectivityListener?.let { removeDisconnectedEventListener(it) }
                peerConnectivityListener?.let { removeConnectedEventListener(it) }
                wallet?.let { removeWallet(it) }
                stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            blockStore?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            wallet?.run {
                walletFile?.let { saveToFile(it) }
                shutdownAutosaveAndWait()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        walletFile = null
        wallet = null
        exception = null
        blocksLeft.postValue(null)
        progress.postValue(0.0)
        connecting.postValue(true)

    }

//    fun backupWallet(wallet: Wallet) {
//        val builder = WalletProtobufSerializer().walletToProto(wallet).toBuilder()
//        // strip redundant
//        builder.clearTransaction()
//        builder.clearLastSeenBlockHash()
//        builder.lastSeenBlockHeight = -1
//        builder.clearLastSeenBlockTimeSecs()
//        val walletProto = builder.build()
//        var os: OutputStream? = null
//        os = App.instance.openFileOutput("key-backup-protobuf", Context.MODE_PRIVATE)
//        walletProto.writeTo(os)
//        os.close()
//    }

    fun openWallet(walletFile: File, blockChainFile: File, directory: String, checkpoints: InputStream) {
        blocksLeft.postValue(null)
        progress.postValue(0.0)
        connecting.postValue(true)

        Context.propagate(context)
        val wallet = getWallet(walletFile)

        getBalance(wallet)
        getTransactions(wallet)

        wallet.context.initDash(true, true)
        wallet.autosaveToFile(walletFile, 5 * 1000, TimeUnit.MILLISECONDS, null)
        try {
            wallet.cleanup()
        } catch (e: Exception) {
            e.printStackTrace()
            val message = e.message
            if (!message.isNullOrBlank() && message.contains("Inconsistent spent tx:")) {
                blockChainFile.delete()
            }
        }

        resetWallet(wallet, blockChainFile)
        val blockStore = getBlockStore(wallet, blockChainFile, checkpoints)
        val blockChain = BlockChain(NETWORK_PARAMETERS, wallet, blockStore)
        startAsync(wallet, blockChain)

        val coinsReceivedEventListener = WalletCoinsReceivedEventListener { w, tx, prevBalance, newBalance ->
            getBalance(wallet)
        }
        wallet.addCoinsReceivedEventListener(Threading.SAME_THREAD, coinsReceivedEventListener)

        val coinsSentEventListener = WalletCoinsSentEventListener { w, tx, prevBalance, newBalance ->
            getBalance(wallet)
        }
        wallet.addCoinsSentEventListener(Threading.SAME_THREAD, coinsSentEventListener)

        val transactionConfidenceEventListener = TransactionConfidenceEventListener { w, tx ->
            getTransactions(wallet)
        }
        wallet.addTransactionConfidenceEventListener(Threading.SAME_THREAD, transactionConfidenceEventListener)

//        wallet.context.sporkManager.addEventListener()
        wallet.context.initDashSync(directory)

        this.walletFile = walletFile
        this.wallet = wallet
        this.blockStore = blockStore
        this.blockChain = blockChain
        this.coinsReceivedEventListener = coinsReceivedEventListener
        this.coinsSentEventListener = coinsSentEventListener
        this.transactionConfidenceEventListener = transactionConfidenceEventListener
    }

    fun getTransactions(wallet: Wallet) {
        Context.propagate(context)
        val transactions = mutableListOf<Transaction>()
        val txs = wallet.getTransactions(true)
        txs.forEach {
            transactions.add(it)
        }
        transactions.sortByDescending {
            it.updateTime.time
        }
        this.transactions.postValue(transactions)
    }

    fun getBalance(wallet: Wallet) {
        Context.propagate(context)
        val balance = wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        val unSigned = formatCoin(balance)
        this.unSignedBalance.postValue(unSigned)
    }

    fun getWallet(walletFile: File): Wallet {
        return FileInputStream(walletFile).use {
            WalletProtobufSerializer().readWallet(it)
        }
    }

    fun getBlockStore(wallet: Wallet, blockChainFile: File, checkpoints: InputStream): BlockStore {
        try {
            val exists = blockChainFile.exists()
            val blockStore = SPVBlockStore(NETWORK_PARAMETERS, blockChainFile)
            val chainHead = blockStore.chainHead
            val earliestKeyCreationTime = wallet.earliestKeyCreationTime
            if (!exists && earliestKeyCreationTime > 0) {
                CheckpointManager.checkpoint(NETWORK_PARAMETERS, checkpoints, blockStore, earliestKeyCreationTime)
            }
            return blockStore
        } catch (e: Exception) {
            blockChainFile.delete()
            throw e
        }
    }

    fun resetWallet(wallet: Wallet, blockChainFile: File) {
        if (!blockChainFile.exists()) {
            Log.e("openWallet", "blockchain does not exist, resetting wallet")
            wallet.reset()
        }
    }

    fun formatCoin(coin: Coin): String {
        return Math.abs(coin.value).times(Math.pow(10.0, -8.0)).toBigDecimal().toPlainString()
    }

    fun getMaxPrecisionFormat(): MonetaryFormat {
        return when (getBtcShift()) {
            0 -> MonetaryFormat().shift(0).minDecimals(2).optionalDecimals(2, 2, 2)
            3 -> MonetaryFormat().shift(3).minDecimals(2).optionalDecimals(2, 1)
            else -> MonetaryFormat().shift(6).minDecimals(0).optionalDecimals(2)
        }
    }

    fun formatAmount(amount: String) = getMaxPrecisionFormat().noCode().parse(amount)

    fun getAddress(address: String) = Address.fromBase58(NETWORK_PARAMETERS, address)

    fun getBtcShift() = 0

    fun getBtcPrecision() = 4

    fun createWallet(walletFile: File, password: String): io.wookey.dash.data.entity.Wallet {

        Context.propagate(context)

        val wallet = Wallet(NETWORK_PARAMETERS)
        wallet.addKeyChain(BIP44_PATH)

        val keyCrypter = KeyCrypterScrypt(SCRYPT_ITERATIONS_TARGET)
        val key = keyCrypter.deriveKey(password)
        wallet.encrypt(keyCrypter, key)

        val mnemonicCode = getMnemonic(wallet, password)

        wallet.saveToFile(walletFile)

        return io.wookey.dash.data.entity.Wallet().apply {
            address = wallet.currentReceiveAddress().toString()
            this.seed = mnemonicCode.joinToString(" ")
        }
    }

    fun getMnemonic(wallet: Wallet, password: String): List<String> {
        val pair = deriveKey(wallet, password)
        val keyCrypter = pair.first
        val key = pair.second
        val seed = wallet.activeKeyChain.seed
        val decrypt = seed?.decrypt(keyCrypter, null, key)
        return decrypt?.mnemonicCode ?: emptyList()
    }

    fun deriveKey(wallet: Wallet, password: String): Pair<KeyCrypter?, KeyParameter?> {
        // Key derivation takes time.
        val keyCrypter = wallet.keyCrypter
        var key = keyCrypter?.deriveKey(password)

        // If the key isn't derived using the desired parameters, derive a new key.
        if (keyCrypter is KeyCrypterScrypt) {
            val scryptIterations = keyCrypter.scryptParameters.n

            if (scryptIterations != SCRYPT_ITERATIONS_TARGET.toLong()) {

                val newKeyCrypter = KeyCrypterScrypt(SCRYPT_ITERATIONS_TARGET)
                val newKey = newKeyCrypter.deriveKey(password)

                // Re-encrypt wallet with new key.
                try {
                    wallet.changeEncryptionKey(newKeyCrypter, key, newKey)
                    key = newKey
                } catch (e: KeyCrypterException) {
                    e.printStackTrace()
                }
            }
        }
        return Pair(keyCrypter, key)
    }

    fun recoveryWallet(
            walletFile: File,
            password: String,
            mnemonic: String,
            creationTimeSeconds: Long
    ): io.wookey.dash.data.entity.Wallet {

        Context.propagate(context)

        val words = mnemonic.split(" ")

        val seeds = DeterministicSeed(words, null, "", creationTimeSeconds)
        val group = KeyChainGroup(NETWORK_PARAMETERS, seeds)
        group.addAndActivateHDChain(DeterministicKeyChain(seeds, BIP44_PATH))

        val wallet = Wallet(NETWORK_PARAMETERS, group)

        if (wallet.params != NETWORK_PARAMETERS)
            throw IOException("bad wallet backup network parameters: " + wallet.params.id)
        if (!wallet.isConsistent)
            throw IOException("inconsistent wallet backup")

        val keyCrypter = KeyCrypterScrypt(SCRYPT_ITERATIONS_TARGET)
        val key = keyCrypter.deriveKey(password)
        wallet.encrypt(keyCrypter, key)

        wallet.saveToFile(walletFile)

        return io.wookey.dash.data.entity.Wallet().apply {
            address = wallet.currentReceiveAddress().toString()
        }
    }

    fun startAsync(wallet: Wallet, blockChain: BlockChain) {
        val walletLastBlockSeenHeight = wallet.lastBlockSeenHeight
        val bestChainHeight = blockChain.bestChainHeight
        if (walletLastBlockSeenHeight != -1 && walletLastBlockSeenHeight != bestChainHeight) {
            val message = ("wallet/blockchain out of sync: " + walletLastBlockSeenHeight + "/"
                    + bestChainHeight)
            throw IllegalStateException(message)
        }

        val peerGroup = PeerGroup(NETWORK_PARAMETERS, blockChain)
        peerGroup.setDownloadTxDependencies(0) // recursive implementation causes StackOverflowError
        peerGroup.addWallet(wallet)
        peerGroup.setUserAgent(CoinDefinition.coinName + " Wallet", "1.0.0")

        val peerConnectivityListener = PeerConnectedEventListener { peer, peerCount ->
            this.peerCount.postValue(peerCount)
        }
        peerGroup.addConnectedEventListener(peerConnectivityListener)

        val peerDisConnectivityListener = PeerDisconnectedEventListener { peer, peerCount ->
            this.peerCount.postValue(peerCount)
        }
        peerGroup.addDisconnectedEventListener(peerDisConnectivityListener)

        peerGroup.maxConnections = 6
        peerGroup.setConnectTimeoutMillis(15 * 1000)
        peerGroup.setPeerDiscoveryTimeoutMillis(100 * 1000)

        peerGroup.addPeerDiscovery(object : PeerDiscovery {

            private val normalPeerDiscovery = MultiplexingDiscovery.forServices(NETWORK_PARAMETERS, 0)

            override fun getPeers(services: Long, timeoutValue: Long, timeoutUnit: TimeUnit?): Array<InetSocketAddress> {
                val peers = LinkedList<InetSocketAddress>()
//                var needsTrimPeersWorkaround = false
                try {
                    // DNS Seeds
                    peers.addAll(Arrays.asList(*normalPeerDiscovery.getPeers(services, timeoutValue, timeoutUnit)))
                } catch (e: PeerDiscoveryException) {
                    e.printStackTrace()
                }

                if (peers.size < 10) {
                    try {
                        val mnlist = Context.get().masternodeListManager.listAtChainTip
                        val discovery = MasternodePeerDiscovery(mnlist)
                        peers.addAll(Arrays.asList(*discovery.getPeers(services, timeoutValue, timeoutUnit)))
                    } catch (e: PeerDiscoveryException) {
                        //swallow and continue with another method of connection
                        e.printStackTrace()
                    }

                    if (peers.size < 10) {
                        // Addr Seeds
                        peers.addAll(Arrays.asList(*SeedPeers(NETWORK_PARAMETERS).getPeers(services, timeoutValue, timeoutUnit)))
                    }
                }

//                // workaround because PeerGroup will shuffle peers
//                if (needsTrimPeersWorkaround)
//                    while (peers.size >= 6)
//                        peers.removeAt(peers.size - 1)

                return peers.toTypedArray()
            }

            override fun shutdown() {
                normalPeerDiscovery.shutdown()
            }
        })

        peerGroup.startAsync()

        val peerDataEventListener = object : DownloadPeerDataListener() {
            override fun startDownload(blocks: Int) {
                super.startDownload(blocks)
                this@WalletHelper.connecting.postValue(false)
                this@WalletHelper.blocksLeft.postValue(blocks)
            }

            override fun doneDownload() {
                super.doneDownload()
                WalletHelper.instance.getBalance(wallet)
                WalletHelper.instance.getTransactions(wallet)
                this@WalletHelper.progress.postValue(100.0)
            }

            override fun progress(pct: Double, blocksLeft: Int) {
                super.progress(pct, blocksLeft)
                this@WalletHelper.progress.postValue(pct)
                this@WalletHelper.blocksLeft.postValue(blocksLeft)
            }
        }
        peerGroup.startBlockChainDownload(peerDataEventListener)

        this.peerGroup = peerGroup
        this.peerConnectivityListener = peerConnectivityListener
        this.peerDisConnectivityListener = peerDisConnectivityListener
        this.peerDataEventListener = peerDataEventListener
    }


    open class DownloadPeerDataListener : PeerDataEventListener {
        private var originalBlocksLeft = -1
        private var lastPercent = 0
        private var caughtUp = false

        override fun onBlocksDownloaded(peer: Peer?, block: Block?, filteredBlock: FilteredBlock?, blocksLeft: Int) {
            if (caughtUp) return

            if (blocksLeft == 0) {
                caughtUp = true
                doneDownload()
            }
            if (blocksLeft < 0 || originalBlocksLeft <= 0) return

            val pct = 100.0 - 100.0 * (blocksLeft / originalBlocksLeft.toDouble())
            if (pct.toInt() != lastPercent) {
                progress(pct, blocksLeft)
                lastPercent = pct.toInt()
            }
        }

        override fun onChainDownloadStarted(peer: Peer?, blocksLeft: Int) {
            if (blocksLeft > 0 && originalBlocksLeft == -1) {
                startDownload(blocksLeft)
            }
            if (originalBlocksLeft == -1) {
                originalBlocksLeft = blocksLeft
            } else {
                Log.i("onBlocksDownloaded", "Chain download switched to ${peer.toString()}")
            }
            if (blocksLeft == 0) {
                doneDownload()
            }
        }

        override fun onPreMessageReceived(peer: Peer?, m: Message?) = m

        override fun getData(peer: Peer?, m: GetDataMessage?) = null

        open fun startDownload(blocks: Int) {}
        open fun doneDownload() {}
        open fun progress(pct: Double, blocksLeft: Int) {}

    }

    fun calculateFee(amount: Coin, address: Address, wallet: Wallet): Coin {
        var sendRequest = createSendRequest(amount, address, wallet)
        wallet.completeTx(sendRequest)
        if (checkDust(sendRequest)) {
            sendRequest = createSendRequest(amount, address, wallet, ensureMinRequiredFee = true)
            wallet.completeTx(sendRequest)
        }
        return sendRequest.tx.fee
    }

    fun createSendRequest(
            amount: Coin,
            address: Address,
            wallet: Wallet,
            signInputs: Boolean = false,
            ensureMinRequiredFee: Boolean = false
    ): SendRequest {
        val transaction = Transaction(NETWORK_PARAMETERS)
        val script = ScriptBuilder.createOutputScript(address)
        transaction.addOutput(amount, script)
        val sendRequest = SendRequest.forTx(transaction)
        sendRequest.coinSelector = ZeroConfCoinSelector.get()
        sendRequest.useInstantSend = false
        sendRequest.feePerKb = Coin.valueOf(1000)
        sendRequest.ensureMinRequiredFee = if (ensureMinRequiredFee) true else sendRequest.useInstantSend
        sendRequest.signInputs = signInputs
        val walletBalance = wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        sendRequest.emptyWallet = walletBalance == amount
        return sendRequest
    }

    fun checkDust(req: SendRequest): Boolean {
        if (req.tx != null) {
            for (output in req.tx.outputs) {
                if (output.isDust)
                    return true
            }
        }
        return false
    }

    fun sendTransaction(amount: Coin, address: Address, wallet: Wallet, password: String) {
        Context.propagate(context)
        val pair = deriveKey(wallet, password)
        val key = pair.second
        var sendRequest = createSendRequest(amount, address, wallet, true)
        if (checkDust(sendRequest)) {
            sendRequest = createSendRequest(amount, address, wallet, true, true)
        }
        sendRequest.aesKey = key
        wallet.sendCoins(sendRequest)
    }

}