package io.wookey.dash.data.dao

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import io.wookey.dash.data.entity.TransactionInfo

@Dao
interface TransactionInfoDao {

    /**
     * Insert a transactionInfo in the database. If the transactionInfo already exists, replace it.
     *
     * @param transactionInfo the transactionInfo to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactionInfo(vararg transactionInfo: TransactionInfo)

    /**
     * Select all transactionInfo by assetId from the transactionInfo table.
     *
     * @return all transactionInfo by assetId.
     */
    @Query("SELECT * FROM transactionInfo WHERE assetId = :assetId")
    fun getTransactionInfoByAssetId(assetId: Int): List<TransactionInfo>

    /**
     * Select all transactionInfo by assetId from the transactionInfo table.
     *
     * @return all transactionInfo by assetId.
     */
    @Query("SELECT * FROM transactionInfo WHERE assetId = :assetId")
    fun loadTransactionInfoByAssetId(assetId: Int): LiveData<List<TransactionInfo>>

    /**
     * Select all transactionInfo by walletId from the transactionInfo table.
     *
     * @return all transactionInfo by walletId.
     */
    @Query("SELECT * FROM transactionInfo WHERE walletId = :walletId")
    fun getTransactionInfoByWalletId(walletId: Int): List<TransactionInfo>

    /**
     * Delete transactionInfo in the database
     *
     * @param transactionInfo the transactionInfo to be deleted.
     */
    @Delete
    fun deleteTransactionInfo(vararg transactionInfo: TransactionInfo)
}