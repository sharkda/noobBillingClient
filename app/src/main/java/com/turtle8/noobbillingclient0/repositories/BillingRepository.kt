package com.turtle8.noobbillingclient0.repositories

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.android.billingclient.api.*
import com.turtle8.noobbillingclient0.localdb.*
import com.turtle8.noobbillingclient0.repositories.BillingRepository.RetryPolicies.taskExecutionRetryPolicy
import com.turtle8.noobbillingclient0.repositories.BillingRepository.Throttle.isLastInvocationTimeStale
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class BillingRepository private constructor(
    private val application: Application
) :
    BillingClientStateListener, PurchasesUpdatedListener, ConsumeResponseListener {

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

    /**
     * A local cache billing client is important in that the Play Store may be temporarily
     * unavailable during updates. In such cases, it may be important that the users
     * continue to get access to premium data that they own. Alternatively, you may choose not to
     * provide offline access to your premium content.
     *
     * Even beyond offline access to premium content, however, a local cache billing client makes
     * certain transactions easier. Without an offline cache billing client, for instance, the app
     * would need both the secure server and the Play Billing client to be available in order to
     * process consumable products.
     *
     * The data that lives here should be refreshed at regular intervals so that it reflects what's
     * in the Google Play Store.
     */
    private lateinit var localCacheBillingClient: LocalBillingDb

    //paidoneTme, the only ones that inAppPurhcase usually should care
    val paidLiveData: LiveData<PaidOneTime> by lazy {
        if (::localCacheBillingClient.isInitialized == false) {
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.entitlementsDao.getPaidOneTime()
    }

    /**
     * Tracks whether this user is entitled to subscribed. This call returns data from the app's
     * own local DB; this way if Play and the secure server are unavailable, users still have
     * access to features they purchased.  Normally this would be a good place to update the local
     * cache to make sure it's always up-to-date. However, onBillingSetupFinished already called
     * queryPurchasesAsync for you; so no need.
     */
    val premiumStatusLiveData: LiveData<PremiumSubscription> by lazy {
        if (::localCacheBillingClient.isInitialized == false) {
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.entitlementsDao.getPremiumStatus()
    }
    val coinLiveData: LiveData<CoinAsset> by lazy {
        if (::localCacheBillingClient.isInitialized == false) {
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.entitlementsDao.getCoin()
    }

    /**
     * This list tells clients what in-app products are available for sale
     */
    val inappSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        if (::localCacheBillingClient.isInitialized == false) {
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.skuDetailsDao.getInappSkuDetails()
    }
    /**
     * This list tells clients what subscriptions are available for sale
     */
    val subsSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        if (::localCacheBillingClient.isInitialized == false) {
            localCacheBillingClient = LocalBillingDb.getInstance(application)
        }
        localCacheBillingClient.skuDetailsDao.getSubscriptionSkuDetails()
    }

    fun startDataSourceConnections() {
        Log.d(LOG_TAG, "startDataSourceConnections")
        localCacheBillingClient = LocalBillingDb.getInstance(application)
        instantiateAndConnectToPlayBillingService()
    }

    fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
        Log.d(LOG_TAG, "endDataSourceConnections")
    }

    private fun instantiateAndConnectToPlayBillingService() {
        playStoreBillingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases() //required or app will crash :-b
            .setListener(this).build()
        connectToPlayBillingService()
    }

    private fun connectToPlayBillingService(): Boolean {
        Log.d(LOG_TAG, "connectToBillingService")
        if (!playStoreBillingClient.isReady) {
            playStoreBillingClient.startConnection(this)
            return true
        }
        return false
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(LOG_TAG, "onBillingSetupFinished successfully")
                querySkuDetailsAsync(BillingClient.SkuType.INAPP, AppSku.InApp_Skus)
                querySkuDetailsAsync(BillingClient.SkuType.SUBS, AppSku.Subs_Skus)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                //Some apps may choose to make decisions based on this knowledge.
                Log.d(LOG_TAG, "unav ${billingResult.debugMessage}")
            }
            else -> {
                //do nothing. Someone else will connect it through retry policy.
                //May choose to send to server though
                Log.d(LOG_TAG, billingResult.debugMessage)
            }
        }
    }
    private fun querySkuDetailsAsync(
        @BillingClient.SkuType skuType: String,
        skuList: List<String>
    ){
        val params = SkuDetailsParams.newBuilder().setSkusList(skuList)
            .setType(skuType).build()
        Log.d(LOG_TAG, "querySkuDetailsAsync for $skuType")
        playStoreBillingClient.querySkuDetailsAsync(params){
            billingResult,
                skuDetailList ->
            when (billingResult.responseCode){
                BillingClient.BillingResponseCode.OK ->{
                    if (skuDetailList.orEmpty().isNotEmpty()){
                        skuDetailList?.forEach{
                            CoroutineScope(Job() +Dispatchers.IO)
                                .launch{
                                    localCacheBillingClient.skuDetailsDao.insertOrUpdate(it)
                                }
                        }
                    }
                }
                else ->{
                    Log.e(LOG_TAG, billingResult.debugMessage)
                }
            }
        }
    }
    /**check play store and add purchase into purchaseResult. inApp and sub(if supported on device)
     *
    */
    fun queryPurchasesAsync() {
        Log.d(LOG_TAG, "queryPurchasesAsync called")
        val purchasesResult = HashSet<Purchase>()
        var result = playStoreBillingClient.queryPurchases(BillingClient.SkuType.INAPP)
        Log.d(LOG_TAG, "queryPurchasesAsync INAPP results: ${result?.purchasesList?.size}")
        result?.purchasesList?.apply { purchasesResult.addAll(this) }
        if (isSubscriptionSupported()) {
            result = playStoreBillingClient.queryPurchases(BillingClient.SkuType.SUBS)
            result?.purchasesList?.apply { purchasesResult.addAll(this) }
            Log.d(LOG_TAG, "queryPurchasesAsync SUBS results: ${result?.purchasesList?.size}")
        }
        processPurchases(purchasesResult)
    }

    /**
     * - input is purchaseResults set.
     * 1. validate signatures. -> into validPurchase set.
     *
     * */
    private fun processPurchases(purchasesResult: Set<Purchase>) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            Log.d(LOG_TAG, "processPurchases called")
            val cachedPurchases = localCacheBillingClient.purchaseDao.getPurchases()
            val newBatch = HashSet<Purchase>(purchasesResult.size)
            purchasesResult.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    && !cachedPurchases.any { it.data == purchase }) {
                    if (isSignatureValid(purchase)) {
                        newBatch.add(purchase)
                    }
                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log.d(LOG_TAG, "Received a pending purchase of SKU: ${purchase.sku}")
                    // handle pending purchases, e.g. confirm with users about the pending
                    // purchases, prompt them to complete it, etc.
                }
            }
            if (newBatch.isNotEmpty()) {
                sendPurchasesToServer(newBatch)
                // We still care about purchasesResult in case a old purchase has not yet been consumed.
                saveToLocalDatabase(newBatch, purchasesResult)
                //consumeAsync(purchasesResult): do this inside saveToLocalDatabase to avoid race condition
            } else if (isLastInvocationTimeStale(application)) {
                handleConsumablePurchasesAsync(purchasesResult)
                //queryPurchasesFromSecureServer()
            }
            //below seems wrongly input here... more like testing of
            //consuming the cons.
//            val (consumables, nonConsumables) = newBatch.partition {
//                AppSku.Consumable_Skus.contains(it.sku)
//            }
//            Log.d(LOG_TAG, "processPurchases consumables content $consumables")
//            Log.d(LOG_TAG, "processPurchases non-consumables content $nonConsumables")
//            /*
//              As is being done in this sample, for extra reliability you may store the
//              receipts/purchases to a your own remote/local database for until after you
//              disburse entitlements. That way if the Google Play Billing library fails at any
//              given point, you can independently verify whether entitlements were accurately
//              disbursed. In this sample, the receipts are then removed upon entitlement
//              disbursement.
//             */
//            val testing = localCacheBillingClient.purchaseDao.getPurchases()
//            Log.d(LOG_TAG, "processPurchases purchases in the lcl db ${testing?.size}")
//            localCacheBillingClient.purchaseDao.insert(*newBatch.toTypedArray())
//            handleConsumablePurchasesAsync(consumables)
//            acknowledgeNonConsumablePurchasesAsync(nonConsumables)
        }


    private fun saveToLocalDatabase(newBatch: Set<Purchase>, allPurchases: Set<Purchase>) {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            newBatch.forEach { purchase ->
                when (purchase.sku) {
                    AppSku.ONE_TIME -> {
                        val oneTime = PaidOneTime(true)
                        insert(oneTime)
                        localCacheBillingClient.skuDetailsDao.insertOrUpdate(purchase.sku, oneTime.mayPurchase())
                    }
                    AppSku.SUB_MONTHLY-> {
                        val sub = PremiumSubscription(true)
                        insert(sub)
                        localCacheBillingClient.skuDetailsDao.insertOrUpdate(purchase.sku, sub.mayPurchase())
                        AppSku.Subs_Skus.forEach { otherSku ->
                            if (otherSku != purchase.sku) {
                                localCacheBillingClient.skuDetailsDao.insertOrUpdate(otherSku, !sub.mayPurchase())
                            }
                        }
                    }
                }
            }
            localCacheBillingClient.purchaseDao.insert(*newBatch.toTypedArray())
            handleConsumablePurchasesAsync(allPurchases)
        }
    }

    private fun saveToLocalDatabase(purchaseToken: String) {
        val scope = CoroutineScope(Job() + Dispatchers.IO)
        scope.launch {
            val cachedPurchases = localCacheBillingClient.purchaseDao.getPurchases()
            val match = cachedPurchases.find { it.purchaseToken == purchaseToken }
            if (match?.sku == AppSku.COIN) {
                //pdateGasTank(GasTank(GAS_PURCHASE))
                localCacheBillingClient.purchaseDao.delete(match)
            }
        }
    }

    private fun handleConsumablePurchasesAsync(purchases: Set<Purchase>) {
        purchases.forEach {
            if (AppSku.Consumable_Skus.contains(it.sku)) {
                //TODO:unmark below and implement it.
                //playStoreBillingClient.consumeAsync(it., this@BillingRepository)
                //tell your server:
                Log.i(LOG_TAG, "handleConsumablePurchasesAsync: asked Play Billing to consume sku = ${it.sku}")
            }
        }
    }

    private fun sendPurchasesToServer(purchases: Set<Purchase>) {
        //not implemented here
    }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchaseAsync] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: List<Purchase>) {
        nonConsumables.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(
                purchase
                    .purchaseToken
            ).build()
            playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        disburseNonConsumableEntitlement(purchase)
                    }
                    else -> Log.d(
                        LOG_TAG,
                        "acknowledgeNonConsumablePurchasesAsync response is ${billingResult.debugMessage}"
                    )
                }
            }

        }
    }

    /**
     * This is the final step, where purchases/receipts are converted to premium contents.
     * In this sample, once the entitlement is disbursed the receipt is thrown out.
     */
    private fun disburseNonConsumableEntitlement(purchase: Purchase) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            when (purchase.sku) {
                AppSku.ONE_TIME -> {
                    val pot = PaidOneTime(true)
                    insert(pot)
                    localCacheBillingClient.skuDetailsDao
                        .insertOrUpdate(purchase.sku, pot.mayPurchase())
                }
                AppSku.SUB_MONTHLY -> {
                    val premiumSubscription = PremiumSubscription(true)
                    insert(premiumSubscription)
                    localCacheBillingClient.skuDetailsDao
                        .insertOrUpdate(purchase.sku, premiumSubscription.mayPurchase())
                    /* there is more than one way to buy gold status. After disabling the
                    one the user just purchased, re-enable the others */
                    //shark this is loop is not necessary if only 1 sub... (monthly only)
                    AppSku.Subs_Skus.forEach { otherSku ->
                        if (otherSku != purchase.sku) {
                            localCacheBillingClient.skuDetailsDao
                                .insertOrUpdate(otherSku, !premiumSubscription.mayPurchase())
                        }
                    }
                }
            }
            localCacheBillingClient.purchaseDao.delete(purchase)
        }

    @WorkerThread
    suspend private fun insert(entitlement: Entitlement) = withContext(Dispatchers.IO) {
        localCacheBillingClient.entitlementsDao.insert(entitlement)
    }

    /**
     * Recall that Google Play Billing only supports two SKU types:
     * [in-app products][BillingClient.SkuType.INAPP] and
     * [subscriptions][BillingClient.SkuType.SUBS]. In-app products are actual items that a
     * user can buy, such as a house or food; subscriptions refer to services that a user must
     * pay for regularly, such as auto-insurance. Subscriptions are not consumable.
     *
     * Play Billing provides methods for consuming in-app products because they understand that
     * apps may sell items that users will keep forever (i.e. never consume) such as a house,
     * and consumable items that users will need to keep buying such as food. Nevertheless, Google
     * Play leaves the distinction for which in-app products are consumable entirely up to you.
     *
     * If an app wants its users to be able to keep buying an item, it must call
     * [BillingClient.consumeAsync] each time they buy it. This is because Google Play won't let
     * users buy items that they've previously bought but haven't consumed. In Trivial Drive, for
     * example, consumeAsync is called each time the user buys gas; otherwise they would never be
     * able to buy gas or drive again once the tank becomes empty.
     */
    private fun handleConsumablePurchasesAsync(consumables: List<Purchase>) {
        Log.d(LOG_TAG, "handleConsumablePurchasesAsync called")
        consumables.forEach {
            Log.d(LOG_TAG, "handleConsumablePurchasesAsync foreach it is $it")
            val params =
                ConsumeParams.newBuilder().setPurchaseToken(it.purchaseToken).build()
            playStoreBillingClient.consumeAsync(params) { billingResult, purchaseToken ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        // Update the appropriate tables/databases to grant user the items
                        purchaseToken.apply { disburseConsumableEntitlements(it) }
                    }
                    else -> {
                        Log.w(LOG_TAG, billingResult.debugMessage)
                    }
                }
            }
        }
    }
    //??? not sure when play called onConsumeRespone!.
    private fun disburseConsumableEntitlements(purchase: Purchase) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            if (purchase.sku == AppSku.COIN) {
                Log.d(LOG_TAG, "updateAppSinceCoinsMustBuySomthing.....")
                /**
                 * This disburseConsumableEntitlements method was called because Play called onConsumeResponse.
                 * So if you think of a Purchase as a receipt, you no longer need to keep a copy of
                 * the receipt in the local cache since the user has just consumed the product.
                 */
                localCacheBillingClient.purchaseDao.delete(purchase)
            }
        }


    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary. @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(
            Security.BASE_64_ENCODED_PUBLIC_KEY, purchase.originalJson, purchase.signature
        )
    }

    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            playStoreBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        var succeeded = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connectToPlayBillingService()
            BillingClient.BillingResponseCode.OK -> succeeded = true
            else -> Log.w(
                LOG_TAG,
                "isSubscriptionSupported() error: ${billingResult.debugMessage}"
            )
        }
        return succeeded
    }

    /**
     * This method is called when the app has inadvertently disconnected from the [BillingClient].
     * An attempt should be made to reconnect using a retry policy. Note the distinction between
     * [endConnection][BillingClient.endConnection] and disconnected:
     * - disconnected means it's okay to try reconnecting.
     * - endConnection means the [playStoreBillingClient] must be re-instantiated and then start
     *   a new connection because a [BillingClient] instance is invalid after endConnection has
     *   been called.
     **/
    override fun onBillingServiceDisconnected() {
        Log.d(LOG_TAG, "onBillingServiceDisconnected")
        connectToPlayBillingService()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.apply { processPurchases(this.toSet()) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                Log.d(LOG_TAG, billingResult.debugMessage)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                connectToPlayBillingService()
            }
            else -> {
                Log.d(LOG_TAG, billingResult.debugMessage)
            }
        }
    }

    suspend fun devInsertPaidOneTime() {
        withContext(Dispatchers.IO) {
            localCacheBillingClient.entitlementsDao.insert(PaidOneTime(true))
            val pot = PaidOneTime(true)
            localCacheBillingClient.skuDetailsDao
                .insertOrUpdate(AppSku.ONE_TIME, pot.mayPurchase())
        }
    }

    suspend fun devDelPaidOneTime() {
        withContext(Dispatchers.IO) {
            localCacheBillingClient.entitlementsDao.delete(PaidOneTime(true))
            localCacheBillingClient.skuDetailsDao.delete(AppSku.ONE_TIME)
        }
    }

    suspend fun devAddCoins(count:Int){
        withContext(Dispatchers.IO) {
            localCacheBillingClient.entitlementsDao.insert(CoinAsset(count))
            localCacheBillingClient.skuDetailsDao
                .insertOrUpdate(AppSku.COIN, CoinAsset(count).mayPurchase())
        }
    }


    fun launchBillingFlow(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) =
        launchBillingFlow(activity, SkuDetails(augmentedSkuDetails.originJson!!))

    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        //val oldSku: String? = getOldSku(skuDetails.sku)
        val purchaseParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails)
            .build()
            //.setOldSku(oldSku).build()

        taskExecutionRetryPolicy(playStoreBillingClient, this) {
            playStoreBillingClient.launchBillingFlow(activity, purchaseParams)
        }
    }

    /**
     * This private object class shows an example retry policies. You may choose to replace it with
     * your own policies.
     */
    private object RetryPolicies {
        private val maxRetry = 5
        private var retryCounter = AtomicInteger(1)
        private val baseDelayMillis = 500
        private val taskDelay = 2000L

        fun resetConnectionRetryPolicyCounter() {
            retryCounter.set(1)
        }

        /**
         * This works because it actually only makes one call. Then it waits for success or failure.
         * onSuccess it makes no more calls and resets the retryCounter to 1. onFailure another
         * call is made, until too many failures cause retryCounter to reach maxRetry and the
         * policy stops trying. This is a safe algorithm: the initial calls to
         * connectToPlayBillingService from instantiateAndConnectToPlayBillingService is always
         * independent of the RetryPolicies. And so the Retry Policy exists only to help and never
         * to hurt.
         */
        fun connectionRetryPolicy(block: () -> Unit) {
            Log.d(LOG_TAG, "connectionRetryPolicy")
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                val counter = retryCounter.getAndIncrement()
                if (counter < maxRetry) {
                    val waitTime: Long = (2f.pow(counter) * baseDelayMillis).toLong()
                    delay(waitTime)
                    block()
                }
            }

        }

        /**
         * All this is doing is check that billingClient is connected and if it's not, request
         * connection, wait x number of seconds and then proceed with the actual task.
         */
        fun taskExecutionRetryPolicy(billingClient: BillingClient, listener: BillingRepository, task: () -> Unit) {
            val scope = CoroutineScope(Job() + Dispatchers.Main)
            scope.launch {
                if (!billingClient.isReady) {
                    Log.d(LOG_TAG, "taskExecutionRetryPolicy billing not ready")
                    billingClient.startConnection(listener)
                    delay(taskDelay)
                }
                task()
            }
        }
    }
    //this fun is for SubScription 1 -> 2
//    private fun getOldSku(sku: String?): String? {
//        var result: String? = null
//        if (AppSku.Subs_Skus.contains(sku)) {
//            premiumStatusLiveData.value?.apply {
//                result = when (sku) {
//                    AppSku.SUB_MONTHLY -> AppSku.GOLD_YEARLY
//                    else -> AppSku.SUB_MONTHLY
//                }
//            }
//        }
//        return result
//    }


    companion object {
        private const val LOG_TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null
        fun getInstance(application: Application): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(application)
                    .also { INSTANCE = it }
            }
    }

    private object AppSku{
        val ONE_TIME = "one_time"
        val COIN = "coin"
        val SUB_MONTHLY = "sub_monthly"
        val InApp_Skus = listOf(ONE_TIME, COIN)
        val Subs_Skus = listOf(SUB_MONTHLY)
        val Consumable_Skus = listOf(COIN)
        val Premium_Status_Skus = Subs_Skus //coincidence.
    }
    /**
     * This is the throttling valve. It is used to modulate how often calls are made to the
     * secure server in order to save money.
     */
    private object Throttle {
        private val DEAD_BAND = 7200000//2*60*60*1000: two hours wait
        private val PREFS_NAME = "BillingRepository.Throttle"
        private val KEY = "lastInvocationTime"

        fun isLastInvocationTimeStale(context: Context): Boolean {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastInvocationTime = sharedPrefs.getLong(KEY, 0)
            return lastInvocationTime + DEAD_BAND < Date().time
        }

        fun refreshLastInvocationTime(context: Context) {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putLong(KEY, Date().time)
                apply()
            }
        }
    }

    override fun onConsumeResponse(billingResult: BillingResult, purchaseToken: String) {
        Log.d(LOG_TAG, "onConsumeResponse")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                //give user the items s/he just bought by updating the appropriate tables/databases
                purchaseToken?.apply { saveToLocalDatabase(this) }
                //secureServerBillingClient.onComsumeResponse(purchaseToken, responseCode)
            }
            else -> {
                Log.w(LOG_TAG, "Error consuming purchase with token ($purchaseToken). " +
                        "Response code: ${billingResult.responseCode}")
            }
        }

    }

    /**
     * The gas level can be updated from the client when the user drives or from a data source
     * (e.g. Play BillingClient) when the user buys more gas. Hence this repo must watch against
     * race conditions and interleaves. sharkda: codes below might be problematic...
     */
    @WorkerThread
    suspend fun updateCoinAsset(coin: CoinAsset) = withContext(Dispatchers.IO) {
        Log.d(LOG_TAG, "updateGasTank")
        var update: CoinAsset = coin
        coinLiveData.value?.apply {
            synchronized(this) {
                if (this != coin) {//new purchase
                    update = CoinAsset(getCoins() + coin.getCoins())
                }
                Log.d(
                    LOG_TAG,
                    "New coin asset is ${coin.getCoins()}; existing level is ${getCoins()}; so the final result is ${update.getCoins()}"
                )
                localCacheBillingClient.entitlementsDao.update(update)
            }
        }
        if (coinLiveData.value == null) {
            localCacheBillingClient.entitlementsDao.insert(update)
            Log.d(LOG_TAG, "No we just added from null gas with level: ${coin.getCoins()}")
        }
        localCacheBillingClient.skuDetailsDao.insertOrUpdate(AppSku.COIN, update.mayPurchase())
        Log.d(LOG_TAG, "updated AugmentedSkuDetails as well")
    }

}