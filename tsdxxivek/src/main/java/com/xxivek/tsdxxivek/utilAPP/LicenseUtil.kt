package com.xxivek.tsdxxivek.utilAPP

import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xxivek.tsdxxivek.LOGING
import com.xxivek.tsdxxivek.appLic
import com.xxivek.tsdxxivek.dataDB.UtilDB
import com.xxivek.tsdxxivek.itemDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.io.IOException
import com.xxivek.tsdxxivek.utilAPP.appendLog

class LicenseUtil :ViewModel(){
    // Данные по лицензии
    var appLIC:String
    var appPORT:String
    var appKONF:String
    var appDEB:String
    var appConnect1C:Int=0
    var appOper:String
    var appClient:String
    var appSumma:Double
    // Данные по ТСД
    var appInfoBD= MutableLiveData<Int>()
    var appInfoCountBD= MutableLiveData<Int>()
    var appInfoCountNotEmptyBD= MutableLiveData<Int>()
    var appInfoINPUT=MutableLiveData<Int>()
    var appInfoOUT=MutableLiveData<Int>()


    init{
        appLIC="-1"
        appPORT="0"
        appKONF="Не определена"
        appDEB="0"
        appConnect1C=0
        appOper=""
        appClient=""
        appSumma=0.0
        appInfoBD.value=0
        appInfoCountBD.value=0
        appInfoCountNotEmptyBD.value=0
        appInfoINPUT.value=0
        appInfoOUT.value=0
    }

    fun saveLic(mTextLic:String){

        if (mTextLic.startsWith("Lic=")){
            var start=mTextLic.substringAfter('=')
            appLIC=start.substringBefore(';')
            start=start.substringAfter('=')
            appPORT=start.substringBefore(';')
            start=start.substringAfter('=')
            appKONF=start.substringBefore(';')
            start=start.substringAfter('=')
            appDEB=start.substringBefore(';')
            if (appDEB=="0"){
                LOGING=false
            }
            appendLog("ViewModel appLIC",
                "Данные сопряжения по лицензии - "+appLIC+"\n" +
                    "Порт: "+appPORT+"\n" +
                    "Конфигурация: "+appKONF+"\n" +
                    "Логирование: "+appDEB+"\n" +
                    "Установлено соединение: "+appConnect1C+"\n" +
                    "Выполняемая операция: "+appOper+"\n" +
                    "Клиент: "+appClient+"\n" +
                    "Сумма: "+appSumma+"\n" +
                    "Состояние БД: "+appInfoBD.value+"\n" +
                    "Количество записей в БД: "+appInfoCountBD.value+"\n" +
                    "Количество записей с данными в БД: "+appInfoCountNotEmptyBD.value+"\n" +
                    "Наличие входящих данных: "+appInfoINPUT.value+"\n" +
                    "Наличие исходящих данных: "+appInfoOUT.value+"\n")
        }
    }
    fun getLiveDataInfoBD(): LiveData<Int> {
        return appInfoBD
    }
    fun getLiveDataInfoINPUT(): LiveData<Int> {
        return appInfoINPUT
    }
    fun setLiveDataInfoINPUT(mSize:Int) {
        val mCount=mSize / 7
        appInfoINPUT.postValue(mCount)
    }
    fun getLiveDataInfoOUT(): LiveData<Int> {
        return appInfoOUT
    }
    fun getLiveDataInfoCountBD(): LiveData<Int> {
        return appInfoCountBD
    }
    fun getLiveDataInfoCountNotEmptyBD(): LiveData<Int> {
        return appInfoCountNotEmptyBD
    }

    fun conditionInfo(){
        CoroutineScope(IO).launch{
            appendLog("ViewModel appLIC",
                "отправка запроса состояния БД - Количество записей: ")
            UtilDB().CountInfo()
            appendLog("ViewModel appLIC",
                "отправка запроса состояния БД - Количество не пустых записей: ")
//            UtilDB().CountNotEmptyInfo()
            var mCount=appLic.appInfoCountBD.value ?: 0
            var mCountNotEmpty=appLic.appInfoCountNotEmptyBD.value ?: 0
//            val job=launch {
//                try {
//                    appendLog("ViewModel appLIC",
//                        "отправка запроса состояния БД - Количество записей: ")
//                    mCount=itemDatabase?.getCount()!!
//                    appendLog("ViewModel appLIC",
//                        "Запрос состояния БД - Количество записей: "+mCount)
//                } catch (e: IOException) {
//                    appLic.appInfoBD.value=1
//                    appLic.appInfoCountBD.postValue(0)
//                    appendLog("ViewModel appLIC","Запрос состояния БД - Ошибка "+e)
////                    e.printStackTrace()
//                }
//            }
//            job.join()
//            val job2=launch {
//                try {
//                    mCountNotEmpty=itemDatabase?.getCountNotEmpty()!!
//                    appendLog("ViewModel appLIC",
//                        "Запрос состояния БД - Количество записей с данными: "+mCount)
//                 } catch (e: IOException) {
//                    appLic.appInfoBD.value=1
//                    appLic.appInfoCountNotEmptyBD.postValue(0)
//                    appendLog("ViewModel appLIC","Запрос состояния БД - Ошибка "+e)
//                }
//            }
//            job2.join()
//            appLic.appInfoCountBD.postValue(mCount)
//            appLic.appInfoCountNotEmptyBD.postValue(mCountNotEmpty)
            if (mCount>0){
                appLic.appInfoBD.postValue(3)
                if (mCountNotEmpty>0){
                    appLic.appInfoBD.postValue(2)
                }else{
                    appLic.appInfoBD.postValue(3)
                }
            }else{
                appLic.appInfoBD.postValue(0)
            }
            appendLog("ViewModel appLIC",
                "Запрос состояния ТСД - "+
                        "Номер лицензии: "+appLic.appLIC+"\n" +
                        "Порт: "+appLic.appPORT+"\n" +
                        "Конфигурация: "+appLic.appKONF+"\n" +
                        "Логирование: "+appLic.appDEB+"\n" +
                        "Установлено соединение: "+appLic.appConnect1C+"\n" +
                        "Выполняемая операция: "+appLic.appOper+"\n" +
                        "Клиент: "+appLic.appClient+"\n" +
                        "Сумма: "+appLic.appSumma+"\n" +
                        "Состояние БД: "+appLic.appInfoBD.value+"\n" +
                        "Количество записей в БД: "+appLic.appInfoCountBD.value+"\n" +
                        "Количество записей с данными в БД: "+appLic.appInfoCountNotEmptyBD.value+"\n" +
                        "Наличие входящих данных: "+appLic.appInfoINPUT.value+"\n" +
                        "Наличие исходящих данных: "+appLic.appInfoOUT.value+"\n")
        }
    }
}