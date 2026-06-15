package com.xxivek.tsdxxivek.dataDB

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.xxivek.tsdxxivek.R
import com.xxivek.tsdxxivek.databinding.FragmentItemList2Binding
import com.xxivek.tsdxxivek.databinding.FragmentItemListBinding
import com.xxivek.tsdxxivek.itemDatabase
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [ItemListFragment2.newInstance] factory method to
 * create an instance of this fragment.
 */
class ItemListFragment2 : Fragment() {
    // Интерфейс доступа к базе данных
    //private var itemDatabase: ItemDao?=null

    // binding FragmentItemListBinding
    private var _binding: FragmentItemList2Binding?=null
    val binding get() = _binding!!

    // Адаптер для recyclerView
    private lateinit var adapter: ItemsRVAdapter

    private lateinit var listItem1:List<Item>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentItemList2Binding.inflate(inflater, container, false)

        listItem1= emptyList()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Подключаем базу данных с интерфейсом обработки данных
        //itemDatabase=(activity?.application as TSDXXIVekApplication).database.itemDao()

        // При нажатии на кнопку "+" переходим на форму ввода данных
        binding.floatingActionButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_itemListFragment2_to_addItemFragment)
        )
        binding.floatingActionButtonRefresh.setOnClickListener{onRefresh()}
        setRecyclerView()
    }

//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//    }

    override fun onResume() {
        super.onResume()
        onRefresh()
    }

    private fun setRecyclerView() {
        adapter = ItemsRVAdapter()
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter=adapter
        binding.recyclerView.layoutManager= LinearLayoutManager(binding.root.context)

        adapter.setItemListener(object : RecyclerClickListener {

                //             Tap the 'X' to delete the note.
                override fun onItemRemoveClick(position: Int) {
    //                val itemsList = adapter.currentList.toMutableList()
    //                val idInt=itemsList[position].id
    //                val itemNameText = itemsList[position].itemName
    //                val itemPriceDuble = itemsList[position].itemPrice
    //                val quantityInStockInt = itemsList[position].quantityInStock
    //                val removeItem = Item(idInt,itemNameText,itemPriceDuble,quantityInStockInt)
    //                lifecycleScope.launch {
    //                    itemDatabase?.delete(removeItem)
                }

                // Tap the note to edit.
                override fun onItemClick (position: Int) {
                    val listItem = adapter.currentList.toMutableList()
                    val instBindle=newInstance(listItem[position])
                    binding.root.findNavController()
                        .navigate(R.id.action_itemListFragment2_to_addItemFragment,instBindle)
                }
            }
        )
    }

    private fun onRefresh(){
        observeNotesNotempty()
    }

     private fun observeNotesNotempty() {
        lifecycleScope.launch {
            itemDatabase?.getItemNotEmpty()?.collect { listItem ->
                submitListToFdapter(listItem)
            }
        }
    }

    private fun submitListToFdapter(listItem:List<Item>){
        binding.infoKolZap.text="Итого на сумму "
        setRecyclerView()
        adapter.submitList(listItem.toList())
    }

    companion object {
        @JvmStatic
        fun newInstance(item: Item):Bundle {
            val bundle = Bundle()
            bundle.putString("ARG_SH",item.itemSh)
            bundle.putString("ARG_SHTIP",item.itemShTip.toString())
            bundle.putString("ARG_NAME",item.itemName)
            bundle.putString("ARG_PRICE",item.itemPrice.toString())
            bundle.putString("ARG_QUANTITYINSTOCK",item.itemQuantityInStock.toString())
            bundle.putString("ARG_QUANTITY",item.itemQuantity.toString())
            return bundle
        }
    }
}