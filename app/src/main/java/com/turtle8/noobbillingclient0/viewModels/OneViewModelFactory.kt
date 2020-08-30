package com.turtle8.noobbillingclient0.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.turtle8.noobbillingclient0.repositories.BillingRepository
import java.lang.IllegalArgumentException

class OneViewModelFactory(
    private val application: Application,
) : ViewModelProvider.AndroidViewModelFactory(application) {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OneViewModel::class.java)) {
            return OneViewModel(application) as T
        }
        throw IllegalArgumentException("unknown viewModel class")
    }
}