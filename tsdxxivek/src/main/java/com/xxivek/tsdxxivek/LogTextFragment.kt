package com.xxivek.tsdxxivek

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xxivek.tsdxxivek.databinding.FragmentLogTextBinding

class LogTextFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentLogTextBinding?=null
    val binding get() = _binding!!

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentLogTextBinding.inflate(inflater, container, false)

        return binding.root
    }
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
//        binding.bOpenLog.setOnClickListener(
//            Navigation.createNavigateOnClickListener(R.id.action_setgeneralFragment_to_logTextFragment)
//        )
//        binding.bSetconnect.setOnClickListener(
//            Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_connectFragment)
//        )
//        binding.bSetgeneral.setOnClickListener(
//            Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_setgeneralFragment)
//        )
//    }

    override fun onResume() {
        super.onResume()
        binding.textLog.text = msg_log
    }
}