package com.xxivek.tsdxxivek.dataDB

import com.xxivek.tsdxxivek.TSDXXIVekApplication
import com.xxivek.tsdxxivek.appLic
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class UtilDB {

    private fun getDao(): ItemDao? {
        val app = TSDXXIVekApplication.instance
        return app?.database?.itemDao()
    }

    suspend fun onDelAllTables() {
        getDao()?.deleteAll()
        appLic.appInfoBD.postValue(0)
        appLic.appInfoINPUT.postValue(0)
    }

    fun clearQuantity() {
        kotlinx.coroutines.GlobalScope.launch(IO) {
            val mListItem = getDao()?.getItemNotEmpty2() ?: emptyList()
            if (mListItem.isNotEmpty()) {
                val mArrayItem = mListItem.map { elem ->
                    Item(
                        itemSh = elem.itemSh,
                        itemShTip = elem.itemShTip,
                        itemName = elem.itemName,
                        itemPrice = elem.itemPrice,
                        itemQuantityInStock = elem.itemQuantityInStock,
                        itemQuantity = 0
                    )
                }
                insertItemList(mArrayItem)
                appLic.conditionInfo()
            }
        }
    }

    /**
     * Launching a new coroutine to insert an item in a non-blocking way
     */
    suspend fun insertItemList(mItemList: List<Item>) = coroutineScope {
        val job = async {
            getDao()?.insertList(mItemList)
        }
        job.await()
    }

    suspend fun CountInfo() = coroutineScope {
        launch {
            val mCount = getDao()?.getCount()
            appLic.appInfoCountBD.postValue(mCount)
        }
    }

    suspend fun CountNotEmptyInfo() = coroutineScope {
        launch {
            val mCountNotEmpty = getDao()?.getCountNotEmpty()
            appLic.appInfoCountNotEmptyBD.postValue(mCountNotEmpty)
        }
    }
}
