package com.xxivek.tsdxxivek

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.xxivek.tsdxxivek.api.ExchangeDataParser
import com.xxivek.tsdxxivek.api.StatusPollingService
import com.xxivek.tsdxxivek.api.FileDownloadApi
import com.xxivek.tsdxxivek.api.PendingFileItem
import com.xxivek.tsdxxivek.api.DeviceStatusUpdate
import com.xxivek.tsdxxivek.dataDB.UtilDB
import com.xxivek.tsdxxivek.databinding.FragmentMenuBinding
import com.xxivek.tsdxxivek.serverHTTP.ServerSocketXXI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import com.xxivek.tsdxxivek.utilAPP.appendLog
import android.widget.Toast
import java.io.File


class MenuFragment : Fragment(), StatusPollingService.Callback {
    // Интерфейс доступа к базе данных
    //private var itemDatabase: ItemDao?=null

    // binding FragmentItemListBinding
    private var _binding: FragmentMenuBinding?=null
    val binding get() = _binding!!

    private var mCount=0
    private var mCountNotEmpty=0
    private var mInfoOutput=0

    // Polling service для опроса статуса устройства
    private var pollingService: StatusPollingService? = null
    private var apiClient: FileDownloadApi? = null
    private var exchangeParser: ExchangeDataParser? = null

    // Скачанные файлы, готовые к загрузке в БД
    private var downloadedFiles: MutableList<File> = mutableListOf()

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
        binding.bDownload.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_menuFragment_to_fileDownloadFragment)
        )
        binding.bExit.setOnClickListener{MainActivity().appExit()}

        binding.bInput.setOnClickListener { onInput() }
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

        // Запускаем polling статуса устройства (только для веб-режима)
        startStatusPolling()
    }

    // ----------------------------------------------------------------
    // StatusPollingService.Callback
    // ----------------------------------------------------------------

    override fun onStatusUpdated(input: Int, output: Int) {
        // Обновляем UI в main thread
        activity?.runOnUiThread {
            if (_binding != null) {
                updateInputStatusUI(input)
            }
        }
    }

    override fun onPendingDataAvailable(fileItems: List<PendingFileItem>) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(),
                "Получены данные: ${fileItems.size} файлов",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onFilesDownloaded(downloadedFiles: List<File>) {
        activity?.runOnUiThread {
            // Сохраняем скачанные файлы для последующей загрузки в БД
            this.downloadedFiles.clear()
            this.downloadedFiles.addAll(downloadedFiles)

            appendLog("Главное меню", "onFilesDownloaded: скачано ${downloadedFiles.size} файлов")
            for (f in downloadedFiles) {
                appendLog("Главное меню", "  файл: ${f.absolutePath}, exists=${f.exists()}, size=${f.length()}")
            }

            Toast.makeText(requireContext(),
                "Файлы скачаны: ${downloadedFiles.size}. Нажмите 'Загрузить'.",
                Toast.LENGTH_LONG
            ).show()

            // Обновляем UI — показываем что данные доступны
            updateInputStatusUI(3)
            appendLog("Главное меню", "Файлы скачаны: ${downloadedFiles.size}")
        }
    }

    override fun onPollError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(),
                "Ошибка опроса: $message",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Обновить textViewBD в зависимости от статуса БД.
     */
    private fun updateBDStatus(bdStatus: Int) {
        when (bdStatus) {
            0 -> {
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                binding.textViewBD.text = "БД: В базе данных нет записей"
                binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_textColor))
                binding.textViewOperInfo.text = "Операция не определена."
            }
            1 -> {
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.error))
                binding.textViewBD.text = "БД: Ошибка в работе с базой данных"
            }
            2 -> {
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                binding.textViewBD.text = "БД: Всего $mCount зап., из них выб. $mCountNotEmpty"
                updateOperInfo()
            }
            3 -> {
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.ok))
                binding.textViewBD.text = "БД: Всего $mCount зап."
                updateOperInfo()
            }
            -1 -> {
                binding.textViewBD.setBackgroundColor(ContextCompat.getColor(context!!, R.color.load))
                binding.textViewBD.text = "БД: Подождите... Идет загрузка."
            }
        }
    }

    /**
     * Обновить textViewOperInfo в зависимости от appOper.
     */
    private fun updateOperInfo() {
        if (appLic.appOper == "") {
            binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_textColor))
            binding.textViewOperInfo.text = "Выберите вариант работы ТСД."
        } else if (appLic.appOper == "0") {
            binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.ok))
            binding.textViewOperInfo.text = "Инвентаризация."
        } else if (appLic.appOper == "1") {
            binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_prihod))
            binding.textViewOperInfo.text = "Приход.\nПоставщик: ${appLic.appClient}"
        } else if (appLic.appOper == "2") {
            binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
            binding.textViewOperInfo.text = "Расход.\nКлиент: ${appLic.appClient}\nна сумму 0 рублей"
        } else if (appLic.appOper == "3") {
            binding.textViewOperInfo.setTextColor(ContextCompat.getColor(context!!, R.color.text_rashod))
            binding.textViewOperInfo.text = "Сверка.\n${appLic.appClient}"
        }
    }

    /**
     * Обновить textViewInput в зависимости от статуса input.
     * input=6 — данные получены (зелёный)
     * input=3 — данные доступны для загрузки (жёлтый)
     * input=0 — нет данных (серый)
     * input=-1 — ошибка (красный)
     */
    private fun updateInputStatusUI(input: Int) {
        when (input) {
            0 -> {
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_fon))
                binding.textViewInput.text = "Загрузка: Нет данных для загрузки"
            }
            3 -> {
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.attention))
                binding.textViewInput.text = "Загрузка: Данные доступны. Нажмите 'Загрузить'"
            }
            6 -> {
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ok))
                binding.textViewInput.text = "Загрузка: Данные получены! Нажмите 'Загрузить'"
            }
            -1 -> {
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
                binding.textViewInput.text = "Загрузка: Ошибка получения статуса"
            }
            else -> {
                binding.textViewInput.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_fon))
                binding.textViewInput.text = "Загрузка: Статус $input"
            }
        }
    }

    /**
     * Обновить textViewOutput в зависимости от статуса output.
     */
    private fun updateOutputStatusUI(output: Int) {
        when (output) {
            0 -> {
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                binding.textViewOutput.text = "Выгрузка: Отсутствуют записи для выгрузки"
            }
            1 -> {
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.error))
                binding.textViewOutput.text = "Выгрузка: Ошибка при попытке выгрузить данные"
            }
            2 -> {
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.attention))
                binding.textViewOutput.text = "Выгрузка: Что то не так!"
            }
            3 -> {
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.ok))
                binding.textViewOutput.text = "Выгрузка: Данные отправлены"
            }
            else -> {
                binding.textViewOutput.setBackgroundColor(ContextCompat.getColor(context!!, R.color.text_fon))
                binding.textViewOutput.text = "Выгрузка: Статус $output"
            }
        }
    }

    /**
     * Запустить polling статуса устройства.
     */
    private fun startStatusPolling() {
        // Проверяем что используется веб-режим
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val useSite = prefs.getBoolean("use_website", false)

        if (!useSite) {
            appendLog("Главное меню", "Polling не запущен: используется локальный режим")
            return
        }

        // Проверяем что устройство активировано
        val token = prefs.getString(AppConstants.APP_PREF_DEVICE_UUID, null)
        if (token.isNullOrEmpty()) {
            appendLog("Главное меню", "Polling не запущен: нет device_uuid")
            return
        }

        pollingService = StatusPollingService()
        apiClient = FileDownloadApi()
        exchangeParser = ExchangeDataParser()

        pollingService!!.setCallback(this)
        pollingService!!.startPolling(
            requireContext(),
            (requireActivity().application as TSDXXIVekApplication).appState,
            apiClient!!
        )

        appendLog("Главное меню", "Polling запущен (5 сек)")
    }

    /**
     * Остановить polling при уходе с экрана.
     */
    private fun stopStatusPolling() {
        pollingService?.stopPolling()
        pollingService = null
        appendLog("Главное меню", "Polling остановлен")
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
        val infoBDliveDataObserver = Observer<Int>() { bdStatus ->
            updateBDStatus(bdStatus)
            if (mInfoOutput == 0) {
                if (mCountNotEmpty > 0) {
                    updateOutputStatusUI(2)
                } else {
                    updateOutputStatusUI(0)
                }
            }
            appendLog("Главное меню", "Подписываемся на состояние БД")
        }
        val infoBDliveData = appLic.getLiveDataInfoBD()
        infoBDliveData.observe(viewLifecycleOwner, infoBDliveDataObserver)

        // Подписываемся на состояние INPUT
        val infoINPUTliveDataObserver = Observer<Int>() { inputStatus ->
            updateInputStatusUI(inputStatus)
            appendLog("Главное меню", "Подписываемся на состояние INPUT")
        }
        val infoINPUTliveData = appLic.getLiveDataInfoINPUT()
        infoINPUTliveData.observe(viewLifecycleOwner, infoINPUTliveDataObserver)

        // Подписываемся на состояние OUTPUT
        val infoOUTliveDataObserver = Observer<Int>() { outputStatus ->
            mInfoOutput = outputStatus
            updateOutputStatusUI(outputStatus)
            appendLog("Главное меню", "Подписываемся на состояние OUTPUT")
        }
        val infoOUTliveData = appLic.getLiveDataInfoOUT()
        infoOUTliveData.observe(viewLifecycleOwner, infoOUTliveDataObserver)


        // Временно кнопка "Настройки" всегда видна
        binding.bSettings.visibility=View.VISIBLE
        appendLog("Главное меню","Состояние кнопки Настройки")
    }


    private fun onInput(){
        val context = requireContext()
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val useWebsite = prefs.getBoolean("use_website", false)
        val downloadedCount = downloadedFiles.size

        appendLog("Главное меню", "onInput: use_website=$useWebsite, downloadedFiles=$downloadedCount")

        // Веб-режим: загрузка скачанных файлов из сайта
        if (useWebsite) {
            appendLog("Главное меню", "onInput: веб-режим, проверка downloadedFiles")
            if (downloadedFiles.isEmpty()) {
                appendLog("Главное меню", "onInput: веб-режим, downloadedFiles пуст!")
                Toast.makeText(context, "Нет скачанных файлов для загрузки", Toast.LENGTH_SHORT).show()
            } else {
                appendLog("Главное меню", "onInput: веб-режим, загружаем ${downloadedFiles.size} файлов")
                loadDownloadedFilesToDB()
            }
            return
        }

        // Локальный режим: старый сценарий
        val builderAD = AlertDialog.Builder(binding.root.context)
        builderAD.setTitle("Операции с БД")

        if (msg_server.isBlank()) {
            if (mCountNotEmpty > 0) {
                builderAD.setMessage(
                    "Отсутствуют данные для загрузки.\n" +
                            "В базе данных есть записи с введенным количеством.\n" +
                            "Если Вы согласитесь база данных будет очищена."
                )
            } else {
                builderAD.setMessage(
                    "Отсутствуют данные для загрузки.\n" +
                            "Если Вы согласитесь база данных будет очищена."
                )
            }
            builderAD.setPositiveButton("Очистить") { _, _ -> onInputBD() }
            builderAD.setNegativeButton("Отмена") { _, _ -> }
            builderAD.show()
        } else {
            if (mCountNotEmpty > 0) {
                builderAD.setMessage(
                    "В базе данных есть записи с введенным количеством.\n" +
                            "Если Вы согласитесь база данных будет очищена."
                )
                builderAD.setPositiveButton("Загрузить") { _, _ -> onInputBD() }
                builderAD.setNegativeButton("Отмена") { _, _ -> }
                builderAD.show()
            } else {
                onInputBD()
            }
        }
        appendLog("Главное меню", "Получение входящих данных (локальный режим)")
    }

    fun onInputBD(){
        CoroutineScope(IO).launch{
            UtilDB().onDelAllTables()
            UtilDB().readInputXML()
            appendLog("Главное меню","Отправка исходящих даныых")
        }
    }

    // ----------------------------------------------------------------
    // Загрузка скачанных файлов в БД
    // ----------------------------------------------------------------

    /**
     * Загрузить скачанные файлы из директории downloads в БД.
     * После загрузки сбрасывает статус input=0 на сервере.
     */
    private fun loadDownloadedFilesToDB() {
        appendLog("Главное меню", "loadDownloadedFilesToDB: начало, downloadedFiles.size=${downloadedFiles.size}")

        if (downloadedFiles.isEmpty()) {
            appendLog("Главное меню", "loadDownloadedFilesToDB: downloadedFiles пуст!")
            Toast.makeText(requireContext(),
                "Нет скачанных файлов",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val context = requireContext()
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val token = prefs.getString(AppConstants.APP_PREF_DEVICE_UUID, null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(),
                "device_uuid не найден",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        CoroutineScope(IO).launch {
            var successCount = 0
            var totalItems = 0

            for (file in downloadedFiles) {
                val parser = exchangeParser ?: continue
                val result = parser.parseFile(file)

                if (result.success && result.data != null) {
                    // Парсим JSON данные в список Item
                    val items = parser.parseDataToList(result.data)
                    totalItems += items.size

                    if (items.isNotEmpty()) {
                        // Записываем в БД
                        UtilDB().insertItemList(items)
                        appendLog("Главное меню", "Записано в БД: ${items.size} записей из ${file.name}")
                        successCount++
                    } else {
                        appendLog("Главное меню", "Нет данных для записи из ${file.name}")
                    }
                } else {
                    appendLog("Главное меню", "Ошибка парсинга: ${result.fileName}: ${result.message}")
                }
            }

            // Сбрасываем статус input=0, bd=3 на сервере
            val appState = (requireActivity().application as TSDXXIVekApplication).appState
            val updateResult = apiClient?.updateDeviceStatusSync(
                context,
                token,
                DeviceStatusUpdate(
                    pairing = true,
                    konf = appLic.appKONF.toIntOrNull() ?: 0,
                    bd = 3,
                    input = 0,
                    output = appState.appOutputStatus
                )
            )

            appendLog("Главное меню", "updateDeviceStatusSync: status=${updateResult?.status}, bd=3, input=0")

            // Удаляем скачанные файлы с устройства
            for (file in downloadedFiles) {
                if (file.exists()) {
                    file.delete()
                    appendLog("Главное меню", "Удалён файл: ${file.absolutePath}")
                }
            }
            downloadedFiles.clear()

            // Обновляем UI
            activity?.runOnUiThread {
                updateInputStatusUI(0)
                Toast.makeText(requireContext(),
                    "Загружено в БД: $totalItems записей из $successCount файлов",
                    Toast.LENGTH_LONG
                ).show()
                appendLog("Главное меню", "Загружено в БД: $totalItems записей из $successCount файлов, bd=3, input=0")
            }
        }
    }

    // ----------------------------------------------------------------
    // Lifecycle
    // ----------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        // Запускаем polling при возврате на экран
        if (pollingService == null || !pollingService!!.isPollingRunning()) {
            startStatusPolling()
        }
    }

    override fun onPause() {
        super.onPause()
        // Останавливаем polling при уходе с экрана
        stopStatusPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Останавливаем polling при уничтожении view
        stopStatusPolling()
        _binding = null
    }
}