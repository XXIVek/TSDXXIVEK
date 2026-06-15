package com.xxivek.tsdxxivek

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.xxivek.tsdxxivek.serverHTTP.ServerSocketXXI
import com.xxivek.tsdxxivek.databinding.FragmentPairingBinding
import com.xxivek.tsdxxivek.utilAPP.LicenseUtil
import com.xxivek.tsdxxivek.utilAPP.appendLog
import kotlinx.coroutines.*

private lateinit var prefs: SharedPreferences

class PairingFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentPairingBinding?=null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         SERVERPORT= appLic.appPORT.toInt()
        if (SERVERPORT!!>0){
//            appLic.appConnect1C=false
            ServerSocketXXI().startServerClient()
        }
        // Подключаем базу данных с интерфейсом обработки данных
        appendLog("Сопряжение", "Подключаем базу данных с интерфейсом обработки данных")
        itemDatabase=(activity?.application as TSDXXIVekApplication).database.itemDao()
     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentPairingBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mMS=MainScope()
        prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        mMS.launch {waitingForPairing()}
    }

    suspend fun waitingForPairing()= coroutineScope{
        val job=launch {
            var k=50
            while (k>0){
                if (appLic.appConnect1C!=0){break}
                delay(1000L)
                k -= 1
            }
        }

        job.join()

//        try {
//            connectionSocket!!.close()
//            connectionSocket = null    //сокет соединения
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//        try {
//            serverSocket!!.close()
////            serverSocket = null  //сокет сервера
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
        if (appLic.appConnect1C==1){
            val editor = prefs.edit()
            editor.putString(APP_PREF_LIC, appLic.appLIC).apply()
            editor.putString(APP_PREF_PORT, appLic.appPORT).apply()
            editor.putString(APP_PREF_KONF, appLic.appKONF).apply()
            editor.putInt(APP_PREF_CONNECT1C, 2).apply()
            editor.putString(APP_PREF_ОPER, appLic.appOper).apply()
            editor.putString(APP_PREF_CLIENT, appLic.appClient).apply()
        }

        val navControler = binding.root.findNavController()
        if (appLic.appConnect1C!=0) {
            navControler.navigate(R.id.action_pairingFragment_to_menuFragment)
        }else{
            LicenseUtil().saveLic("Lic=-1;Port=0;Konf=XXI;")
            navControler.navigate(R.id.action_pairingFragment_to_logoFragment)
        }
    }
}