package com.turtle8.noobbillingclient0.fragements

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.turtle8.noobbillingclient0.R
import com.turtle8.noobbillingclient0.viewModels.OneViewModel

class OneFragment : Fragment() {

    companion object {
        fun newInstance() = OneFragment()
    }

    private lateinit var viewModel: OneViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.one_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(OneViewModel::class.java)
        // TODO: Use the ViewModel
    }

}