package com.turtle8.noobbillingclient0.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.turtle8.noobbillingclient0.localdb.PaidOneTime
import com.turtle8.noobbillingclient0.repositories.BillingRepository

class OneViewModel(application:Application) :AndroidViewModel(application) {

    val paidLiveData: LiveData<PaidOneTime>
    private val repository: BillingRepository

    companion object{
        const val LOG_TAG = "OneViewModel"
    }

    init{
        Log.d(LOG_TAG, "init")
        repository = BillingRepository.getInstance(application)
        repository.startDataSourceConnections()
        paidLiveData = repository.paidLiveData
    }

    override fun onCleared() {
        Log.d(LOG_TAG, "onCleared")
        //repository.endDataSourceConnections()
        super.onCleared()
    }
}