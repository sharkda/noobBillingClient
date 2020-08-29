package com.turtle8.noobbillingclient0.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.turtle8.noobbillingclient0.localdb.PaidOneTime
import com.turtle8.noobbillingclient0.repositories.BillingRepository

class OneViewModel(private val repository:BillingRepository) :ViewModel() {

    val paidLiveData: LiveData<PaidOneTime>

    companion object{
        const val LOG_TAG = "OneViewModel"
    }

    init{
        Log.d(LOG_TAG, "init")
        paidLiveData = repository.paidLiveData
    }

    override fun onCleared() {
        Log.d(LOG_TAG, "onCleared")
        repository.endDataSourceConnections()
        super.onCleared()
    }
}