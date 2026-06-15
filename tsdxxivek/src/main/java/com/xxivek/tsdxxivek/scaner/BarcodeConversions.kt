package com.xxivek.tsdxxivek.scaner

import androidx.core.text.isDigitsOnly
import kotlin.math.pow

class BarcodeConversions {

    //9 и 10 сигареты у XXI Век в базе
    //7 DataMatrix у самеры при чтении
    fun conversions(cameraSh: String, cameraShTip: Int): Map<String, String> {
//        val aaa="6951275300058"
//        val resultCheckSh = checkSh(aaa, cameraShTip)
        val resultCheckSh = checkSh(cameraSh, cameraShTip)
        val mShtrih = resultCheckSh["shtrih"]!!
        val mShtrihTip = resultCheckSh["shtrihTip"]!!
        val mAccept = resultCheckSh["accept"]!!
        return mapOf("shtrih" to mShtrih, "shtrihTip" to mShtrihTip, "accept" to mAccept)
    }
    private fun checkSh(cameraSh: String, cameraShTip: Int): Map<String, String> {
        val checkResultMarking = checkingShForMarking(cameraSh)
        val mShtrih = checkResultMarking["shtrih"]!!
        val thisDataMatrix = checkResultMarking["thisDataMatrix"]!!
        val shdisassembled = checkResultMarking["shdisassembled"]!!
        var mShtrihTip = cameraShTip.toString()
        var mAccept = "0"
        if (shdisassembled == "1") {
            if (thisDataMatrix == "1") {// Это ДатаМатрикс
                mAccept = "1"
            }else if (thisDataMatrix == "3") {// Это Марка алкоголь
                mAccept = "1"
            } else {// Это простой штрихкод или акцизная марка
                if (checkEAN(cameraSh)) {
                    mAccept = "1"
                }
            }
        }
        return mapOf(
            "shtrih" to mShtrih,
            "shtrihTip" to mShtrihTip, "accept" to mAccept
        )
    }
    private fun checkingShForMarking(tekShtrih: String): Map<String, String> {
        // znDopInf Значения ДопИнф: 0 - Неопределено,
        // thisDataMatrix ЭтоДатаМатрикс: 0-не ДатаМатрикс, 1-Сигареты, 2-Блок сигарет, обувь, легпром и т.д.

        var znDopInf = 0
        var thisDataMatrix = "0"
        var mShdisassembled = "0"
        var mShtrih = ""
        var mMRC = 0.0
        val lengthSh = tekShtrih.length

        if ((lengthSh == 25) || (lengthSh == 29)) {//Код товара в формате Data Matrix маркировки табачной продукции.
//            Если ПроверкаСтроки(ШтриховойКодТовара, Истина, Истина, Истина, "«!”""%&’'()*+-.,/_:;=<>?»") Тогда
            val mGTIN = strLeft(tekShtrih, 14)// Получаем GTIN
            var sernom = strAverage(tekShtrih, 15, 7)
            var mShEAN = ""
            if (checkEAN(strRight(mGTIN, 8))) {
                mShEAN = strRight(mGTIN, 8)
            } else if (checkEAN(strRight(mGTIN, 13))) {
                mShEAN = strRight(mGTIN, 13)
            }
            mMRC = markMRC(tekShtrih, 29)
            mShtrih = if (mMRC > 0) {
                val mCenaInt=mMRC.toInt()
                val mCena = strRight(("000000" + mCenaInt * 1000), 6)
                mShEAN + "_" + mCena
            } else {
                mShEAN
            }
            thisDataMatrix = "1"
            mShdisassembled = "1"
        } else if (lengthSh < 14) {
            mShtrih = tekShtrih;
            thisDataMatrix = "0"
            mShdisassembled = "1"
        } else if (lengthSh == 150) {
            mShtrih = tekShtrih;
            thisDataMatrix = "3"
            mShdisassembled = "1"
        } else if (lengthSh == 68) {
            mShtrih = tekShtrih;
            thisDataMatrix = "3"
            mShdisassembled = "1"
        } else if (tekShtrih.startsWith("01")) {//  Штрихкод Datamatrix содержит идентификаторы применения (AI).
            val mGTIN = strAverage(tekShtrih, 3, 14)
            var mShEAN = ""
            if (checkEAN(strRight(mGTIN, 8))) {
                mShEAN = strRight(mGTIN, 8)
            } else if (checkEAN(strRight(mGTIN, 13))) {
                mShEAN = strRight(mGTIN, 13)
            }

            var sernom = ""

            if (strAverage(tekShtrih, 17, 2) == "21") {// идентификатор серийного номера
                val strData = strAverage(tekShtrih, 19)
                if (strData.length > 13) {
                    val start8005 = strFind(strData, "8005")
                    if (start8005 > 0) {//Это сигареты
                        //серийный номер состоит из 7 символов
                        sernom = strAverage(strData, 1, 7)
                        val nextGroop4 = strAverage(strData, 8, 4)
                        if (nextGroop4 == "8005") {
                            val strmMRC = strAverage(strData, 12, 6)
                            if (isNumericXXI(strmMRC)) {
                                mMRC = strmMRC.toDouble()
                            }
                        }
                        mShtrih = if (mMRC > 0) {
                            val mCenaInt=mMRC.toInt()
                            val mCena = strRight(("000000" + mCenaInt * 1000), 6)
                            mShEAN + "_" + mCena
                        } else {
                            mShEAN
                        }
                        thisDataMatrix = "1"
                        mShdisassembled = "1"
                    } else {//Это что то другое
                        // Предполагаем что серийный номер состоит из 13 символов
                        val idKlPr = "91" // идентификатор ключ проверки
                        val idKodPr = "92" // ИдентификаторКодаПроверки
                        val idKodTNVED = "240" // ИдентификаторКодаТНВЭД
                        sernom = strAverage(strData, 1, 13)
                        val nextGroop2 = strAverage(strData, 14, 2)
                        val nextGroop3 = strAverage(strData, 14, 3)
                        val nextGroop4 = strAverage(strData, 14, 4)
                        if (nextGroop2 == idKlPr || nextGroop2 == idKodPr || nextGroop2 == "17" ||
                            nextGroop4 == "7003" || nextGroop3 == idKodTNVED
                        ) {
                            mShtrih =mShEAN
                            thisDataMatrix = "1"
                            mShdisassembled = "1"
                        }
                    }
                }else{
                    mShtrih =mShEAN
                    sernom = strAverage(strData, 1)
                    thisDataMatrix = "1"
                    mShdisassembled = "1"
                }
            }
        } else {//Дата Матрикс блоков сигарет и всей отстальной маркировки.

            val codesGS1=listCodesGS1()
            var shtrihData = createResultStructure()

            val idGTIN = "01" // идентификатор GTIN
            val idSn = "21" // идентификатор серийного номера

            val sepGS1FNC1 = Char(232) // Dec 232 РазделительGS1_FNC1
            val sepGS1 = Char(29) // Dec 29 РазделительGS1
            val ekrCharGS1 = "\\x1d" // Используется для экранирования символа GS1.

            val idxGS1FNC1 = tekShtrih.indexOf(sepGS1FNC1)+1 // ПозицияРазделителяGS1_FNC1
            val idxGS1 = tekShtrih.indexOf(sepGS1)+1 // ПозицияРазделителяGS1
            val idxCharGS1 = tekShtrih.indexOf(ekrCharGS1)+1 // ПозицияРазделителяЭкран

            if (tekShtrih.startsWith("(")) {
                // Штрихкод по стандарту GS1 в HRI виде.
                shtrihData = areBracketsInBarcode(tekShtrih, shtrihData, codesGS1)
            }

            if (shtrihData["Shdisassembled"]?.get("mValio") == "0") {
                // Штрихкод по стандарту GS1 присутствует символ разделитель GS1 (Dec 29).
                if ((idxGS1FNC1 > 0) || (idxGS1 > 0) || (idxCharGS1 > 0)) {
                    //            ЧастиШтрихкода = СтрРазделить(ШтриховойКодТовара, РазделительGS1, Ложь);
                    val partsSh: List<String> =
                        if (idxCharGS1 > 0) tekShtrih.split(ekrCharGS1) else tekShtrih.split(sepGS1)
                    for (partWithoutSeparators in partsSh) {
                        shtrihData = parseBarcodeString(partWithoutSeparators, shtrihData, codesGS1)
                    }
                }
 //               var aaa=1
            }

            if ((shtrihData["Shdisassembled"]?.get("mValio") == "1")) {
                // Штрихкод по стандарту GS1
 //               var mValioGS1 = shtrihData[idGTIN]!!
                var mValioGS1=shtrihData.getOrElse(idGTIN) {mutableMapOf()}
                val mGTIN=if (mValioGS1.isNotEmpty()){mValioGS1["mValio"]!!}else{""}
                mValioGS1 = shtrihData.getOrElse(idSn) {mutableMapOf()}
                var sernom =if (mValioGS1.isNotEmpty()){mValioGS1["mValio"]!!}else{""}

                var mShEAN = ""
                if (checkEAN(strRight(mGTIN, 8))) {
                    mShEAN = strRight(mGTIN, 8)
                } else if (checkEAN(strRight(mGTIN, 13))) {
                    mShEAN = strRight(mGTIN, 13)
                }

                mValioGS1 =shtrihData.getOrElse("mMRC") { kotlin.collections.mutableMapOf() }
                mMRC = if (mValioGS1.isNotEmpty()){mValioGS1["mValio"]!!.toDouble()}else{0.0}
                mShtrih = if (mMRC > 0) {
                    val mCenaInt=mMRC.toInt()
                    val mCena = strRight(("000000" + mCenaInt * 1000), 6)
                    mShEAN + "_" + mCena
                } else {
                    mShEAN
                }
                thisDataMatrix = "1"
                mShdisassembled = "1"
            }
        }
        return mapOf(
            "shtrih" to mShtrih,
            "thisDataMatrix" to thisDataMatrix,
            "shdisassembled" to mShdisassembled
        )
    }
    private fun areBracketsInBarcode (tekShtrih:String,shtrihData:MutableMap<String,MutableMap<String,String>>,
          codesGS1:MutableMap<String,MutableMap<String,String>>):MutableMap<String,MutableMap<String,String>>{

        val tekShtrihLength=tekShtrih.length
        val minLengthGS1=2
        val maxLengthGS1=4

        var numChar=1
        var mMRC=0.0

        while (numChar <=tekShtrihLength) {
            if (strAverage(tekShtrih,numChar,1)!="(") {
                return shtrihData
            }

            numChar++

            val positionGS1=strFind(tekShtrih,")",true,numChar,1)

            if (positionGS1==0) {
                return shtrihData
            }

            val mGS1=strAverage(tekShtrih,numChar,positionGS1-numChar)
            val mGS1Length=mGS1.length
            if ((mGS1Length<minLengthGS1)||(mGS1Length>maxLengthGS1)){
                return shtrihData
            }
            var strDecimalPointPosition=""
            var codeDescription=codesGS1.getOrElse(mGS1) {mutableMapOf()
            }
            if (codeDescription.isEmpty()){
                if (mGS1Length==maxLengthGS1){
                    codeDescription=codesGS1.getOrElse(strLeft(mGS1,maxLengthGS1-1)) {mutableMapOf()}
                }
                strDecimalPointPosition=strRight(mGS1,1)
            }
            if (codeDescription.isEmpty()){
                return shtrihData
            }
            numChar++

            var mValio=""
            val fixedLength=codeDescription["fixedLength"]?.toInt()!!
            val fixedValueType=codeDescription["fixedValueType"]!!
            if (fixedLength>0) {
                mValio=strAverage(tekShtrih,numChar,fixedLength)
                if (mValio.length!=fixedLength){
                    return shtrihData
                }
                if (fixedValueType=="N"){
                    if (!isNumericXXI(mValio)) {
                        return shtrihData
                    }
                }
                numChar += fixedLength
            }
            val variableLength=codeDescription["variableLength"]?.toInt()!!
            val variableValueType=codeDescription["variableValueType"]!!
            if ((variableLength>0)&&(positionGS1<tekShtrihLength)){
                var nextPositionGS1=strFind(tekShtrih,"(",true,numChar,1)
                var correctGS1=false
                while ((nextPositionGS1>0)&&(!correctGS1)){
                    val closePositionGS1=strFind(tekShtrih,")",true,nextPositionGS1,1)
                    val assumedGS1=strAverage(tekShtrih,nextPositionGS1+1,closePositionGS1-nextPositionGS1-1)
                    correctGS1=(assumedGS1.length>1)&&(assumedGS1.length<5)&&(isNumericXXI(assumedGS1))
                    if (nextPositionGS1>=tekShtrihLength){
                        nextPositionGS1=0
                    }else if (!correctGS1){
                        nextPositionGS1=strFind(tekShtrih,"(",true,nextPositionGS1+1,1)
                    }
                }
                val mValioVariable = if (nextPositionGS1>0) {
                    strAverage(tekShtrih, numChar, nextPositionGS1 - numChar)
                }else{
                    strAverage(tekShtrih, numChar)
                }
                if (mValioVariable.length>variableLength){
                    return shtrihData
                }
                if (variableValueType=="N"){
                    if (!isNumericXXI(mValioVariable)) {
                        return shtrihData
                    }
                }
                numChar += mValioVariable.length
                mValio += mValioVariable
            }
            var decimalPointPosition=0
            if (strDecimalPointPosition.isNotBlank()) {
                decimalPointPosition = strDecimalPointPosition.toInt()
                if (decimalPointPosition>0) {
                    for (i in 0..(decimalPointPosition-mValio.length)) {
                        mValio= "0$mValio"
                    }
                    mValio=strLeft(mValio,mValio.length-decimalPointPosition)+ "." +
                            strRight(mValio,decimalPointPosition)
                }
            }
            val dataDescription=mutableMapOf("decimalPointPosition" to decimalPointPosition.toString(),
                "mValio" to mValio)
            shtrihData[mGS1] = dataDescription

            if (mGS1=="8005") {
                mMRC=(strLeft(mValio,3)+"."+strRight(mValio,3)).toDouble()
            }
        }
        if (mMRC>0){
            val dataDescription=mutableMapOf("decimalPointPosition" to "0",
                "mValio" to mMRC.toString())
            shtrihData["mMRC"] = dataDescription
        }
        val dataDescription=mutableMapOf("decimalPointPosition" to "0",
            "mValio" to "1")
        shtrihData["Shdisassembled"] = dataDescription

        return shtrihData
    }
    // разобрать строку штрихкода
    private fun parseBarcodeString(tekShtrih:String,shtrihData:MutableMap<String,MutableMap<String,String>>,
                   codesGS1:MutableMap<String,MutableMap<String,String>>):MutableMap<String,MutableMap<String,String>>{

        val tekShtrihLength=tekShtrih.length

        var numChar=1
        var mMRC=0.0

        while (numChar <=tekShtrihLength) {

            var mGS1=strAverage(tekShtrih,numChar,2)
//            var mGS1Length=mGS1.length
            var strDecimalPointPosition=""
            var codeDescription=codesGS1.getOrElse(mGS1) {mutableMapOf()
            }
            if (codeDescription.isEmpty()){
                mGS1=strAverage(tekShtrih,numChar,3)
                codeDescription=codesGS1.getOrElse(mGS1) {mutableMapOf()
                }
            }
            if (codeDescription.isEmpty()){
                mGS1=strAverage(tekShtrih,numChar,4)
                codeDescription=codesGS1.getOrElse(mGS1) {mutableMapOf()
                }
            }
            if (codeDescription.isEmpty()){
                return shtrihData
            }

            numChar += mGS1.length

            if (codeDescription["trueDecimalPointPosition"] =="true"){
                strDecimalPointPosition=strAverage(mGS1,numChar,1)
                numChar += 1
            }


            var mValio=""
            val fixedLength=codeDescription["fixedLength"]?.toInt()!!
            if (fixedLength>0) {
                val fixedValueType=codeDescription["fixedValueType"]!!
                mValio=strAverage(tekShtrih,numChar,fixedLength)
                if (mValio.length!=fixedLength){
                    return shtrihData
                }
                if (fixedValueType=="N"){
                    if (!isNumericXXI(mValio)) {
                        return shtrihData
                    }
                }
                numChar += fixedLength
            }

            val variableLength=codeDescription["variableLength"]?.toInt()!!
            if (variableLength>0){
                val variableValueType=codeDescription["variableValueType"]!!
                val mValioVariable=strAverage(tekShtrih,numChar)
                if (mValioVariable.length>variableLength){
                    return shtrihData
                }
                if (variableValueType=="N"){
                    if (!isNumericXXI(mValioVariable)) {
                        return shtrihData
                    }
                }
                numChar += mValioVariable.length
                mValio += mValioVariable
            }

            var decimalPointPosition=0
            if (strDecimalPointPosition.isNotBlank()) {
                decimalPointPosition = strDecimalPointPosition.toInt()
                if (decimalPointPosition>0) {
                    for (i in 0..(decimalPointPosition-mValio.length)) {
                        mValio= "0$mValio"
                    }
                    mValio=strLeft(mValio,mValio.length-decimalPointPosition)+ "." +
                            strRight(mValio,decimalPointPosition)
                }
            }
            val dataDescription=mutableMapOf("decimalPointPosition" to decimalPointPosition.toString(),
                "mValio" to mValio)
            shtrihData[mGS1] = dataDescription

            if (mGS1=="8005") {
                mMRC=(strLeft(mValio,3)+"."+strRight(mValio,3)).toDouble()
            }
        }
        if (mMRC>0){
            val dataDescription=mutableMapOf("decimalPointPosition" to "0",
                "mValio" to mMRC.toString())
            shtrihData["mMRC"] = dataDescription
        }
        val dataDescription=mutableMapOf("decimalPointPosition" to "0",
            "mValio" to "1")
        shtrihData["Shdisassembled"] = dataDescription

        return shtrihData
    }
    private fun createResultStructure():MutableMap<String,MutableMap<String,String>>{
        val shtrihData= mutableMapOf<String,MutableMap<String,String>>()
        val dataDescription=mutableMapOf("decimalPointPosition" to "0","mValio" to "0")
        shtrihData["Shdisassembled"] = dataDescription
        return shtrihData
    }
    // СТО ЮНИСКАН 4-2013
    private fun listCodesGS1():MutableMap<String,MutableMap<String,String>>{
        var codesGS1= mutableMapOf<String,MutableMap<String,String>>()

        //    n - располож запятой с конца                                                          Длина	Тип (N-Чис,X-любой)
        //           Код         Имя                                Фикс            перем           фикс            перем           разделитель                 Описание
        //                                                                                                                          (знак символа F1 (FNC1))
        codesGS1=addCodeGS1(codesGS1, "00"  , "SSCC"                      , 18 , 0 ,"" ,"" ,false,"Серийный грузовой контейнерный код")
        codesGS1=addCodeGS1(codesGS1, "01"  , "GTIN"                      , 14 , 0 ,"" ,"" ,false,"Идентификационный номер единицы товара")
        codesGS1=addCodeGS1(codesGS1, "02"  , "CONTENT"                   , 14 , 0 ,"" ,"" ,false,"GTIN торговых единиц, содержащихся в грузе")
        codesGS1=addCodeGS1(codesGS1, "10"  , "BATCH_LOT"                 , 0  , 20,"" ,"X",true ,"Номер лота (партии, группы, пакета)")
        codesGS1=addCodeGS1(codesGS1, "11"  , "PROD_DATE"                 , 6  , 0 ,"" ,"" ,false,"Дата изготовления (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "12"  , "DUE_DATE"                  , 6  , 0 ,"" ,"" ,false,"Дата оплаты (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "13"  , "PACK_DATE"                 , 6  , 0 ,"" ,"" ,false,"Дата упаковки (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "15"  , "BEST_BEFORE"               , 6  , 0 ,"" ,"" ,false,"Дата реализации (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "16"  , "SELL_BY"                   , 6  , 0 ,"" ,"" ,false,"Продажа по дате (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "17"  , "EXPIRE"                    , 6  , 0 ,"" ,"" ,false,"Дата окончания срока годности (ГГММДД)")
        codesGS1=addCodeGS1(codesGS1, "20"  , "VARIANT"                   , 2  , 0 ,"" ,"" ,false,"Вариант продукции")
        codesGS1=addCodeGS1(codesGS1, "21"  , "SERIAL"                    , 0  , 20,"" ,"X",false,"Серийный номер")
        codesGS1=addCodeGS1(codesGS1, "22"  , "CPV"                       , 0  , 20,"" ,"X",true ,"Вспомогательные данные специальных фармацевтических продуктов")
        codesGS1=addCodeGS1(codesGS1, "240" , "ADDITIONAL_ID"             , 0  , 30,"" ,"X",true ,"Дополнительная идентификация продукта, присваиваемая производителем")
        codesGS1=addCodeGS1(codesGS1, "241" , "CUSTOMER_PART_NO"          , 0  , 30,"" ,"X",true ,"Номер товара присвоенный потребителем")
        codesGS1=addCodeGS1(codesGS1, "242" , "MTO_VARIANT"               , 0  , 6 ,"" ,"" ,true ,"Номер исполнения на заказ")
        codesGS1=addCodeGS1(codesGS1, "243" , "PCN"                       , 0  , 20,"" ,"X",true ,"Номер упаковочного компанента")
        codesGS1=addCodeGS1(codesGS1, "250" , "SECONDARY_SERIAL"          , 0  , 30,"" ,"X",true ,"Дополнительный серийный номер")
        codesGS1=addCodeGS1(codesGS1, "251" , "REF_TO_SOURCE"             , 0  , 30,"" ,"X",true ,"Ссылка на исходный продукт")
        codesGS1=addCodeGS1(codesGS1, "253" , "GDTI"                      , 13 , 17,"N","X",true ,"Глобальный идентиыикатор типа документа")
        codesGS1=addCodeGS1(codesGS1, "254" , "GLN_EXTENSION_COMPONENT"   , 0  , 20,"" ,"X",true ,"Добавочный компанент Глобальный номер места нахождения")
        codesGS1=addCodeGS1(codesGS1, "255" , "GСТ"                       , 13 , 12,"" ,"" ,true ,"Глобальный номер купона")
        codesGS1=addCodeGS1(codesGS1, "30"  , "VAR_COUNT"                 , 0  , 8 ,"" ,"" ,true ,"Число штучных предметов (предмет торговли переменной величины)")
        codesGS1=addCodeGS1(codesGS1, "310n", "NET_WEIGHT_kg"             , 6  , 0 ,"" ,"" ,false,"Вес НЕТТО Кг")
        codesGS1=addCodeGS1(codesGS1, "311n", "LENGTH_m"                  , 6  , 0 ,"" ,"" ,false,"Длина или 1 изм  метр")
        codesGS1=addCodeGS1(codesGS1, "312n", "WIDTH_m"                   , 6  , 0 ,"" ,"" ,false,"Ширина или 2 изм метр")
        codesGS1=addCodeGS1(codesGS1, "313n", "HEIGHT_m"                  , 6  , 0 ,"" ,"" ,false,"Высота или 3 изм метр")
        codesGS1=addCodeGS1(codesGS1, "314n", "AREA_m2"                   , 6  , 0 ,"" ,"" ,false,"Площадь кв.м")
        codesGS1=addCodeGS1(codesGS1, "315n", "NET_VOLUME_l"              , 6  , 0 ,"" ,"" ,false,"Объем НЕТТО Литр")
        codesGS1=addCodeGS1(codesGS1, "316n", "NET_VOLUME_m3"             , 6  , 0 ,"" ,"" ,false,"Объем НЕТОО куб.м")
        codesGS1=addCodeGS1(codesGS1, "320n", "NET_WEIGHT_lb"             , 6  , 0 ,"" ,"" ,false,"Вес НЕТТО фунты")
        codesGS1=addCodeGS1(codesGS1, "321n", "LENGTH_i"                  , 6  , 0 ,"" ,"" ,false,"Длина дюймы")
        codesGS1=addCodeGS1(codesGS1, "322n", "LENGTH_f"                  , 6  , 0 ,"" ,"" ,false,"Длина футы")
        codesGS1=addCodeGS1(codesGS1, "323n", "LENGTH_y"                  , 6  , 0 ,"" ,"" ,false,"Длина ярды")
        codesGS1=addCodeGS1(codesGS1, "324n", "WIDTH_i"                   , 6  , 0 ,"" ,"" ,false,"Ширина дюймы")
        codesGS1=addCodeGS1(codesGS1, "325n", "WIDTH_f"                   , 6  , 0 ,"" ,"" ,false,"Ширина футы")
        codesGS1=addCodeGS1(codesGS1, "326n", "WIDTH_y"                   , 6  , 0 ,"" ,"" ,false,"Ширина ярды")
        codesGS1=addCodeGS1(codesGS1, "327n", "HEIGHT_i"                  , 6  , 0 ,"" ,"" ,false,"Высота дюймы")
        codesGS1=addCodeGS1(codesGS1, "328n", "HEIGHT_f"                  , 6  , 0 ,"" ,"" ,false,"Высота футы")
        codesGS1=addCodeGS1(codesGS1, "329n", "HEIGHT_y"                  , 6  , 0 ,"" ,"" ,false,"Высота ярды")
        codesGS1=addCodeGS1(codesGS1, "330n", "GROSS_WEIGHT_kg"           , 6  , 0 ,"" ,"" ,false,"Логический вес кг")
        codesGS1=addCodeGS1(codesGS1, "331n", "LENGTH_m_log"              , 6  , 0 ,"" ,"" ,false,"Логический Длина или 1 изм  метр")
        codesGS1=addCodeGS1(codesGS1, "332n", "WIDTH_m_log"               , 6  , 0 ,"" ,"" ,false,"Логический Ширина или 2 изм метр")
        codesGS1=addCodeGS1(codesGS1, "333n", "HEIGHT_m_log"              , 6  , 0 ,"" ,"" ,false,"Логический Высота или 3 изм метр")
        codesGS1=addCodeGS1(codesGS1, "334n", "AREA_m2_log"               , 6  , 0 ,"" ,"" ,false,"Логический Площадь кв.м")
        codesGS1=addCodeGS1(codesGS1, "335n", "VOLUME_l_log"              , 6  , 0 ,"" ,"" ,false,"Логический Объем Литр")
        codesGS1=addCodeGS1(codesGS1, "336n", "VOLUME_m3_log"             , 6  , 0 ,"" ,"" ,false,"Логический Объем куб.м")
        codesGS1=addCodeGS1(codesGS1, "337n", "KG_PER_m2"                 , 6  , 0 ,"" ,"" ,false,"Кг на кв.метр")
        codesGS1=addCodeGS1(codesGS1, "340n", "GROSS_WEIGHT_lb"           , 6  , 0 ,"" ,"" ,false,"Логический вес фунты")
        codesGS1=addCodeGS1(codesGS1, "341n", "LENGTH_i_log"              , 6  , 0 ,"" ,"" ,false,"Логический длина дюймы")
        codesGS1=addCodeGS1(codesGS1, "342n", "LENGTH_f_log"              , 6  , 0 ,"" ,"" ,false,"Логический длина футы")
        codesGS1=addCodeGS1(codesGS1, "343n", "LENGTH_y_log"              , 6  , 0 ,"" ,"" ,false,"Логический длина ярды")
        codesGS1=addCodeGS1(codesGS1, "344n", "WIDTH_i_log"               , 6  , 0 ,"" ,"" ,false,"Логический Ширина дюймы")
        codesGS1=addCodeGS1(codesGS1, "345n", "WIDTH_f_log"               , 6  , 0 ,"" ,"" ,false,"Логический Ширина футы")
        codesGS1=addCodeGS1(codesGS1, "346n", "WIDTH_y_log"               , 6  , 0 ,"" ,"" ,false,"Логический Ширина ярды")
        codesGS1=addCodeGS1(codesGS1, "347n", "HEIGHT_i_log"              , 6  , 0 ,"" ,"" ,false,"Логический Высота дюймы")
        codesGS1=addCodeGS1(codesGS1, "348n", "HEIGHT_f_log"              , 6  , 0 ,"" ,"" ,false,"Логический Высота футы")
        codesGS1=addCodeGS1(codesGS1, "349n", "HEIGHT_y_log"              , 6  , 0 ,"" ,"" ,false,"Логический Высота ярды")
        codesGS1=addCodeGS1(codesGS1, "350n", "AREA_i2"                   , 6  , 0 ,"" ,"" ,false,"Площадь кв. дюймы")
        codesGS1=addCodeGS1(codesGS1, "351n", "AREA_f2"                   , 6  , 0 ,"" ,"" ,false,"Площадь кв. футы")
        codesGS1=addCodeGS1(codesGS1, "352n", "AREA_y2"                   , 6  , 0 ,"" ,"" ,false,"Площадь кв. ярды")
        codesGS1=addCodeGS1(codesGS1, "353n", "AREA_i2_log"               , 6  , 0 ,"" ,"" ,false,"Логический Площадь кв. дюймы")
        codesGS1=addCodeGS1(codesGS1, "354n", "AREA_f2_log"               , 6  , 0 ,"" ,"" ,false,"Логический Площадь кв. футы")
        codesGS1=addCodeGS1(codesGS1, "355n", "AREA_y2_log"               , 6  , 0 ,"" ,"" ,false,"Логический Площадь кв. ярды")
        codesGS1=addCodeGS1(codesGS1, "356n", "NET_WEIGHT_t"              , 6  , 0 ,"" ,"" ,false,"Вес нетто тройские унции")
        codesGS1=addCodeGS1(codesGS1, "357n", "NET_VOLUME_oz"             , 6  , 0 ,"" ,"" ,false,"Объем нетто унции")
        codesGS1=addCodeGS1(codesGS1, "360n", "NET_VOLUME_q"              , 6  , 0 ,"" ,"" ,false,"Объем нетто кварты")
        codesGS1=addCodeGS1(codesGS1, "361n", "NET_VOLUME_g"              , 6  , 0 ,"" ,"" ,false,"Объем нетто галоны США")
        codesGS1=addCodeGS1(codesGS1, "362n", "VOLUME_q"                  , 6  , 0 ,"" ,"" ,false,"Логический объем кварты")
        codesGS1=addCodeGS1(codesGS1, "363n", "VOLUME_g"                  , 6  , 0 ,"" ,"" ,false,"Логический объем галоны США")
        codesGS1=addCodeGS1(codesGS1, "364n", "VOLUME_i3"                 , 6  , 0 ,"" ,"" ,false,"Объем нетто куб. дюймы")
        codesGS1=addCodeGS1(codesGS1, "365n", "VOLUME_f3"                 , 6  , 0 ,"" ,"" ,false,"Объем нетто куб. футы")
        codesGS1=addCodeGS1(codesGS1, "366n", "VOLUME_y3"                 , 6  , 0 ,"" ,"" ,false,"Объем нетто куб. ярды")
        codesGS1=addCodeGS1(codesGS1, "367n", "VOLUME_i3_log"             , 6  , 0 ,"" ,"" ,false,"Логический Объем куб. дюймы")
        codesGS1=addCodeGS1(codesGS1, "368n", "VOLUME_f3_log"             , 6  , 0 ,"" ,"" ,false,"Логический Объем куб. футы")
        codesGS1=addCodeGS1(codesGS1, "369n", "VOLUME_y3_log"             , 6  , 0 ,"" ,"" ,false,"Логический Объем куб. ярды")
        codesGS1=addCodeGS1(codesGS1, "37"  , "COUNT"                     , 0  , 8 ,"" ,"" ,true ,"Количество торговых единиц в грузе")
        codesGS1=addCodeGS1(codesGS1, "390n", "AMOUNT"                    , 0  , 15,"" ,"" ,true ,"Сумма, подлежащая к оплате – местная валюта")
        codesGS1=addCodeGS1(codesGS1, "391n", "AMOUNT_ISO"                , 3  , 15,"" ,"" ,true ,"Сумма, подлежащая к оплате – с ISO кодом валюты")
        codesGS1=addCodeGS1(codesGS1, "392n", "PRICE"                     , 0  , 15,"" ,"" ,true ,"Сумма, подлежащая к оплате за товар переменного величины – местная валюта")
        codesGS1=addCodeGS1(codesGS1, "393n", "PRICE_ISO"                 , 3  , 15,"" ,"" ,true ,"Сумма, подлежащая к оплате за товар переменного величины – с ISO кодом валюты")
        codesGS1=addCodeGS1(codesGS1, "394n", "PRCNT_OFF"                 , 4  , 0 ,"" ,"" ,true ,"Процентная скидка купона")
        codesGS1=addCodeGS1(codesGS1, "400" , "ORDER_NUMBER"              , 0  , 30,"" ,"X",true ,"Номер заявки покупателя на покупку")
        codesGS1=addCodeGS1(codesGS1, "401" , "GINC"                      , 0  , 30,"" ,"X",true ,"Глобальный идентификационный номер партии груза")
        codesGS1=addCodeGS1(codesGS1, "402" , "GSIN"                      , 17 , 0 ,"" ,"" ,true ,"Глобальный идентификационный номер отправки груза")
        codesGS1=addCodeGS1(codesGS1, "403" , "ROUTE"                     , 0  , 30,"" ,"X",true ,"Код маршрута")
        codesGS1=addCodeGS1(codesGS1, "410" , "SHIP_TO_LOC"               , 13 , 0 ,"" ,"" ,false,"Доставить-вручить EAN/UCC Глобальный адресный номер")
        codesGS1=addCodeGS1(codesGS1, "411" , "BILL_TO"                   , 13 , 0 ,"" ,"" ,false,"Счет-фактура EAN/UCC Глобальный адресный номер")
        codesGS1=addCodeGS1(codesGS1, "412" , "PURCHASE_FROM"             , 13 , 0 ,"" ,"" ,false,"Закуплено у EAN/UCC Глобальный адресный номер")
        codesGS1=addCodeGS1(codesGS1, "413" , "SHIP_FOR_LOC"              , 13 , 0 ,"" ,"" ,false,"Груз Для - Поставка Для - Переслать EAN/UCC Глобальный адресный номер")
        codesGS1=addCodeGS1(codesGS1, "414" , "LOC_No"                    , 13 , 0 ,"" ,"" ,false,"Идентификация места размещения, EAN/UCC Глобальный адресный номер")
        codesGS1=addCodeGS1(codesGS1, "415" , "PAY_TO"                    , 13 , 0 ,"" ,"" ,false,"EAN/UCC Глобальный адресный номер стороны, выставившей счет")
        codesGS1=addCodeGS1(codesGS1, "416" , "PROD_SERV_LOC"             , 13 , 0 ,"" ,"" ,false,"GLN места производства или обслуживания")
        codesGS1=addCodeGS1(codesGS1, "420" , "SHIP_TO_POST"              , 0  , 20,"" ,"X",true ,"Доставить-вручить Почтовый код в пределах одной страны")
        codesGS1=addCodeGS1(codesGS1, "421" , "SHIP_TO_POST_ISO"          , 3  , 9 ,"" ,"X",true ,"Доставить-вручить Почтовый код с 3-хзначным кодом страны по ISO")
        codesGS1=addCodeGS1(codesGS1, "422" , "ORIGIN"                    , 3  , 0 ,"" ,"" ,true ,"Страна происхождения торговой единицы")
        codesGS1=addCodeGS1(codesGS1, "423" , "CONTRY_INITIAL_PROCESS"    , 3  , 12,"" ,"" ,true ,"Страна первоначальной обработки")
        codesGS1=addCodeGS1(codesGS1, "424" , "CONTRY_PROCESS"            , 3  , 0 ,"" ,"" ,true ,"Страна обработки")
        codesGS1=addCodeGS1(codesGS1, "425" , "CONTRY_DISASSEMBLY"        , 3  , 12,"" ,"" ,true ,"Страна демонтажа")
        codesGS1=addCodeGS1(codesGS1, "426" , "CONTRY_FULL_PROCESS"       , 3  , 0 ,"" ,"" ,true ,"Страна окончательной обработки")
        codesGS1=addCodeGS1(codesGS1, "427" , "ORIGIN_SUBDIVISION"        , 0  , 3 ,"" ,"X",true ,"Код названия по ISO единицы административно-хозяйственного деления")
        codesGS1=addCodeGS1(codesGS1, "7001", "NSN"                       , 13 , 0 ,"" ,"" ,true ,"Номенклатурный номер (NATO)")
        codesGS1=addCodeGS1(codesGS1, "7002", "MEAT_CUT"                  , 0  , 30,"" ,"X",true ,"UN/ECE Туши и классификация разделки ЕЭК ООН")
        codesGS1=addCodeGS1(codesGS1, "7003", "EXPIRY_TIME"               , 10 , 0 ,"" ,"" ,true ,"Дата и время окончания срока годности")
        codesGS1=addCodeGS1(codesGS1, "7004", "ACTIVE_POTENCY"            , 0  , 4 ,"" ,"" ,true ,"Фактическая эффективность")
        codesGS1=addCodeGS1(codesGS1, "7005", "CATCH_AREA"                , 0  , 12,"" ,"X",true ,"Зона улова")
        codesGS1=addCodeGS1(codesGS1, "7006", "FIRST_FREEZE_DATE"         , 6  , 0 ,"" ,"" ,true ,"Первая дата замораживания")
        codesGS1=addCodeGS1(codesGS1, "7007", "HARVEST_DATE"              , 6  , 6 ,"" ,"" ,true ,"Дата сбора урожая")
        codesGS1=addCodeGS1(codesGS1, "7008", "AQUATIC_SPECIES"           , 0  , 3 ,"" ,"X",true ,"Виды для рыбохозяйственных целей")
        codesGS1=addCodeGS1(codesGS1, "7009", "FISHING_GEAR_TYPE"         , 0  , 10,"" ,"X",true ,"Тип орудия лова")
        codesGS1=addCodeGS1(codesGS1, "7010", "PROD_METHOD"               , 0  , 2 ,"" ,"X",true ,"Метод производства")
        codesGS1=addCodeGS1(codesGS1, "7020", "REFURB_LOT"                , 0  , 20,"" ,"X",true ,"Идентификатор партии ремонта")
        codesGS1=addCodeGS1(codesGS1, "7021", "FUNC_STAT"                 , 0  , 20,"" ,"X",true ,"Функциональное состояние")
        codesGS1=addCodeGS1(codesGS1, "7022", "REV_STAT"                  , 0  , 20,"" ,"X",true ,"Статус ревизии")
        codesGS1=addCodeGS1(codesGS1, "7023", "GIAI_ASSEMBLY"             , 0  , 30,"" ,"X",true ,"Глобальный индивидуальный идентификатор активов сборки")
        codesGS1=addCodeGS1(codesGS1, "703s", "PROCESSOR_s"               , 3  , 27,"N","X",true ,"Номер процессора с кодом страны ISO")
        codesGS1=addCodeGS1(codesGS1, "710" , "NHRN_PZN"                  , 0  , 20,"" ,"X",true ,"Национальный номер возмещения расходов на здравоохранение-Германия")
        codesGS1=addCodeGS1(codesGS1, "711" , "NHRN_CIP"                  , 0  , 20,"" ,"X",true ,"Национальный номер возмещения расходов на здравоохранение-Франция")
        codesGS1=addCodeGS1(codesGS1, "712" , "NHRN_CN"                   , 0  , 20,"" ,"X",true ,"Национальный номер возмещения расходов на здравоохранение-Испания")
        codesGS1=addCodeGS1(codesGS1, "713" , "NHRN_DRN"                  , 0  , 20,"" ,"X",true ,"Национальный номер возмещения расходов на здравоохранение-Бразилия")
        codesGS1=addCodeGS1(codesGS1, "714" , "NHRN_AIM"                  , 0  , 20,"" ,"X",true ,"Национальный номер возмещения расходов на здравоохранение-Португалия")
        codesGS1=addCodeGS1(codesGS1, "8001", "DIMENSIONS"                , 14 , 0 ,"" ,"" ,true ,"Рулонные товары–Толщина, длина, диаметр, направление обмотки, сращивание")
        codesGS1=addCodeGS1(codesGS1, "8002", "CMT_No"                    , 0  , 20,"" ,"X",true ,"Электронный серийный идентификацион- ный номер для мобильного телефона")
        codesGS1=addCodeGS1(codesGS1, "8003", "GRAI"                      , 14 , 16,"N","X",true ,"Глобальный номер оборотной тары")
        codesGS1=addCodeGS1(codesGS1, "8004", "GIAI"                      , 0  , 30,"" ,"X",true ,"Глобальный номер индивидуального имущества")
        codesGS1=addCodeGS1(codesGS1, "8005", "PRICE_PER_UNIT"            , 6  , 0 ,"" ,"" ,true ,"Цена единицы измерения товара")
        codesGS1=addCodeGS1(codesGS1, "8006", "ITIP_or_GCTIN"             , 18 , 0 ,"" ,"" ,true ,"Идентификация компонент торговой единицы")
        codesGS1=addCodeGS1(codesGS1, "8007", "IBAN"                      , 0  , 34,"" ,"X",true ,"Международный номер банковского счета")
        codesGS1=addCodeGS1(codesGS1, "8008", "PROD_TIME"                 , 8  , 4 ,"" ,"" ,true ,"Дата и время производства")
        codesGS1=addCodeGS1(codesGS1, "8010", "CPID"                      , 0  , 30,"" ,"X",true ,"Идентификатор компонента/детали")
        codesGS1=addCodeGS1(codesGS1, "8011", "CPID_SERIAL"               , 0  , 12,"" ,"" ,true ,"Серийный номер идентификатора компонента/детали")
        codesGS1=addCodeGS1(codesGS1, "8012", "VERSION"                   , 0  , 20,"" ,"X",true ,"Версия программного обеспечения")
        codesGS1=addCodeGS1(codesGS1, "8013", "GMN"                       , 0  , 20,"" ,"X",true ,"Глобальный номер модели")
        codesGS1=addCodeGS1(codesGS1, "8017", "GSRN_PROVIDER"             , 18 , 0 ,"" ,"" ,true ,"Глобальный номер для услуг с поставщиком")
        codesGS1=addCodeGS1(codesGS1, "8018", "GSRN_RECIPIENT"            , 18 , 0 ,"" ,"" ,true ,"Глобальный номер для услуг с получателем")
        codesGS1=addCodeGS1(codesGS1, "8019", "SRIN"                      , 0  , 10,"" ,"" ,true ,"Номер экземпляра отношения службы")
        codesGS1=addCodeGS1(codesGS1, "8020", "REF_No"                    , 0  , 25,"" ,"X",true ,"Ссылочный номер платежного требования")
        codesGS1=addCodeGS1(codesGS1, "8110", "COUPON_CODE_ID"            , 0  , 70,"" ,"X",true ,"Идентификация кода купона для США")
        codesGS1=addCodeGS1(codesGS1, "8111", "POINTS"                    , 4  , 0 ,"" ,"" ,true ,"Баллы лояльности купона")
        codesGS1=addCodeGS1(codesGS1, "8112", "PAPPERLESS_COUPON_CODE_ID" , 0  , 70,"" ,"X",true ,"Безбужная идентификация кода купона для США")
        codesGS1=addCodeGS1(codesGS1, "8200", "PRODUCT_URL"               , 0  , 70,"" ,"X",true ,"URL расширенной упаковки")
        codesGS1=addCodeGS1(codesGS1, "90"  , "INTERNAL"                  , 0  , 30,"" ,"X",true ,"Информация по согласованию между торговыми партнерами")
        codesGS1=addCodeGS1(codesGS1, "91"  , "INTERNAL1"                 , 0  , 90,"" ,"X",true ,"GS1: Ключ проверки")
        codesGS1=addCodeGS1(codesGS1, "92"  , "INTERNAL2"                 , 0  , 90,"" ,"X",true ,"GS1: Код проверки")
        codesGS1=addCodeGS1(codesGS1, "93"  , "INTERNAL3"                 , 0  , 90,"" ,"X",true ,"Сигареты: Код проверки")
        codesGS1=addCodeGS1(codesGS1, "94"  , "INTERNAL4"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")
        codesGS1=addCodeGS1(codesGS1, "95"  , "INTERNAL5"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")
        codesGS1=addCodeGS1(codesGS1, "96"  , "INTERNAL6"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")
        codesGS1=addCodeGS1(codesGS1, "97"  , "INTERNAL7"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")
        codesGS1=addCodeGS1(codesGS1, "98"  , "INTERNAL8"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")
        codesGS1=addCodeGS1(codesGS1, "99"  , "INTERNAL9"                 , 0  , 90,"" ,"X",true ,"Внутренняя информация компании")

        return codesGS1
    }
    // Добавить Код GS1
    private fun addCodeGS1(codesGS1:MutableMap<String,MutableMap<String,String>>,
                           cod:String,
                           name:String,
                           fixedLength:Int,
                           variableLength:Int,
                           fixedValueType:String,
                           variableValueType:String,
                           hasSeparator:Boolean,
                           description:String
                           ):MutableMap<String,MutableMap<String,String>>{

        val lastCharCode=strRight(cod,1)
        if (!lastCharCode.isDigitsOnly()) {
            val codeWithoutLastChar=strLeft(cod,cod.length-1)
            if (lastCharCode=="n") {
                val codeDescription = codeDescription(
                    codeWithoutLastChar,
                    name, fixedLength, variableLength, fixedValueType, variableValueType,
                    hasSeparator, description
                )
                codeDescription["trueDecimalPointPosition"] = true.toString()
                codesGS1[codeWithoutLastChar] = codeDescription
            }else{
                for (i in 1..9) {
                    val newCode=codeWithoutLastChar+i
                    codesGS1[newCode] = codeDescription(newCode,name,fixedLength,variableLength,
                        fixedValueType,variableValueType,hasSeparator,description)
                }
            }
        }else{
            codesGS1[cod] = codeDescription(cod,name,fixedLength,variableLength,
                fixedValueType,variableValueType,hasSeparator,description)
        }

        return codesGS1
    }
    // Добавить описание кода GS1
    private fun codeDescription(cod:String,
                                name:String,
                                fixedLength:Int,
                                variableLength:Int,
                                fixedValueType:String,
                                variableValueType:String,
                                hasSeparator:Boolean,
                                description:String):MutableMap<String,String>{

        val codeDescription= mutableMapOf<String,String>()
        codeDescription["cod"] = cod
        codeDescription["name"] = name
        codeDescription["fixedLength"] = fixedLength.toString()
        if (fixedLength>0) {
//            if (fixedValueType==""){
//                codeDescription["fixedValueType"] = "N"
//            }else{
                codeDescription["fixedValueType"] = fixedValueType
//            }
        }
        codeDescription["variableLength"] = variableLength.toString()
        if (variableLength>0) {
//            if (variableValueType==""){
//                codeDescription["variableValueType"] = "N"
//            }else{
                codeDescription["variableValueType"] = variableValueType
//            }
        }
        if (variableLength>0){
            codeDescription["hasSeparator"] = "true"
        }else{
            codeDescription["hasSeparator"] = hasSeparator.toString()
        }
        codeDescription["variableLength"] = variableLength.toString()
        codeDescription["trueDecimalPointPosition"] = "false"
        codeDescription["description"] = description

        return codeDescription
    }
    //Проверяем на ЕАН и акцизные марки
    private fun checkEAN(mShtrih:String):Boolean{
        var result=false
        val lengthSh=mShtrih.length
        if (lengthSh==8){
            val isNumeric=isNumericXXI(mShtrih)
            if (isNumeric){
                var ratio=3
                var summa=0.0
                val contrShtrih=mShtrih.dropLast(1)
                val lastChar=mShtrih.last()
                for (ellement in contrShtrih){
                    summa += ratio * ellement.digitToInt()
                    ratio=4-ratio
                }
                summa=(10-summa%10)%10
                if (lastChar.digitToInt()==summa.toInt()){
                    result = true
                }
            }
        }else if ((lengthSh==13)||(lengthSh==14)){
            val isNumeric=isNumericXXI(mShtrih)
            if (isNumeric){
                var ratio=1
                var summa=0.0
                val contrShtrih=mShtrih.dropLast(1)
                val lastChar=mShtrih.last()
                for (ellement in contrShtrih){
                    summa += ratio * ellement.digitToInt()
                    ratio=4-ratio
                }
                summa=(10-summa%10)%10
                if (lastChar.digitToInt()==summa.toInt()){
                    result = true
                }
            }
        }else if ((lengthSh==13)&&(lengthSh==68)){//Старая акцизная марка алкоголя
            result = true
        }else if ((lengthSh==13)&&(lengthSh==150)){//Старая акцизная марка алкоголя
            result = true
        }
        return result
    }
    // Вычисляет МРЦ из кода маркировки табачной пачки.
    //
    // Параметры:
    // 	КодМаркировки - Строка - Код маркировки табачной пачки.
    // Возвращаемое значение:
    // 	Неопределено, Число - МРЦ.
    private fun markMRC(kodMark:String,nMark:Int):Double{
        var strMRC=""
        var mMRC=0.0
        if (nMark==29){
           strMRC= strAverage(kodMark,22,4)
        }else if (nMark==31){
            strMRC= strAverage(kodMark,26,4)
        }

        val alfabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"%&'*+-./_,:;=<>?"

        var index = 1
        while (index <= 4) {
            val nChar= strAverage(strMRC, index, 1)
            val idxChar = alfabet.indexOf(nChar)
            val bbb=80.0
            mMRC += bbb.pow(4 - index) * idxChar
            index ++
       }

        // Если цена <= 5000 и нет копеек, то высокая вероятность, что это реальное МРЦ.
        if ((mMRC <= 500000)&&(mMRC%100 == 0.0)) {
            mMRC /= 100
        }
        return mMRC
    }

    // Проверяем на цифры
    private fun isNumericXXI(toCheck: String): Boolean {
        return toCheck.all { char -> char.isDigit() }
    }

    // ЛЕВО слева n символов
    private fun strLeft(str: String, nChar: Int): String {
        var n = str.length - nChar
        if (n < 0) {
            n = 0
        }
        return str.dropLast(n)
    }

    // ПРАВО справа n символов
    private fun strRight(str: String, nChar: Int): String {
        var n = str.length - nChar
        if (n < 0) {
            n = 0
        }
        return str.drop(n)
    }
    // СРЕД с символа beginChar nChar символов
    private fun strAverage(str:String,beginChar:Int,qChar:Int=0):String{
        var subString=str.substring(beginChar-1)
        if (qChar!=0){
            subString = strLeft(subString, qChar)
        }
        return subString
    }
    // СтрНайти(<Строка>, <ПодстрокаПоиска>, <НаправлениеПоиска>, <НачальнаяПозиция>, <НомерВхождения>)
    private fun strFind(str:String,subStr:String,searchBeginning:Boolean=true,
                        startingPosition:Int=1,occurrenceNumber:Int=1):Int{
        var result=0
        var tekNumOccur=0
        if (searchBeginning){
            for(i in startingPosition..str.length){
                if (strAverage(str,i,subStr.length)==subStr){
                    result =i
                    tekNumOccur += 1
                    if (tekNumOccur==occurrenceNumber) {
                        break
                    }
                }
            }
        }else{
            for(i in str.length..startingPosition){
                if (strAverage(str,i,subStr.length)==subStr){
                    result =i
                    tekNumOccur += 1
                    if (tekNumOccur==occurrenceNumber) {
                        break
                    }
                }
            }
        }
        return result
    }
}
