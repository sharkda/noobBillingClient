package com.turtle8.noobbillingclient0.localdb

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [
    PaidOneTime::class, PremiumSubscription::class, CoinAsset::class
], version = 1, exportSchema = false)
abstract class LocalBillingDb:RoomDatabase(){
    abstract val entitilmentDao:EntitlementDao
    companion object{
        @Volatile
        private var INSTANCE: LocalBillingDb? = null
        private val DATABASE_NAME = "purchse_db"
        fun getInstance(context: Context): LocalBillingDb =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LocalBillingDb::class.java,
                    DATABASE_NAME
                ).fallbackToDestructiveMigration()
                    .build()
            }
    }
}