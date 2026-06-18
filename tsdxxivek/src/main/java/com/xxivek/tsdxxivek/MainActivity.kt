package com.xxivek.tsdxxivek

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.xxivek.tsdxxivek.dataDB.ItemDao
import com.xxivek.tsdxxivek.databinding.ActivityMainBinding
import com.xxivek.tsdxxivek.utilAPP.LicenseUtil
import com.xxivek.tsdxxivek.utilAPP.appendLog
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess

lateinit var appLic:LicenseUtil

//APP_PREF_LIC - номер лицензии
const val APP_PREF_LIC = "license"

//APP_PREF_PORT - номер порта сервера
const val APP_PREF_PORT = "port"

//APP_PREF_KONF - вариант конфигурации ТСД
const val APP_PREF_KONF = "configuration"

//APP_PREF_CONNECT1C - сопряжение установлено
const val APP_PREF_CONNECT1C = "connct1c"

//APP_PREF_ОPER - выполняемая операция
const val APP_PREF_ОPER = "operition"

//APP_PREF_CLIENT - контрагент
const val APP_PREF_CLIENT = "client"

//APP_PREF_LOGGING - включить логирование в файл
const val APP_PREF_LOGGING = "logging"
var LOGING=false

//APP_PREF_TOPIC - день/ночь
const val APP_PREF_TOPIC = "topic"
var TOPIC=0

//APP_PREF_DESIGN - вариант сканера ТСД
const val APP_PREF_DESIGN = "design"
var DESIGN=0

//APP_PREF_USE_WEBSITE - использовать внешний сайт для обмена
const val APP_PREF_USE_WEBSITE = "use_website"
var USE_WEBSITE=false

var msg_log:String?=null

// Сервер HTTP
var serverSocket: ServerSocket? = null  //сокет сервера
var SERVERPORT: Int?=null
var client_Data:ArrayList<String>?=null

// Клиент HTTP
var connectionSocket:Socket?=null

lateinit var msg_server:String
lateinit var msg_client:String
lateinit var headingHTTP:String

// База данных
// Интерфейс доступа к базе данных
var itemDatabase: ItemDao?=null

class MainActivity : AppCompatActivity() {
    //Позволяет отслеживать навигацию по экранам
 //   lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        // Устанавливаем начальные значения в appLic
        appLic= ViewModelProvider(this).get(LicenseUtil::class.java)

        prefs = this.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if(prefs.contains(APP_PREF_LIC)){
            appLic.appLIC = prefs.getString(APP_PREF_LIC,"-1")!!
        }
        if(prefs.contains(APP_PREF_PORT)){
            appLic.appPORT = prefs.getString(APP_PREF_PORT,"0")!!
        }
        if(prefs.contains(APP_PREF_KONF)){
            appLic.appKONF = prefs.getString(APP_PREF_KONF,"Не определена")!!
        }
        if(prefs.contains(APP_PREF_CONNECT1C)){
            appLic.appConnect1C = prefs.getInt(APP_PREF_CONNECT1C,0)
        }
        if(prefs.contains(APP_PREF_ОPER)){
            appLic.appOper = prefs.getString(APP_PREF_ОPER,"")!!
        }
        if(prefs.contains(APP_PREF_CLIENT)){
            appLic.appClient = prefs.getString(APP_PREF_CLIENT,"")!!
        }
        if(prefs.contains(APP_PREF_LOGGING)){
            LOGING = prefs.getBoolean(APP_PREF_LOGGING,false)
        }
        if(prefs.contains(APP_PREF_TOPIC)){
            TOPIC = prefs.getInt(APP_PREF_TOPIC,0)
        }
        if(prefs.contains(APP_PREF_DESIGN)){
            DESIGN = prefs.getInt(APP_PREF_DESIGN,0)
        }
        if(prefs.contains(APP_PREF_USE_WEBSITE)){
            USE_WEBSITE = prefs.getBoolean(APP_PREF_USE_WEBSITE,false)
        }
        apptheme()

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)

        msg_server=""
        msg_client=""
        headingHTTP=""

//        // Подключаем базу данных с интерфейсом обработки данных
//        itemDatabase=(application as TSDXXIVekApplication).database.itemDao()
//        appendLog("MainActivity", "Подключаем базу данных с интерфейсом обработки данных")



//        // Извлекаем NavController из фрагмента узла навигации. В данном случае за основу
//        // берем фрагмент навигации nav_host_fragment
//        val navHostFragment = supportFragmentManager
//            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
//        navController = navHostFragment.navController
//        // Настраиваем панель действий для использования с NavController
//        NavigationUI.setupActionBarWithNavController(this, navController)

//        val prefs = binding.root.context.getSharedPreferences("settings", Context.MODE_PRIVATE)
//        if(prefs.contains(APP_PREF_LOGGING)){
//            // Получаем число из настроек
////            LOGGING= prefs.getBoolean(APP_PREF_LOGGING,false)
//        }else{
////            LOGGING=false
//        }
        if (isCameraPermissionGranted()) {
            //                   bindCameraUseCases()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA_REQUEST
            )
        }
    }

    //Предоставлено ли Разрешение На Камеру
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED //проверяем разрешене на камеру
    }


    /**
     * Управление навигацией, когда пользователь выбирает "Вверх" на панели действий.
     */
//    override fun onSupportNavigateUp(): Boolean {
//        return navController.navigateUp() || super.onSupportNavigateUp()
//    }

    fun appExit(){
        try {
            if (serverSocket!=null){
                if (!serverSocket!!.isClosed){
                    serverSocket!!.close()
                    appendLog("MainActivity", "HTTP Сервер остановлен")
                }
            }
        } catch (e: IOException) {
            appendLog("MainActivity","Ошибка остановки HTTP сервера")
        }
        try {
            if (connectionSocket!=null){
                if (!connectionSocket!!.isClosed){
                    connectionSocket!!.close()
                    appendLog("MainActivity","HTTP клиент остановлен")
                }
            }
        } catch (e: IOException) {
            appendLog("MainActivity","Ошибка остановки HTTP клиента")
        }
//        finishAndRemoveTask()
        exitProcess(-1)
    }

    fun apptheme(){
        if (TOPIC==0){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    override fun onDestroy() {
        appExit()
        super.onDestroy()
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1
    }
}