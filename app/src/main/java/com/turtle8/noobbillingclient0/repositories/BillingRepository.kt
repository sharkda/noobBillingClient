package com.turtle8.noobbillingclient0.repositories

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.turtle8.noobbillingclient0.localdb.LocalBillingDb
import com.turtle8.noobbillingclient0.localdb.PaidOneTime

class BillingRepository private constructor(
    private val application: Application):
BillingClientStateListener{

    /**
     * The [BillingClient] is the most reliable and primary source of truth for all purchases
     * made through the Google Play Store. The Play Store takes security precautions in guarding
     * the data. Also, the data is available offline in most cases, which means the app incurs no
     * network charges for checking for purchases using the [BillingClient]. The offline bit is
     * because the Play Store caches every purchase the user owns, in an
     * [eventually consistent manner](https://developer.android.com/google/play/billing/billing_library_overview#Keep-up-to-date).
     * This is the only billing client an app is actually required to have on Android. The other
     * two (webServerBillingClient and localCacheBillingClient) are optional.
     *
     * ASIDE. Notice that the connection to [playStoreBillingClient] is created using the
     * applicationContext. This means the instance is not [Activity]-specific. And since it's also
     * not expensive, it can remain open for the life of the entire [Application]. So whether it is
     * (re)created for each [Activity] or [Fragment] or is kept open for the life of the application
     * is a matter of choice.
     */
    lateinit private var playStoreBillingClient: BillingClient

    private lateinit var localCacheBillingClient:LocalBillingDb

    val paidLiveData: LiveData<PaidOneTime> by lazy{
        if (::localCacheBillingClient.isInitialized == false){
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.entitlementsDao.getPaidOneTime()
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(LOG_TAG, "onBillingSetupFinished ${billingResult.toString()}")
    }

    override fun onBillingServiceDisconnected() {
        Log.d(LOG_TAG, "onBillingServiceDisconnected")
    }
    fun startDataSourceConnections(){
        Log.d(LOG_TAG, "startDataSourceConnections")
        localCacheBillingClient = LocalBillingDb.getInstance(application)
    }
    fun endDataSourceConnections(){
        playStoreBillingClient.endConnection()
        Log.d(LOG_TAG, "endDataSourceConnections")
    }
    companion object{
        private const val LOG_TAG = "BillingRepository"
        @Volatile
        private var INSTANCE:BillingRepository? = null
        fun getInstance(application: Application): BillingRepository =
            INSTANCE ?: synchronized(this){
                INSTANCE ?: BillingRepository(application)
                    .also { INSTANCE = it }
            }
    }
}