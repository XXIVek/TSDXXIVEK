package com.xxivek.tsdxxivek

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.xxivek.tsdxxivek.databinding.FragmentLogTextBinding
import java.io.File

class LogTextFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentLogTextBinding?=null
    val binding get() = _binding!!

    companion object {
        private const val TAG = "LogTextFragment"
        private const val LOG_FILE_PATH = "/storage/emulated/0/Download/logTSD.dat"
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentLogTextBinding.inflate(inflater, container, false)

        return binding.root
    }

    /**
     * Прочитать лог из файла logTSD.dat.
     */
    private fun readLogFile(): String {
        val logFile = File(LOG_FILE_PATH)
        return try {
            if (logFile.exists() && logFile.canRead()) {
                logFile.readText()
            } else {
                "Файл лога не найден: $LOG_FILE_PATH"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения лога", e)
            "Ошибка чтения файла лога: ${e.message}"
        }
    }

    override fun onResume() {
        super.onResume()
        // Читаем лог из файла (а не из переменной msg_log, которая сбрасывается при перезапуске)
        val logContent = readLogFile()
        if (logContent.isBlank()) {
            binding.textLog.text = "Лог пуст"
        } else {
            binding.textLog.text = logContent
        }
    }

    override fun onPause() {
        super.onPause()
        // Ничего не нужно делать
    }
}