package com.xxivek.tsdxxivek.dataDB

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xxivek.tsdxxivek.appLic
import com.xxivek.tsdxxivek.databinding.ItemlineBinding

class ItemsRVAdapter : ListAdapter<Item, ItemsRVAdapter.ItemHolder>(DiffCallback()) {

    class ItemHolder(var viewBinding: ItemlineBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    private lateinit var listener: RecyclerClickListener
    fun setItemListener(listener: RecyclerClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val binding =
            ItemlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val itemline = ItemHolder(binding)

//        itemline.viewBinding.itemDelete.setOnClickListener {
//            listener.onItemRemoveClick(itemline.adapterPosition)
//        }

        itemline.viewBinding.itemline.setOnClickListener {
            listener.onItemClick(itemline.adapterPosition)
        }

        return itemline
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val currentItem = getItem(position)
        holder.viewBinding.shitem.text = currentItem.itemSh
        holder.viewBinding.shtipitem.text = currentItem.itemShTip.toString()
        holder.viewBinding.nameitem.text = currentItem.itemName
        if (appLic.appOper=="1"){
            holder.viewBinding.quantityIitem.text = currentItem.itemQuantity.toString()
            holder.viewBinding.priceitem.visibility = View.GONE
            holder.viewBinding.summaitem.visibility = View.GONE
        }else if (appLic.appOper=="2"){
            holder.viewBinding.priceitem.text = currentItem.itemPrice.toString()
            holder.viewBinding.summaitem.text = (currentItem.itemPrice*currentItem.itemQuantity).toString()
            holder.viewBinding.quantityIitem.text = ""+currentItem.itemQuantity.toString() +"/"+currentItem.itemQuantityInStock.toString()
            holder.viewBinding.priceitem.visibility = View.VISIBLE
            holder.viewBinding.summaitem.visibility = View.VISIBLE
        }else if (appLic.appOper=="3"){
            holder.viewBinding.priceitem.text = currentItem.itemPrice.toString()
            holder.viewBinding.summaitem.text = (currentItem.itemPrice*currentItem.itemQuantity).toString()
            holder.viewBinding.quantityIitem.text =""+currentItem.itemQuantity.toString() +"/"+currentItem.itemQuantityInStock.toString()
            holder.viewBinding.priceitem.visibility = View.VISIBLE
            holder.viewBinding.summaitem.visibility = View.VISIBLE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item)=
            oldItem == newItem

        override fun areContentsTheSame(oldItem: Item, newItem: Item) =
            oldItem == newItem
    }
}