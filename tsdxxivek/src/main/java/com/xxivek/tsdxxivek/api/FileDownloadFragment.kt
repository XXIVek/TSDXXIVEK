package com.xxivek.tsdxxivek.api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.xxivek.tsdxxivek.R

/**
 * Fragment для скачивания файлов с сервера.
 *
 * Поток работы:
 * 1. При входе загружает список pending файлов
 * 2. Показывает список пользователю
 * 3. По нажатию на элемент — скачивает файл
 * 4. Обновляет статус устройства
 */
class FileDownloadFragment : Fragment() {

    // ViewModel через companion — чтобы не создавать новый при каждом recreation
    private val viewModel: FileDownloadViewModel by viewModels()

    // Адаптер для списка файлов
    private lateinit var filesListView: ListView
    private lateinit var filesAdapter: ArrayAdapter<String>
    private lateinit var filesList: MutableList<PendingFileItem>
    // Отдельный список для отображения (ArrayAdapter<String> требует String)
    private val displayList = mutableListOf<String>()

    // UI-элементы
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var btnUpdateStatus: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Используем стандартный layout с ListView
        val view = inflater.inflate(R.layout.fragment_file_download, container, false)

        // Инициализация UI-элементов
        filesListView = view.findViewById(R.id.filesListView)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvStatus = view.findViewById(R.id.tvStatus)
        progressBar = view.findViewById(R.id.progressBar)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        btnUpdateStatus = view.findViewById(R.id.btnUpdateStatus)

        // Инициализация списка
        filesList = mutableListOf()
        filesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            displayList
        )
        filesListView.adapter = filesAdapter

        // Обработчик выбора элемента
        filesListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val item = filesList[position]
            downloadFile(item)
        }

        // Кнопка обновления списка
        btnRefresh.setOnClickListener {
            viewModel.getPendingFiles()
        }

        // Кнопка обновления статуса устройства
        btnUpdateStatus.setOnClickListener {
            // Пример: input=2, output=4 (настроить под реальные значения ТСД)
            viewModel.updateDeviceStatus(input = 2, output = 4)
        }

        // Кнопка назад
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            view.findNavController().navigateUp()
        }

        // Загружаем список при создании
        viewModel.getPendingFiles()

        return view
    }

    /**
     * Подписка на изменения LiveData и обновление UI.
     */
    private fun observeViewModel() {
        // Список pending файлов
        viewModel.pendingFiles.observe(viewLifecycleOwner, Observer { items ->
            filesList.clear()
            displayList.clear()
            if (items != null && items.isNotEmpty()) {
                // Формируем отображаемый список: "filename (subject, date)"
                filesList.addAll(items)
                val displayItems = items.map { item ->
                    val subject = if (item.subject.isNotBlank()) item.subject else "Без темы"
                    val date = item.metadata?.date ?: item.created_at
                    "${item.filename}\n  $subject | $date"
                }
                displayList.addAll(displayItems)
                filesAdapter.notifyDataSetChanged()
                tvStatus.text = "Файлов: ${items.size}"
            } else {
                tvStatus.text = "Нет pending файлов"
            }
        })

        // Индикатор загрузки
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            btnRefresh.isEnabled = !loading
            btnUpdateStatus.isEnabled = !loading
        })

        // Сообщение об успехе
        viewModel.successMessage.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        })

        // Сообщение об ошибке
        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearMessages()
            }
        })

        // Текущий скачиваемый файл
        viewModel.downloadingFile.observe(viewLifecycleOwner, Observer { filename ->
            if (filename != null) {
                tvStatus.text = "Скачивание: $filename..."
            }
        })

        // Статус устройства
        viewModel.deviceStatus.observe(viewLifecycleOwner, Observer { status ->
            status?.let {
                tvStatus.text = "Статус: pairing=${it.pairing}, input=${it.input}, output=${it.output}"
            }
        })
    }

    /**
     * Скачивание файла по нажатию на элемент списка.
     */
    private fun downloadFile(item: PendingFileItem) {
        // Извлекаем message_id из file_url
        val messageId = item.id
        if (messageId.isBlank()) {
            Toast.makeText(requireContext(), "Неверный ID файла", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.downloadSelectedFile(messageId, item.filename)
    }
}
