package com.xxivek.tsdxxivek.serverHTTP

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.databinding.FragmentConnectBinding


class ConnectFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentConnectBinding?=null
    val binding get() = _binding!!

//    private lateinit var prefs: SharedPreferences

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
     }

     override fun onResume() {
        super.onResume()
        // Выводим на экран данные из настроек
        binding.textPort.text=appLic.appPORT
        binding.textLic.text=appLic.appLIC
        var mTextConnect=""
        if (appLic.appConnect1C==0){
            mTextConnect="Отсутствует"
        }else{
            mTextConnect="Установлено"
        }
        binding.textConnect.text=mTextConnect
        binding.text2con.text=headingHTTP
    }
}