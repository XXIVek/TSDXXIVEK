package com.xxivek.tsdxxivek.serverHTTP


import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.dataXML.BuilderXML
import com.xxivek.tsdxxivek.utilAPP.createLog
import com.xxivek.tsdxxivek.utilAPP.appendLog
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.*
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*


class ServerSocketXXI {
    private val logTAG="ServerSocketXXI"

    private lateinit var reader: BufferedReader
    private lateinit var writer: OutputStream

    val pD:ParsingDataServer = ParsingDataServer()

    @OptIn(DelicateCoroutinesApi::class)
    fun startServerClient(){
        GlobalScope.launch(IO){
            if ((serverSocket == null) || (serverSocket!!.isClosed)) {
                val startServer=launch {
                    try {
                        serverSocket = ServerSocket(SERVERPORT!!.toInt())
                    } catch (e: IOException) {
                        appendLog(logTAG, "Ошибка создания сервера\n" + e.message)
                        //                        e.printStackTrace()
                    }
                    appendLog(
                        logTAG,
                        "Запущен сервер HTTP: " + serverSocket.toString()
                    )
                }
                startServer.join()
                while (this.isActive) {
                    connectionSocket = serverSocket!!.accept()
                    delay(100)
                    startClient()
                    appendLog(
                        logTAG,
                        "Создано соединение: " + connectionSocket.toString()
                    )
                }
            }
        }
     }

    fun startClient()= runBlocking(IO){
//        var connectionClosed=false
        client_Data=ArrayList<String>()
        reader = BufferedReader(InputStreamReader(connectionSocket!!.getInputStream()))
//        val reader: Scanner = Scanner(ClientSocket.getInputStream())
        writer = connectionSocket!!.getOutputStream()
        connectionSocket!!.soTimeout = 10000
        val tcpNoDelay = connectionSocket!!.tcpNoDelay
        connectionSocket!!.tcpNoDelay = !tcpNoDelay
        connectionSocket!!.keepAlive = true

 //       while (!connectionClosed){
        createLog(logTAG, "Начало загрузки данных в client_Data")
        read()
        createLog(logTAG, "Загружено в client_Data-" + client_Data!!.size)
        createLog(logTAG, "Данные отправлены для парсинга ")
        val parsAnswer = pD.parsingDataArryList()
        createLog(logTAG, "Подготавливаем ответ на запрос ")
            val answerKod = parsAnswer.substringBefore(":")
            val start=parsAnswer.substringAfter(":")
            val parsTipOper = start.substringBefore(":")
            val parsMessage=start.substringAfter(":")
//            if (answerKod!="201"){
//                connectionClosed=true
//            }
            sendAnswer(answerKod, parsTipOper,parsMessage)
 //       }
        try {
            if (connectionSocket != null) {
                if (!connectionSocket!!.isClosed) {
                    connectionSocket!!.close()
                    createLog(logTAG, "HTTP клиент остановлен")
                }
            }
        } catch (e: IOException) {
            createLog(logTAG, "Ошибка остановки HTTP клиента")
        }
//             sendAnswer("200","Pairing")
    }

    private fun read(){
         while (reader.ready()){
            client_Data!!.add(reader.readLine())
        }
     }

    private fun sendAnswer(ansverCod:String,nameData:String,pars_message:String){
        // Отправка данных
        createLog(logTAG, "Начало отправки сообщения клиенту")
        val ansverData = ansverHTTP(ansverCod, nameData,pars_message)

        try {
            /* отправляем на сервер данные */
            writer.write(ansverData.toByteArray())
            writer.flush()
            createLog(logTAG, "Сообщения клиенту отправлено")
        } catch (e: Exception) {
            createLog(logTAG, "Ошибка отправки данных клиенту\n" + e.message)
        }
    }

    private fun ansverHTTP(ansver_cod:String,mNameOper:String,pars_message:String):String{

        var ansverText=""
        val textDate=setDateTime()
//        ConditionViewModel().runConditionTSD()

        val mTextXML=createXML(mNameOper,pars_message)
//        var utf8: ByteArray? = null
        var mTextXMLlength = 0
        try {
            var utf8 = mTextXML.toByteArray()//getBytes("UTF-8")
            mTextXMLlength = utf8.size
        } catch (ex: UnsupportedEncodingException) {
            ex.printStackTrace()
        }
        if ((ansver_cod=="200")||(ansver_cod=="201")){
            ansverText += "HTTP/1.1 200 OK \n"
            createLog(logTAG,"Ответ: 200 Операция выполнена успешно")
        }else if (ansver_cod=="400"){
            ansverText += "HTTP/1.1 400 Bad Request \n"
            createLog(logTAG,"Ответ: 400 Ошибка: Неверный запрос. "+pars_message)
        }
        ansverText += textDate+" \n"
        ansverText += "Server: Android \n"
        ansverText += "Content-Length: "+mTextXMLlength.toString()+" \n"
        ansverText += "Content-Type: text/html \n"
        if (ansver_cod=="201"){
            ansverText += "Connection: keep-alive \n"
        }else {
            ansverText += "Connection: Closed \n"
        }
        ansverText += "\n"
        ansverText += mTextXML
        return ansverText
    }
    private fun setDateTime():String {
        var textDateTime="Date: "
        val timestamp = System.currentTimeMillis()
        val formatter= SimpleDateFormat("E, dd LLL yyyy HH:MM:SS")
        textDateTime =textDateTime+ formatter.format(Date(timestamp))+" GMT"
        return textDateTime
    }
    private fun createXML(nameOper:String,pars_message:String):String{
        var mTextXML=""
        val builderXML= BuilderXML()
        if (nameOper=="Pairing") {
            builderXML.beginningElementXXI("Answers")
            builderXML.beginningElementXXI("Pairing")
            builderXML.beginningElementXXI("NomLic")
            builderXML.writeTextXXI(appLic.appLIC)
            builderXML.endElementXXI("NomLic")
            builderXML.endElementXXI("Pairing")
            builderXML.endElementXXI("Answers")
            mTextXML = builderXML.getTextXMLXXI()
        }
        else if ((nameOper=="xml_file")||(nameOper=="xml_file_f_begin")||
            (nameOper=="xml_file_f")||(nameOper=="xml_file_f_end")) {
            builderXML.beginningElementXXI("Answers")
            builderXML.beginningElementXXI("xml_file")
            builderXML.writeTextXXI(""+pars_message)
            builderXML.endElementXXI("xml_file")
            builderXML.endElementXXI("Answers")
            mTextXML = builderXML.getTextXMLXXI()
        }
        else if (nameOper=="Condition"){
            appLic.conditionInfo()
            builderXML.beginningElementXXI("Answers")
                builderXML.beginningElementXXI("Condition")
                    builderXML.beginningElementXXI("Lic")
                        builderXML.writeTextXXI(appLic.appLIC)
                    builderXML.endElementXXI("Lic")
                    builderXML.beginningElementXXI("Konf")
                        builderXML.writeTextXXI(appLic.appKONF)
                    builderXML.endElementXXI("Konf")
                    builderXML.beginningElementXXI("BD")
                        builderXML.writeTextXXI(appLic.appInfoBD.value.toString())
                    builderXML.endElementXXI("BD")
                    builderXML.beginningElementXXI("Input")
                       builderXML.writeTextXXI(appLic.appInfoINPUT.value.toString())
                    builderXML.endElementXXI("Input")
                    builderXML.beginningElementXXI("Output")
                       builderXML.writeTextXXI(appLic.appInfoOUT.value.toString())
                    builderXML.endElementXXI("Output")
                builderXML.endElementXXI("Condition")
            builderXML.endElementXXI("Answers")
            mTextXML = builderXML.getTextXMLXXI()
        }
        else if (nameOper=="OutFile"){
            mTextXML= msg_client
            msg_client =""
        }
        return mTextXML
    }
}