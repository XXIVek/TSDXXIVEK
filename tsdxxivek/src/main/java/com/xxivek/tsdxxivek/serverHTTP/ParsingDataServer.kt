package com.xxivek.tsdxxivek.serverHTTP

import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.dataXML.XMLDOMParser
import com.xxivek.tsdxxivek.utilAPP.createLog
import com.xxivek.tsdxxivek.utilAPP.appendLog
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.*

class ParsingDataServer {
    private val logTAG="ParsingDataServer"

    private var pars_TimeData=""
    private var pars_NameData=""
    private var pars_TipOper=""
    private var pars_Numberrecords=0
    private var pars_error=true
    private var pars_blok=0
    private var closeConnect=false

    private var frSubListXML: MutableList<String>
    private var subListXML: MutableList<String>
    private var nomFragmentsBlok=0

    init{
        frSubListXML= MutableList(100){""}
        subListXML= MutableList(100){""}
    }

    suspend fun parsingDataArryList():String{
        pars_TimeData=""
        pars_NameData=""
        pars_TipOper=""
        pars_error=true
        pars_blok=0
        closeConnect=false

        //var pars_ansver=""
        //var mSize=0

        //Чтение заголовков
        appendLog(logTAG, "Приступаем к парсингу Заголовка")
        var mSize=client_Data!!.size
        if (mSize>0) {
            val indexEndingData = mSize - 1
            val indexBeginningData = client_Data!!.indexOf("Content-Type: text/xml; charset=utf-8")+2
            for ((index, strData) in client_Data!!.withIndex()) {
                if (index == indexBeginningData) {
                    break
                }
                getData(strData)
            }
            getData(client_Data!!.get(indexEndingData))
            appendLog(logTAG, "Разобран заголовок. Операция - "+pars_TipOper)

            //Чтение тела запроса
            appendLog(logTAG, "Приступаем к парсингу Тела запроса")
            subListXML = client_Data!!.subList(indexBeginningData, indexEndingData)
            appendLog(logTAG, "Получено subListXML-" + subListXML.size)
            client_Data=null
            if (pars_TipOper == "xml_file_f_begin"){
                if (subListXML.size == pars_Numberrecords) {
                    frSubListXML = subListXML
                    nomFragmentsBlok = 1
                }else{
                    pars_error = true
                    appendLog(logTAG
                        , "Ошибка парсинга первого фрагмента. Ожидается - "+pars_Numberrecords+
                            "; Получено - "+subListXML.size)
                }
            }
            else if (pars_TipOper == "xml_file_f"){
                if(nomFragmentsBlok>0) {
                    if (subListXML.size == pars_Numberrecords) {
                        val resfrSubListXML_f = frSubListXML + subListXML
                        frSubListXML = resfrSubListXML_f.toMutableList()
//                        frSubListXML.addAll(subListXML.orEmpty())
                        nomFragmentsBlok++
                    } else {
                        pars_error = true
                        appendLog(
                            logTAG,
                            "Ошибка парсинга фрагмента. Ожидается - " + pars_Numberrecords +
                                    "; Получено - " + subListXML.size
                        )
                    }
                }else{
                    pars_error = true
                    appendLog(
                        logTAG,
                        "Ошибка парсинга фрагмента. Не загружен первый блок"
                    )
                }
            }
            else if (pars_TipOper == "xml_file_f_end"){
                if(nomFragmentsBlok>0) {
                    val resfrSubListXML = frSubListXML + subListXML
                    if (resfrSubListXML.size == (pars_Numberrecords)) {
                        nomFragmentsBlok++
                        msg_server = resfrSubListXML.joinToString(separator = "\n")
                        appLic.setLiveDataInfoINPUT(resfrSubListXML.size-4-((nomFragmentsBlok-2)*2))
                        if (msg_server.isBlank()){
                            appLic.appInfoINPUT.postValue(0)
                        }
                        subListXML.clear()
                        frSubListXML.clear()
                        closeConnect=true
                    } else {
                        pars_error = true
                        appendLog(
                            logTAG,
                            "Ошибка парсинга последнего фрагмента. Ожидается - " + pars_Numberrecords +
                                    "; Получено - " + subListXML.size
                        )
                    }
                }else{
                    pars_error = true
                    appendLog(
                        logTAG,
                        "Ошибка парсинга последнего фрагмента. Не загружен первый блок"
                    )
                }
            }else {
                if (subListXML.size == pars_Numberrecords) {
                    if (pars_TipOper == "xml_file") {
                        nomFragmentsBlok = 1
                        msg_server = subListXML.joinToString(separator = "\n")
                        appLic.setLiveDataInfoINPUT(subListXML.size-2)
                        if (msg_server.isBlank()){
                            appLic.appInfoINPUT.postValue(0)
                        }
                        subListXML.clear()
                        closeConnect=true
                    } else {
                        val textMessage = subListXML.joinToString(separator = "\n")
                        readInputXML(textMessage)
                        subListXML.clear()
                        closeConnect=true
                    }
                    appendLog(logTAG, "Окончание парсинга")
                } else {
                    appendLog(
                        logTAG, "Ошибка парсинга. Ожидается - " + pars_Numberrecords +
                                "; Получено - " + subListXML.size
                    )
                    pars_error = true
                }
            }
         }else{
            pars_error=true
            appendLog(logTAG
                , "Ошибка парсинга. Поступил пустой блок")
        }
        appendLog(logTAG,"Начало формирования ответа на полученные данные")
        if (pars_error) {
            return "400:"+pars_TipOper+":Ошибка запроса. Для данных ошибка в блоке - "+ nomFragmentsBlok
        } else{
            if (closeConnect){
                return "200:"+pars_TipOper+":Для данных принято блоков - "+ nomFragmentsBlok
            }else{
                return "201:"+pars_TipOper+":Продолжаем принято блоков - "+ nomFragmentsBlok
            }
        }
    }

    private fun getData(strLine:String){
        //В этой функции разбираем заголовок
        if (strLine=="") {
            appendLog(logTAG,"Заголовок разобран блок № "+pars_blok.toString())
            pars_blok=pars_blok+1
        }else{
            if ((pars_blok<3)&&(pars_blok>0)){
                //headingHTTP=headingHTTP+strLine+"\n"
                appendLog(logTAG,strLine)
            }
            if (strLine.startsWith("POST")) {
                if ((strLine.substring(7) == "HTTP/1.1") && (pars_blok == 0)) {
                    pars_error = false
                    pars_blok=pars_blok+1
                }
            }
            if ((pars_error == false)&&(pars_blok==2)) {
                if (strLine.startsWith("--")) {
                    pars_TimeData = strLine
                } else if (strLine.startsWith("Content-Disposition")) {
                    val fff: String = ("=" + '"')
                    var start = strLine.substringAfter(fff)
                    pars_NameData = start.substringBefore('"')
                    start = start.substringAfter(fff)
                    pars_TipOper = start.substringBefore('"')
                    start = start.substringAfter(fff)
                    val mNumberrecords = start.substringBefore('"')
                    pars_Numberrecords=mNumberrecords.toInt()
                 }
            }
            if (strLine == pars_TimeData + "--") {
                pars_TimeData = ""
                pars_error = false
            }
        }
    }


    private fun readInputXML(textMessage:String) {
        val parser = XMLDOMParser()
        val stream: InputStream?

        if (textMessage.isNotBlank()) {
            var lic=-99
            val condition=-99
            try {
                stream = textMessage.byteInputStream()
                val doc: Document? = parser.getDocument(stream)

                // Get elements by name employee
                val nodeDocuments: NodeList? = doc?.getElementsByTagName("Documents")
                for (i in 0 until nodeDocuments!!.getLength()) {
                    val e: Element = nodeDocuments.item(i) as Element
                    val appOper = e.getAttribute("Oper")
                    val appClient = e.getAttribute("Client")
                    if (appOper.isNotBlank()){
                        appLic.appOper=appOper
                    }
                    if (appClient.isNotBlank()){
                        appLic.appClient=appClient
                    }
                    val mlic=parser.getValue(e, "Pairing")
                    if (mlic!=""){lic=mlic.toInt()}
                    val mcondition=parser.getValue(e, "Condition")
                    if (mcondition=="OutFile=1"){
                        appLic.appInfoOUT.postValue(0)
                    }
                }
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            if (lic>-1){
                appLic.appConnect1C=1
                appendLog(logTAG,"Успешное сопряжение устройств")
            }
            if (condition>-1){
                appendLog(logTAG,"Получен запрос на состояние ТСД")
            }
        }
    }
}