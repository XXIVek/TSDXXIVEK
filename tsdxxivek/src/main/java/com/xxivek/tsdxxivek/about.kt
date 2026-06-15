package com.xxivek.tsdxxivek

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xxivek.tsdxxivek.databinding.FragmentAboutBinding
import com.xxivek.tsdxxivek.databinding.FragmentMenuBinding

private var _binding: FragmentAboutBinding?=null
val binding get() = _binding!!

private lateinit var prefs: SharedPreferences

class about : Fragment() {

     override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
         _binding = FragmentAboutBinding.inflate(inflater, container, false)
         // Inflate the layout for this fragment
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        binding.checkBoxLog2.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                LOGING = true
            } else {
                LOGING = false
             }
        }
    }
    override fun onPause() {
        super.onPause()
        // Запоминаем данные
        val editor = prefs.edit()
        val mChek=binding.checkBoxLog2.isChecked
        editor.putBoolean(APP_PREF_LOGGING, mChek).apply()
    }
    override fun onResume() {
        super.onResume()
        // Устанавливаем ChekBox
        binding.checkBoxLog2.isChecked= LOGING
    }
}