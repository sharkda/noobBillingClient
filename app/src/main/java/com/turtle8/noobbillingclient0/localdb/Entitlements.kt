package com.turtle8.noobbillingclient0.localdb

import androidx.room.Entity
import androidx.room.PrimaryKey

private const val MAX_ASSET_COINS = 4
private const val EMTPY_ASSET = 0

abstract class Entitlement{
    @PrimaryKey
    var id:Int = 1
    /**
     * This method tells clients whether a user __should__ buy a particular item at the moment. For
     * example, if the gas tank is full the user should not be buying gas. This method is __not__
     * a reflection on whether Google Play Billing can make a purchase.
     */
    abstract fun mayPurchase(): Boolean
}

/**
 * Indicates whether the user owns a Paid Pro onetime.
 */
@Entity(tableName = "paid_one_time")
data class PaidOneTime(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

/**
 * Indicates whether the user owns a premium car,e= subscribed
 */
@Entity(tableName = "premium_subscription")
data class PremiumSubscription(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

/*for copper coins, consumable, please trivialKotlin on github to add it back.*/
@Entity(tableName = "coin_asset")
class CoinAsset(private var coins: Int) : Entitlement() {
    /**
     * Gibberish below is excerpt from TrivialDrive kotlin sample
     * In order to exercise great control over how clients use the API, [setLevel] is made
     * private while keeping [getLevel] public. There is no idiomatic way to do this
     * in Kotlin for an [Entity] data class. So instead of going for "idiomatic", the favor is given
     * to "simple".  But in your own app feel free to go for idiomatic Kotlin.
     */
    fun getCoins() = coins
    override fun mayPurchase(): Boolean = coins < MAX_ASSET_COINS
    fun decrement(by: Int = 1) {
        coins -= by
    }
}
