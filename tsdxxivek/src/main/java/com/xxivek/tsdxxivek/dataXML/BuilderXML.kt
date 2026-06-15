package com.xxivek.tsdxxivek.dataXML

import com.xxivek.tsdxxivek.dataDB.Item
import java.time.temporal.ValueRange

class BuilderXML {
    //Добавьте \t для вкладки и \n для новой строки.
    //val pad = name.padStart(10, '#')
    //println(pad) // ####Barsik
    //
    //val name = "Barsik"
    //val pad = name.padEnd(10, '*')
    //println(pad) // Barsik****

    var textXML="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    var level=0
    fun beginningElementXXI(tElement:String){
        val newStr=""
        newStr.padStart(level, '\t')
        textXML=textXML+"\n<"+tElement+">"
        level++
    }
    fun endElementXXI(tElement:String){
        var newStr=""
        newStr.padStart(level, '\t')
        newStr=newStr+"</"+tElement+">"
        textXML += newStr
        level -= 1
    }
    fun writeTextXXI(text:String){
//        Character  Predeclared Entity
//   1     &          &amp;
//   2     <          &lt;
//   3     >          &gt;
//   4     "          &quot;
//   5     '          &apos;
        var newText=""
        newText=text.replace("&","&amp;")
        newText=newText.replace("<","&lt;")
        newText=newText.replace(">","&gt;")
        newText=newText.replace("\"","&quot;")
        newText=newText.replace("'","&apos;")
        textXML += newText
    }
    fun getTextXMLXXI():String{
        return textXML
    }
    fun createOutXMLXXI(listItem:List<Item>):String{
        beginningElementXXI("Documents")
            beginningElementXXI("Answers")
                for (item in listItem) {
                    beginningElementXXI("Shtrihs")
                        beginningElementXXI("Shtrih")
                            writeTextXXI(item.itemSh)
                        endElementXXI("Shtrih")
                        beginningElementXXI("ShtrihTip")
                           writeTextXXI(item.itemShTip.toString())
                        endElementXXI("ShtrihTip")
                        beginningElementXXI("TovNaim")
                           writeTextXXI(item.itemName)
                        endElementXXI("TovNaim")
                        beginningElementXXI("TovCena")
                           writeTextXXI(item.itemPrice.toString())
                        endElementXXI("TovCena")
                        beginningElementXXI("TovKol")
                            writeTextXXI(item.itemQuantity.toString())
                        endElementXXI("TovKol")
                    endElementXXI("Shtrihs")
                }
            endElementXXI("Answers")
        endElementXXI("Documents")
        return  textXML
    }
}