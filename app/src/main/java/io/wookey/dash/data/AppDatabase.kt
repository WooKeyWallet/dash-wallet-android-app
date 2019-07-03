package io.wookey.dash.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import io.wookey.dash.App
import io.wookey.dash.data.dao.*
import io.wookey.dash.data.entity.*

@Database(entities = [Wallet::class, Asset::class, Node::class, AddressBook::class, TransactionInfo::class, SubAddress::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletDao(): WalletDao
    abstract fun assetDao(): AssetDao
    abstract fun nodeDao(): NodeDao
    abstract fun addressBookDao(): AddressBookDao
    abstract fun transactionInfoDao(): TransactionInfoDao
    abstract fun subAddressDao(): SubAddressDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context = App.instance): AppDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
                }

        private fun buildDatabase(context: Context) =
                Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "Wallet.db")
                        .build()

    }
}