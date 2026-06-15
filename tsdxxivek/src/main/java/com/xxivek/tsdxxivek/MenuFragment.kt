package com.xxivek.tsdxxivek

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.xxivek.tsdxxivek.dataDB.UtilDB
import com.xxivek.tsdxxivek.databinding.FragmentMenuBinding
import com.xxivek.tsdxxivek.serverHTTP.ServerSocketXXI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import com.xxivek.tsdxxivek.utilAPP.appendLog


class MenuFragment : Fragment() {
    // Интерфейс доступа к базе данных
    //private var itemDatabase: ItemDao?=null

    // binding FragmentItemListBinding
    private var _binding: FragmentMenuBinding?=null
    val binding get() = _binding!!

    private var mCount=0
    private var mCountNotEmpty=0
    private var mInfoOutput=0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (appLic.appConnect1C==2){
            SERVERPORT= appLic.appPORT.toInt()
            if (SERVERPORT!!>0){
                appendLog("Сопряжение", "Запускаем сервер из главного меню")
    //            appLic.appConnect1C=false
                ServerSocketXXI().startServerClient()
            }
            // Подключаем базу данных с интерфейсом обработки данных
            appendLog("Сопряжение", "Подключаем базу данных с интерфейсом обработки данных из главного меню")
            itemDatabase=(activity?.application as TSDXXIVekApplication).database.itemDao()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentMenuBinding.inflate(inflater, container, false)

        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        // Подключаем базу данных с интерфейсом обработки данных
//        itemDatabase=(activity?.application as TSDXXIVekApplication).database.itemDao()

        if (DESIGN == 0) {
           binding.bScaner.setOnClickListener (
                Navigation.createNavigateOnClickListener(R.id.action_menuFragment_to_scanerFragment))
        } else if (DESIGN == 1) {
            binding.bScaner.setOnClickListener (
                Navigation.createNavigateOnClickListener(R.id.action_menuFragment_to_scanerFragment_d))
        }

        binding.bSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_menuFragment_to_settingsFragment)
        )
        binding.bExit.setOnClickListener{MainActivity().appExit()}

        binding.bInput.setOnClickListener{onInput()}
        binding.bOutput.setOnClickListener{
            UtilDB().writeXML()
        }
        binding.bClearCont.setOnClickListener{
            UtilDB().clearQuantity()
        }
        val navControler = binding.root.findNavController()
        val fm=getParentFragmentManager()
        binding.textViewOperInfo.setOnClickListener {
            if (appLic.appOper == "1") {
                navControler.navigate(R.id.action_menuFragment_to_itemListFragment1)
            } else if (appLic.appOper == "2") {
                navControler.navigate(R.id.action_menuFragment_to_itemListFragment2)
            } else if (appLic.appOper == "3") {
                navControler.navigate(R.id.action_menuFragment_to_itemListFragment)
            }else{
                OperDialog().show(fm, "Выберите операцию")
            }
        }
        appendLog("Главное меню","Проверяем настройки")
        appLic.conditionInfo()
        infoLiveData()
        appendLog("Главное меню","Настройки проверены")

    }

    private fun infoLiveData(){

        // Подписываемся на INFO_CountBD
        val infoCountBDliveDataObserver = Observer<Int>() {count->
            mCount=count
            appendLog("Главное меню","Подписываемся на INFO_CountBD")
        }
        val infoCountBDliveData=appLic.getLiveDataInfoCountBD()
        infoCountBDliveData.observe(viewLifecycleOwner, infoCountBDliveDataObserver)

        // Подписываемся на INFO_CountNotEmptyBD
        val infoCountNotEmptyBDliveDataObserver = Observer<Int>() {count->
            mCountNotEmpty=count
            appendLog("Главное меню","Подписываемся на INFO_CountNotEmptyBD")
        }
        val infoCountNotEmptyBDLiveData=appLic.getLiveDataInfoCountNotEmptyBD()
        infoCountNotEmptyBDLiveData.observe(viewLifecycleOwner, infoCountNotEmptyBDliveDataObserver)

        // Подписываемся на состояние БД
        val infoBDliveDataObserver = Observer<Int>() {
            // Раскрашиваем BD
            if (it==0){
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                binding.textViewBD.text="БД: В базе данных нет записей"
                binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_textColor))
                binding.textViewOperInfo.text="Операция не определена."
            }
            else if(it==1){
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.error))
                binding.textViewBD.text="БД: Ошибка в работе с базой данных"
            }
            else if(it==2){
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                binding.textViewBD.text="БД: Всего "+mCount+" зап., из них выб. "+mCountNotEmpty
                if (appLic.appOper=="") {
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!,R.color.text_textColor))
                    binding.textViewOperInfo.text = "Выберите вариант работы ТСД."
                }else if (appLic.appOper=="0"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.ok))
                    binding.textViewOperInfo.text="Инвентаризация."
                }else if (appLic.appOper=="1"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_prihod))
                    binding.textViewOperInfo.text="Приход.\n" +
                            "Поставщик: "+appLic.appClient
                }else if (appLic.appOper=="2"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
                    binding.textViewOperInfo.text="Расход.\n" +
                            "Клиент" + appLic.appClient+"\n"+
                            "на сумму 0 рублей"
                }else if (appLic.appOper=="3"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
                    binding.textViewOperInfo.text="Сверка.\n" + appLic.appClient
                }
            }
            else if(it==3){
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.ok))
                binding.textViewBD.text="БД: Всего "+mCount+" зап."
                if (appLic.appOper==""){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_textColor))
                    binding.textViewOperInfo.text="Выберите вариант работы ТСД."
                }else if (appLic.appOper=="0"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.ok))
                    binding.textViewOperInfo.text="Инвентаризация."
                }else if (appLic.appOper=="1"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_prihod))
                    binding.textViewOperInfo.text="Приход.\n" +
                            "Поставщик: "+appLic.appClient
                }else if (appLic.appOper=="2"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
                    binding.textViewOperInfo.text="Расход.\n" +
                            "Клиент: "+appLic.appClient
                }else if (appLic.appOper=="3"){
                    binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
                    binding.textViewOperInfo.text="Сверка.\n" +appLic.appClient
                }
            }
            else if(it==-1){
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.load))
                binding.textViewBD.text="БД: Подождите... Идет загрузка."
            }
            if(mInfoOutput==0){
                if (mCountNotEmpty>0) {
                    binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                    binding.textViewOutput.text ="Выгрузка: Можно выгрузить " + mCountNotEmpty + " зап."
                }else{
                    binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                    binding.textViewOutput.text ="Выгрузка: Отсутствуют записи для выгрузки"
                }
            }
            appendLog("Главное меню","Подписываемся на состояние БД")
        }
        val infoBDliveData=appLic.getLiveDataInfoBD()
        infoBDliveData.observe(viewLifecycleOwner, infoBDliveDataObserver)

        // Подписываемся на состояние INPUT
        val infoINPUTliveDataObserver = Observer<Int>() {
            if (it==0){
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                binding.textViewInput.text="Загрузка: Отсутствуют данные для загрузки"
            }
            else if(it==-1){
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.error))
                binding.textViewInput.text="Загрузка: Ошибка загрузки данных"
            }
            else if(it==-2){
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                binding.textViewInput.text="Загрузка: Что то не так"
            }
            else{
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.ok))
                binding.textViewInput.text="Загрузка: Поступило "+it+" зап."
            }
            appendLog("Главное меню","Подписываемся на состояние INPUT")
        }
        val infoINPUTliveData=appLic.getLiveDataInfoINPUT()
        infoINPUTliveData.observe(viewLifecycleOwner, infoINPUTliveDataObserver)

        // Подписываемся на состояние OUTPUT
        val infoOUTliveDataObserver = Observer<Int>() {
            mInfoOutput=it
            if (it==0){
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
            }
            else if(it==1){
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.error))
                binding.textViewOutput.text ="Выгрузка: Ошибка при попытке выгрузить данные"
            }
            else if(it==2){
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                binding.textViewOutput.text ="Выгрузка: Что то не так!"
            }
            else if(it==3){
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.ok))
                binding.textViewOutput.text ="Выгрузка: Данные отправлены"
            }
            appendLog("Главное меню","Подписываемся на состояние OUTPUT")
        }
        val infoOUTliveData=appLic.getLiveDataInfoOUT()
        infoOUTliveData.observe(viewLifecycleOwner, infoOUTliveDataObserver)


        if (appLic.appDEB=="1"){
            binding.bSettings.visibility=View.VISIBLE
        }else{
            binding.bSettings.visibility=View.GONE
        }
        appendLog("Главное меню","Состояние кнопки Настройки")
    }


    private fun onInput(){

        val builderAD = AlertDialog.Builder(binding.root.context)

        // set title
        builderAD.setTitle("Операции с БД")

        if (msg_server.isBlank()) {
            //set content area
            if (mCountNotEmpty>0) {
                builderAD.setMessage(
                    "Отсутствуют данные для загрузки.\n" +
                            "В базе данных есть записи с введенным количеством.\n"+
                            "Если Вы согласитьсь база данных будет очищена."
                )
            }else{
                builderAD.setMessage(
                    "Отсутствуют данные для загрузки.\n" +
                            "Если Вы согласитьсь база данных будет очищена."
                )
            }
            builderAD.setPositiveButton(
                "Очистить") { dialog, id ->
                onInputBD()
            }
            builderAD.setNegativeButton(
                "Отмена") { dialog, id ->
                // User clicked Update Now button
                //Toast.makeText(this, "Updating your device",Toast.LENGTH_SHORT).show()
            }
            builderAD.show()
        }else{
            if (mCountNotEmpty>0){
                //set content area
                builderAD.setMessage("В базе данных есть записи с введенным количеством.\n" +
                        "Если Вы согласитьсь база данных будет очищена.")
                builderAD.setPositiveButton(
                    "Загрузить") { dialog, id ->
                    onInputBD()
                }
                builderAD.setNegativeButton(
                    "Отмена") { dialog, id ->
                    // User clicked Update Now button
                    //Toast.makeText(this, "Updating your device",Toast.LENGTH_SHORT).show()
                }
                builderAD.show()
            }else{
              onInputBD()
            }
        }
        appendLog("Главное меню","Получение входящих даныых")
    }

    fun onInputBD(){
        CoroutineScope(IO).launch{
            UtilDB().onDelAllTables()
            UtilDB().readInputXML()
            appendLog("Главное меню","Отправка исходящих даныых")
        }
    }
}