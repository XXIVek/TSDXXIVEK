package com.xxivek.tsdxxivek.dataDB

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xxivek.tsdxxivek.*
import com.xxivek.tsdxxivek.dataXML.BuilderXML
import com.xxivek.tsdxxivek.dataXML.XMLDOMParser
import kotlinx.coroutines.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList

/**
 * View Model to keep a reference to the Inventory repository and an up-to-date list of all items.
 *
 */
class InventoryViewModel(private val itemDao: ItemDao) : ViewModel() {


    /**
     * Inserts the new Item into database.
     */
    fun addNewItem(itemSh:String, itemShTip:String, itemName: String, itemPrice: String,
                   quantityInStock: String, itemCount: String) {
        val newItem = getNewItemEntry(itemSh, itemShTip, itemName, itemPrice, quantityInStock, itemCount)
        insertItem(newItem)
    }


    fun deleteItem(itemSh:String, itemShTip:String, itemName: String, itemPrice: String,
                   quantityInStock: String, itemCount: String) {
        val newItem = getNewItemEntry(itemSh, itemShTip, itemName, itemPrice, quantityInStock,itemCount)
        viewModelScope.launch {
            itemDao.delete(newItem)
        }
    }

    /**
     * Launching a new coroutine to insert an item in a non-blocking way
     */
    private fun insertItem(item: Item) {
        viewModelScope.launch {
            itemDao.insert(item)
        }
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    fun isEntryValid(itemSh: String, itemShTip: String, itemName: String): Boolean {
        if (itemSh.isBlank() || itemShTip.isBlank() ||  itemName.isBlank()) {
            return false
        }
        return true
    }

    /**
     * Returns an instance of the [Item] entity class with the item info entered by the user.
     * This will be used to add a new entry to the Inventory database.
     */
    private fun getNewItemEntry(itemSh:String, itemShTip:String, itemName:String,
                                itemPrice:String, quantityInStock:String, itemCount:String): Item {
        return Item(
            itemSh=itemSh,
            itemShTip=itemShTip.toInt(),
            itemName = itemName,
            itemPrice = itemPrice.toDouble(),
            itemQuantityInStock = quantityInStock.toInt(),
            itemQuantity = itemCount.toInt()
        )
    }
}


/**
 * Factory class to instantiate the [ViewModel] instance.
 */
class InventoryViewModelFactory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

