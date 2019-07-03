package io.wookey.dash.data.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "subAddress", indices = [Index(value = arrayOf("walletId", "address"), unique = true)])
data class SubAddress @JvmOverloads constructor(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "_id")
        var id: Int = 0,
        @ColumnInfo
        var walletId: Int = 0,
        @ColumnInfo
        var address: String = "",
        @ColumnInfo
        var label: String = ""
) : Parcelable