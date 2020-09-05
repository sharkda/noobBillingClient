package com.turtle8.noobbillingclient0.fragements

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.turtle8.noobbillingclient0.R
import com.turtle8.noobbillingclient0.databinding.FragmentPortalBinding
import java.lang.Exception

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [portalFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class portalFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding:FragmentPortalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPortalBinding.inflate(inflater)
        binding.navToOneButton.setOnClickListener{
            try{
                this.findNavController().navigate(R.id.action_portalFragment_to_oneFragment)
            }catch(e: Exception){
                Log.e(LOG_TAG, e.toString())
            }

        }
        binding.navToPurchaseButton.setOnClickListener{
            try{
                this.findNavController().navigate(R.id.action_portalFragment_to_purchaseFragment)
            }catch(e: Exception){
                Log.e(LOG_TAG, e.toString())
            }

        }
        return binding.root
    }

    companion object {
       const val LOG_TAG = "PortalFragment"
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            portalFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}