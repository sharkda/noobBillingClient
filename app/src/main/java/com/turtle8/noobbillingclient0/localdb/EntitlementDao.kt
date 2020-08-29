package com.turtle8.noobbillingclient0.localdb

import androidx.lifecycle.LiveData
import androidx.room.*

/**
* No update methods necessary since for each table there is ever expecting one row, hence why
* the primary key is hardcoded.
*/
@Dao
interface EntitlementDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(paidOneTime: PaidOneTime)

    @Update
    fun update(paidOneTime: PaidOneTime)

    @Query("SELECT * FROM paid_one_time LIMIT 1")
    fun getPaidPro(): LiveData<PaidOneTime>

    @Delete
    fun delete(paidOneTime: PaidOneTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(premium:PremiumSubscription)

    @Update
    fun update(premium: PremiumSubscription)

    @Query("SELECT * FROM premium_subscription LIMIT 1")
    fun getPremiumStatus(): LiveData<PremiumSubscription>

    @Delete
    fun delete(premium: PremiumSubscription)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(coin: CoinAsset )

    @Update
    fun update(coin: CoinAsset)

    @Query("SELECT * FROM coin_asset LIMIT 1")
    fun getCoin(): LiveData<CoinAsset>

    @Delete
    fun delete(coin: CoinAsset)

    /**
     * This is purely for convenience. The clients of this DAO don't have to discriminate among
     * [GasTank] vs [PremiumCar] vs [GoldStatus] but can simply send in a list of
     * [entitlements][Entitlement].
     */
    @Transaction
    fun insert(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is CoinAsset -> insert(it)
                is PaidOneTime -> insert(it)
                is PremiumSubscription -> insert(it)
            }
        }
    }

    @Transaction
    fun update(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is CoinAsset -> update(it)
                is PaidOneTime -> update(it)
                is PremiumSubscription -> update(it)
            }
        }
    }
}
