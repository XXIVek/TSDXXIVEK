package com.xxivek.tsdxxivek

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.xxivek.tsdxxivek.databinding.DialogOperBinding

class OperDialog: DialogFragment() {
    // binding FragmentItemListBinding
    private var _binding: DialogOperBinding?=null
    val binding get() = _binding!!

    private var oper=""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = DialogOperBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.radioButton0.setOnClickListener{
            onCansel(0)
        }
        binding.radioButton1.setOnClickListener{
            onCansel(1)
        }
        binding.radioButton2.setOnClickListener{
            onCansel(2)
        }
        binding.radioButton3.setOnClickListener{
            onCansel(3)
        }
//        binding.bSettings.setOnClickListener(
//            Navigation.createNavigateOnClickListener(R.id.action_menuFragment_to_settingsFragment)
//        )
//        binding.bExit.setOnClickListener{MainActivity().appExit()}
//
//        binding.bInput.setOnClickListener{onInput()}
    }
    private fun onCansel(oper:Int){
        appLic.appOper=oper.toString()
        appLic.conditionInfo()
        dismiss()
    }
}