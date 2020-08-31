package com.turtle8.noobbillingclient0.fragements

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.turtle8.noobbillingclient0.R
import com.turtle8.noobbillingclient0.databinding.OneFragmentBinding
import com.turtle8.noobbillingclient0.repositories.BillingRepository
import com.turtle8.noobbillingclient0.viewModels.OneViewModel
import com.turtle8.noobbillingclient0.viewModels.OneViewModelFactory

class OneFragment : Fragment() {

    companion object {
        const val LOG_TAG = "OneFragment"
        fun newInstance() = OneFragment()
    }

    //private lateinit var viewModel: OneViewModel
    private val viewModel:OneViewModel by viewModels()

    private lateinit var binding:OneFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG_TAG, "onCreate")
        val application = requireNotNull(this.activity).application
//        val viewModelFactory = OneViewModelFactory(
//            application
//        )
//        viewModel = ViewModelProvider(this, viewModelFactory)
//            .get(OneViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = OneFragmentBinding.inflate(inflater)
        viewModel.paidLiveData.observe(viewLifecycleOwner,
        Observer{
            if (it == null){
                Snackbar.make(requireView(), "observe:paidLiveData null",
                    Snackbar.LENGTH_SHORT).show()
                binding.oneFragmentText.text = "null"
            }else{
                Snackbar.make(requireView(), "observe:paidLiveData ${it.entitled.toString()}",
                    Snackbar.LENGTH_SHORT).show()
                binding.oneFragmentText.text = "paid"
            }
        })
        binding.inertPotButton.setOnClickListener{
            viewModel.devInsertPaidOneTime()
        }
        binding.deletePotButton.setOnClickListener{
            viewModel.devDeletePaidOneTime()
        }
        return binding.root
        //return inflater.inflate(R.layout.one_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        Log.d(LOG_TAG, "onActivityCreated...")
        super.onActivityCreated(savedInstanceState)
        //viewModel = ViewModelProviders.of(this).get(OneViewModel::class.java)
        // TODO: Use the ViewModel
    }

}