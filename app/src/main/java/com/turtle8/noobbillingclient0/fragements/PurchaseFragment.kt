package com.turtle8.noobbillingclient0.fragements

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.turtle8.noobbillingclient0.R
import com.turtle8.noobbillingclient0.databinding.PurchaseFragmentBinding
import com.turtle8.noobbillingclient0.viewModels.PurchaseViewModel

class PurchaseFragment : Fragment() {

    companion object {
        const val LOG_TAG = "PurchaseFragment"
        fun newInstance() = PurchaseFragment()
    }

    private val viewModel:PurchaseViewModel by viewModels()

    private lateinit var binding:PurchaseFragmentBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PurchaseFragmentBinding.inflate(inflater)


        return binding.root
    }


}