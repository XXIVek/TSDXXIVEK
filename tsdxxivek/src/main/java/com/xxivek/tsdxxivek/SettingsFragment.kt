package com.xxivek.tsdxxivek

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.xxivek.tsdxxivek.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentSettingsBinding?=null
    val binding get() = _binding!!

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.bSetdb.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_itemListFragment)
        )
        binding.bSetconnect.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_connectFragment)
        )
        binding.bSetgeneral.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_setgeneralFragment)
        )
        if (appLic.appDEB=="1"){
            binding.bSetdb.visibility=View.VISIBLE
            binding.bSetconnect.visibility=View.VISIBLE
        }else{
            binding.bSetdb.visibility=View.GONE
            binding.bSetconnect.visibility=View.GONE
        }
    }
}