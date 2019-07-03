package io.wookey.dash.data.dao

import android.arch.persistence.room.*
import io.wookey.dash.data.entity.SubAddress

@Dao
interface SubAddressDao {

    /**
     * Insert a subAddress in the database. If the subAddress already exists, replace it.
     *
     * @param subAddress the subAddress to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSubAddress(vararg subAddress: SubAddress)

    /**
     * Select all subAddress from the subAddress table.
     *
     * @return all subAddress.
     */
    @Query("SELECT * FROM subAddress WHERE walletId = :walletId")
    fun loadSubAddressByWalletId(walletId: Int): List<SubAddress>

    /**
     * Update a subAddress in the database.
     *
     * @param subAddress the subAddress to be updated.
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun updateSubAddress(subAddress: SubAddress)
}