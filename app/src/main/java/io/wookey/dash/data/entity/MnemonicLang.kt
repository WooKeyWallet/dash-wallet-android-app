package io.wookey.dash.data.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MnemonicLang(val lang: String = "English", val fileName: String = "bip39-wordlist.txt") : Parcelable