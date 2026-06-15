package com.xxivek.tsdxxivek

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import com.xxivek.tsdxxivek.databinding.FragmentSetgeneralBinding

class SetgeneralFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentSetgeneralBinding?=null
    val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentSetgeneralBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        binding.bOpenLog.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_setgeneralFragment_to_logTextFragment)
        )
        binding.checkBoxLog.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                LOGING = true
                binding.bOpenLog.visibility = View.VISIBLE
            } else {
                LOGING = false
                binding.bOpenLog.visibility = View.GONE
            }
        }
        binding.radioGroupTopic.setOnCheckedChangeListener {rG, checkedId ->
            if (checkedId==binding.radioButtonDay.id){
               TOPIC=0
            }else if (checkedId==binding.radioButtonNight.id) {
                TOPIC = 1
            }
        }
        binding.radioGroupDesign.setOnCheckedChangeListener {rG, checkedId ->
            if (checkedId==binding.radioButtonT.id){
                DESIGN=0
            }else if (checkedId==binding.radioButtonD.id) {
                DESIGN = 1
            }
        }

    }

    override fun onPause() {
        super.onPause()
        // Запоминаем данные
        val editor = prefs.edit()
        val mChek=binding.checkBoxLog.isChecked
        if (binding.radioButtonDay.isChecked){
           TOPIC=0
        }else{
            TOPIC=1
        }
        if (binding.radioButtonT.isChecked){
            DESIGN=0
        }else{
            DESIGN=1
        }
        editor.putBoolean(APP_PREF_LOGGING, mChek).apply()
        editor.putInt(APP_PREF_TOPIC, TOPIC).apply()
        editor.putInt(APP_PREF_DESIGN, DESIGN).apply()
    }
    override fun onResume() {
        super.onResume()
        // Устанавливаем ChekBox
        binding.checkBoxLog.isChecked= LOGING

        if (LOGING){
            binding.bOpenLog.visibility=View.VISIBLE
        }
        else{
            binding.bOpenLog.visibility=View.GONE
        }
        if (TOPIC==0){
            binding.radioButtonDay.isChecked=true
        }else{
            binding.radioButtonNight.isChecked=true
        }
        if (DESIGN==0){
            binding.radioButtonT.isChecked=true
        }else{
            binding.radioButtonD.isChecked=true
        }
    }
}