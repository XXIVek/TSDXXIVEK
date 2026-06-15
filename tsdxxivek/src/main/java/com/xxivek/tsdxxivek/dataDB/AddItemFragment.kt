package com.xxivek.tsdxxivek.dataDB

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xxivek.tsdxxivek.R
import com.xxivek.tsdxxivek.TSDXXIVekApplication
import com.xxivek.tsdxxivek.appLic
import com.xxivek.tsdxxivek.databinding.FragmentAddItemBinding

class AddItemFragment : Fragment() {
    private var strSh =""
    private var strShTip=""
    private var strName = ""
    private var strPrice = ""
    private var strQuantityInStock = ""
    private var strQuantity = ""
    private var strView = ""

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    lateinit var item: Item
    //Позволяет отслеживать навигацию по экранам
//    lateinit var navController: NavController


    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    // to share the ViewModel across fragments.
    private val viewModel: InventoryViewModel by activityViewModels {
        InventoryViewModelFactory(
            (activity?.application as TSDXXIVekApplication).database.itemDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            strShTip = it.getString("ARG_SHTIP").toString()
            strSh = it.getString("ARG_SH").toString()
            strName = it.getString("ARG_NAME").toString()
            strPrice = it.getString("ARG_PRICE").toString()
            strQuantityInStock = it.getString("ARG_QUANTITYINSTOCK").toString()
            strQuantity = it.getString("ARG_QUANTITY").toString()
            strView = it.getString("ARG_VIEW").toString()
        }

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called when the view is created.
     * The itemId Navigation argument determines the edit item  or add new item.
     * If the itemId is positive, this method retrieves the information from the database and
     * allows the user to update it.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (appLic.appOper=="1"){
            binding.layoutEdit.visibility=View.GONE
            binding.prTvTipsh.setText(strShTip)
            binding.prTvSh.setText(strSh)
            binding.prTinpetName.setText(strName)
            binding.prTinpetCount.setText(strQuantity)
            if (strView=="ScanerFragment"||strView=="ScanerFragment_d"){
                binding.fabDelete.visibility=View.GONE
            }
            binding.layoutPrihod.visibility=View.VISIBLE
        }else{
            binding.layoutPrihod.visibility=View.GONE
            binding.editTvTipsh.setText(strShTip)
            binding.editTvSh.setText(strSh)
            binding.editTvName.setText(strName)
            binding.editTvPrice.setText(strPrice)
            binding.editTvCountToStock.setText(strQuantityInStock)
            binding.editTinpetCount.setText(strQuantity)
            binding.fabDelete.visibility=View.GONE
            binding.layoutEdit.visibility=View.VISIBLE
        }
        binding.fabEdit.setOnClickListener {addNewItem("Add")}
        binding.fabDelete.setOnClickListener {showConfirmationDialog()}
        binding.fabReturn.setOnClickListener {findNavController().navigateUp()}
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    private fun isEntryValid(): Boolean {
        if (appLic.appOper=="1"){
            strName= binding.prTinpetName.text.toString()
            strQuantity= binding.prTinpetCount.text.toString()
        }else{
            strQuantity= binding.editTinpetCount.text.toString()
        }
        return viewModel.isEntryValid(
            strSh,
            strShTip,
            strName,
        )
    }

    /**
     * Inserts the new Item into database and navigates up to list fragment.
     */
    private fun addNewItem(mVariant:String) {
        if (isEntryValid()) {
            if (mVariant=="Delite"){
                strQuantity="0"
            }
            viewModel.addNewItem(
                strSh,
                strShTip,
                strName,
                strPrice,
                strQuantityInStock,
                strQuantity
            )
         }
        findNavController().navigateUp()
    }

    /**
     * Отображает диалоговое окно предупреждения для получения подтверждения
     * пользователя перед удалением элемента.
     */
    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Предупреждение")
            .setMessage("Да-удалить элемент, Нет-очистить количество")
            .setCancelable(false)
            .setNegativeButton(getString(R.string.no)) { _, _ ->
                strQuantity="0"
                addNewItem("Delite")
            }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                deleteItem()
            }
            .show()
    }

    /**
     * Удаляет текущий элемент и переходит к фрагменту списка.
     */
    private fun deleteItem() {
        viewModel.deleteItem(
            strSh,
            strShTip,
            strName,
            strPrice,
            strQuantityInStock,
            strQuantity
        )
        findNavController().navigateUp()
    }


    /**
     * Called before fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard.
        val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }

}