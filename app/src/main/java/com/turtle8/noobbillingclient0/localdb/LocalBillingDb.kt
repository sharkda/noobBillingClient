package com.turtle8.noobbillingclient0.localdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [
    AugmentedSkuDetails::class,
    PaidOneTime::class,
    PremiumSubscription::class,
    CoinAsset::class,
    CachedPurchase::class
], version = 1_4  , exportSchema = false)
@TypeConverters(PurchaseTypeConverter::class)
abstract class LocalBillingDb:RoomDatabase(){
    abstract val entitlementsDao:EntitlementsDao
    abstract val skuDetailsDao:AugmentedSkuDetailsDao
    abstract val purchaseDao:PurchaseDao
    companion object{
        @Volatile
        private var INSTANCE: LocalBillingDb? = null
        private val DATABASE_NAME = "billing_Db"

        fun getInstance(context: Context): LocalBillingDb =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalBillingDb::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration()
                    .build().also {
                        INSTANCE  = it
                    }
            }
    }
}