package com.xxivek.tsdxxivek

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.Navigation
import com.xxivek.tsdxxivek.databinding.FragmentLogoBinding

class LogoFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentLogoBinding?=null
    val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

//     override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentLogoBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (msg_server.isBlank()){
            appLic.appInfoINPUT.postValue(0)
        }
        if (msg_client.isBlank()){
            appLic.appInfoOUT.postValue(0)
        }
        if (appLic.appLIC=="-1"){
            if (DESIGN == 0) {
                binding.bNext.setOnClickListener (
                Navigation.createNavigateOnClickListener(R.id.action_logoFragment_to_scanerFragment))
            } else if (DESIGN == 1) {
                binding.bNext.setOnClickListener (
                Navigation.createNavigateOnClickListener(R.id.action_logoFragment_to_scanerFragment_d))
            }
        }else {
            binding.bNext.setOnClickListener(
                Navigation.createNavigateOnClickListener(R.id.action_logoFragment_to_menuFragment)
            )
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
        binding.radioGroupTopic.setOnCheckedChangeListener { rG, checkedId ->
            var mMODE = 0
            var mEdit = false
            if (checkedId == binding.radioButtonDay.id) {
                TOPIC = 0
                mMODE = AppCompatDelegate.MODE_NIGHT_NO
                mEdit = true
            } else if (checkedId == binding.radioButtonNight.id) {
                TOPIC = 1
                mMODE = AppCompatDelegate.MODE_NIGHT_YES
                mEdit = true
            }
            if (mEdit) {
                val editor = prefs.edit()
                editor.putInt(APP_PREF_TOPIC, TOPIC).apply()
                Thread.sleep(100)
                AppCompatDelegate.setDefaultNightMode(mMODE)
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }
        }
        binding.radioGroupDesign.setOnCheckedChangeListener {rG, checkedId ->
            if (checkedId==binding.radioButtonT.id){
                DESIGN=0
            }else if (checkedId==binding.radioButtonD.id) {
                DESIGN = 1
            }
            val editor = prefs.edit()
            editor.putInt(APP_PREF_DESIGN, DESIGN).apply()
        }
        binding.bLogoExit.setOnClickListener{MainActivity().appExit()}
        binding.buttonSite.setOnClickListener {
            val url = "http://ооо21век.рф/otraslevye-reshenija-xxi-vek/apk-tsd-1c/"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
        binding.buttonAbout.setOnClickListener (
            Navigation.createNavigateOnClickListener(R.id.action_logoFragment_to_about))

        if (appLic.appConnect1C==0){
            binding.bLogoPairing.visibility=View.GONE
        }else{
            binding.bLogoPairing .visibility=View.VISIBLE
        }
        binding.bLogoPairing.setOnClickListener {
            val editor = prefs.edit()
            editor.putString(APP_PREF_LIC, "-1").apply()
            editor.putString(APP_PREF_PORT, "0").apply()
            editor.putString(APP_PREF_KONF, "").apply()
            editor.putInt(APP_PREF_CONNECT1C, 0).apply()
            editor.putString(APP_PREF_ОPER, "").apply()
            editor.putString(APP_PREF_CLIENT, "").apply()
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
    }
    override fun onPause() {
        super.onPause()
        // Запоминаем данные
//        val editor = prefs.edit()
//        if (binding.radioButtonDay.isChecked){
//            TOPIC=0
//        }else{
//            TOPIC=1
//        }
//        if (binding.radioButtonT.isChecked){
//            DESIGN=0
//        }else{
//            DESIGN=1
//        }
//        editor.putInt(APP_PREF_TOPIC, TOPIC).apply()
//        editor.putInt(APP_PREF_DESIGN, DESIGN).apply()
    }
    override fun onResume() {
        super.onResume()
//        if (TOPIC==0){
//            binding.radioButtonDay.isChecked=true
//        }else{
//            binding.radioButtonNight.isChecked=true
//        }
//        if (DESIGN==0){
//            binding.radioButtonT.isChecked=true
//        }else{
//            binding.radioButtonD.isChecked=true
//        }
    }
}