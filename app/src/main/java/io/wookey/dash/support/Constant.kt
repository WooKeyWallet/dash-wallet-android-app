package io.wookey.dash.support

import io.wookey.dash.data.entity.Coin
import io.wookey.dash.data.entity.MnemonicLang
import io.wookey.dash.data.entity.Node
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.wallet.DeterministicKeyChain

const val WALLET_RECOVERY = 1
const val WALLET_CREATE = 0

const val TRANSFER_ALL = 0
const val TRANSFER_IN = 1
const val TRANSFER_OUT = 2

const val SELECT_ADDRESS = 1

const val REQUEST_SCAN_ADDRESS = 100
const val REQUEST_SELECT_COIN = 101
const val REQUEST_SELECT_ADDRESS = 102
const val REQUEST_SELECT_NODE = 103
const val REQUEST_SELECT_SUB_ADDRESS = 104
const val REQUEST_SELECT_MNEMONIC_LANGUAGE = 105

const val REQUEST_CODE_PERMISSION_CAMERA = 501

const val ZH_CN = "zh-CN"
const val EN = "en"

val NETWORK_PARAMETERS = MainNetParams.get()

val BIP44_PATH = DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH

var CURRENT_MNEMONIC_LANGUAGE = MnemonicLang("English", "bip39-wordlist.txt")

const val SCRYPT_ITERATIONS_TARGET = 65536

val mnemonicLangList = listOf(
        MnemonicLang("English", "bip39-wordlist.txt"),
        MnemonicLang("中文（简体）", "chinese_simplified.txt"),
        MnemonicLang("中文（繁體）", "chinese_traditional.txt"),
        MnemonicLang("日本語", "japanese.txt"),
        MnemonicLang("Español", "spanish.txt"),
        MnemonicLang("Français", "french.txt"),
        MnemonicLang("Italiano", "italian.txt"),
        MnemonicLang("한국어", "korean.txt")
)

val coinList = listOf(
        Coin("XMR", "Monero")
)

val nodeArray = arrayOf(
        Node().apply {
            symbol = "XMR"
            url = "node.moneroworld.com:18089"
            isSelected = true
        },
        Node().apply {
            symbol = "XMR"
            url = "opennode.xmr-tw.org:18089"
            isSelected = false
        },
        Node().apply {
            symbol = "XMR"
            url = "uwillrunanodesoon.moneroworld.com:18089"
            isSelected = false
        },
        Node().apply {
            symbol = "XMR"
            url = "124.160.224.28:18081"
            isSelected = false
        }
)
