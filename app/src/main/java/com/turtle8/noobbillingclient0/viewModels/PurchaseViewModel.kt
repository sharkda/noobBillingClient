package com.turtle8.noobbillingclient0.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turtle8.noobbillingclient0.localdb.PaidOneTime
import com.turtle8.noobbillingclient0.repositories.BillingRepository
import kotlinx.coroutines.launch

class PurchaseViewModel(application:Application)
    : AndroidViewModel(application) {

    val paidLiveData : LiveData<PaidOneTime>
    private val repository: BillingRepository

    init{
        Log.d(LOG_TAG,"init")
        repository = BillingRepository.getInstance(application)
        repository.startDataSourceConnections()
        paidLiveData = repository.paidLiveData
    }

    fun devInsertPaidOneTime(){
        viewModelScope.launch() { repository.devInsertPaidOneTime() }

    }
    fun devDeletePaidOneTime(){
        viewModelScope.launch() {
            repository.devDelPaidOneTime()
        }

    }

    fun addCoins(){
        viewModelScope.launch {
            //TODO:
        }
    }

    companion object{
        const val LOG_TAG = "PurchaseViewModel"
    }
}