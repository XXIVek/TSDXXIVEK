package com.xxivek.tsdxxivek.dataDB

import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.dataXML.BuilderXML
import com.xxivek.tsdxxivek.dataXML.XMLDOMParser
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

class UtilDB {
    suspend fun onDelAllTables() = coroutineScope{
        val job =  launch {
            TSDXXIVekApplication().database.clearAllTables()
        }
        job.join() // ждем завершения вложенной сопрограммы
        appLic.appInfoBD.postValue(0)
        appLic.appInfoINPUT.postValue(0)
    }

    fun clearQuantity(){
       observeItemNotempty(1)
    }


    suspend fun readInputXML(){
        val parser = XMLDOMParser()
        //       val manager: AssetManager = getAssets()
        val stream: InputStream?
        val mArrayItem= ArrayList<Item>()
        if (msg_server.isNotBlank()) {
            appLic.appInfoBD.postValue(-1)
            try {
//            stream= binding.root.context.getContentResolver().openInputStream(m_filePath)
                stream = msg_server.byteInputStream()
                val doc: Document? = parser.getDocument(stream)
                val nodeListDoc: NodeList? = doc?.getElementsByTagName("Documents")
                for (i in 0 until nodeListDoc!!.getLength()){
                    val e: Element = nodeListDoc.item(0) as Element
                    val appOper = e.getAttribute("Oper")
                    val appClient = e.getAttribute("Client")
                    if (appOper.isNotBlank()){
                        appLic.appOper=appOper
                    }
                    if (appClient.isNotBlank()){
                        appLic.appClient=appClient
                    }
                }

                // Get elements by name employee
                val nodeList: NodeList? = doc?.getElementsByTagName("Shtrihs")

                /*
             * for each <employee> element get text of name, salary and
             * designation
             */
                // Here, we have only one <employee> element
                for (i in 0 until nodeList!!.getLength()) {
                    val e: Element = nodeList.item(i) as Element
                    val itemShtrih = parser.getValue(e, "Shtrih")
                    val itemShtrihTip = parser.getValue(e, "ShtrihTip")
                    val itemTovName = parser.getValue(e, "TovNaim")
                    var itemTovCena = parser.getValue(e, "TovCena")
                    itemTovCena = itemTovCena.replace(",", ".")
                    var itemTovKol = parser.getValue(e, "TovKol")
                    itemTovKol = itemTovKol.replace(",", ".")
                    val mItem = Item(
                        itemSh=itemShtrih,
                        itemShTip=itemShtrihTip.toInt(),
                        itemName = itemTovName,
                        itemPrice = itemTovCena.toDouble(),
                        itemQuantityInStock = itemTovKol.toInt(),
                        itemQuantity = 0)
                    mArrayItem.add(mItem)
                }
                msg_server =""
                val mArrayItemList=mArrayItem.toList()
                insertItemList(mArrayItemList)
                appLic.conditionInfo()
                appLic.appInfoINPUT.postValue(0)
            } catch (e1: IOException) {
                appLic.appInfoINPUT.postValue(1)
                e1.printStackTrace()
            }
        }
    }
    fun writeXML(){
       observeItemNotempty(0)
    }

  private fun observeItemNotempty(variant:Int){
      CoroutineScope(IO).launch {
          launch {
              val mListItem = itemDatabase?.getItemNotEmpty2()!!
              //.collect { listItem ->
//                val mListItem = listItem.toList()
              if (mListItem.isNotEmpty()) {
                  if (variant == 0) {
//                            uploadlistItem(listItem)
                      msg_client = BuilderXML().createOutXMLXXI(mListItem)
                      if (msg_client.isBlank()) {
                          appLic.appInfoOUT.postValue(0)
                      } else {
                          appLic.appInfoOUT.postValue(3)
                      }
                  } else if (variant == 1) {
                      if (mListItem.isNotEmpty()) {
                          val mArrayItem = ArrayList<Item>()
                          for (elem in mListItem) {
                              val mItem = Item(
                                  itemSh = elem.itemSh,
                                  itemShTip = elem.itemShTip,
                                  itemName = elem.itemName,
                                  itemPrice = elem.itemPrice,
                                  itemQuantityInStock = elem.itemQuantityInStock,
                                  itemQuantity = 0
                              )
                              mArrayItem.add(mItem)
                          }
                          val mArrayItemList = mArrayItem.toList()
                          insertItemList(mArrayItemList)
                          appLic.conditionInfo()
                      }
                  }
              }
          }
      }
    }

//    private fun observeItemNotempty(variant:Int){
//        CoroutineScope(IO).launch {
//            launch {
//                itemDatabase?.getItemNotEmpty()?.collect { listItem ->
//                    val mListItem = listItem.toList()
//                    if (mListItem.isNotEmpty()) {
//                        if (variant == 0) {
////                            uploadlistItem(listItem)
//                            msg_client = BuilderXML().createOutXMLXXI(listItem)
//                            if (msg_client.isBlank()) {
//                                appLic.appInfoOUT.postValue(0)
//                            } else {
//                                appLic.appInfoOUT.postValue(3)
//                            }
//                        } else if (variant == 1) {
//                            if (listItem.isNotEmpty()) {
//                                val mArrayItem = ArrayList<Item>()
//                                for (elem in listItem) {
//                                    val mItem = Item(
//                                        itemSh = elem.itemSh,
//                                        itemShTip = elem.itemShTip,
//                                        itemName = elem.itemName,
//                                        itemPrice = elem.itemPrice,
//                                        quantityInStock = 0
//                                    )
//                                    mArrayItem.add(mItem)
//                                }
//                                val mArrayItemList = mArrayItem.toList()
//                                insertItemList(mArrayItemList)
//                                appLic.conditionInfo()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }

    /**
     * Launching a new coroutine to insert an item in a non-blocking way
     */
    suspend fun insertItemList(mItemList: List<Item>) = coroutineScope{
        val job = async {
            itemDatabase?.insertList(mItemList)
        }
        job.await()
    }
    suspend fun CountInfo() = coroutineScope{
        launch {
            val mCount=itemDatabase?.getCount()
            appLic.appInfoCountBD.postValue(mCount)
        }
    }
    suspend fun CountNotEmptyInfo() = coroutineScope{
        launch {
            val mCountNotEmpty=itemDatabase?.getCountNotEmpty()
            appLic.appInfoCountNotEmptyBD.postValue(mCountNotEmpty)
        }
    }
}