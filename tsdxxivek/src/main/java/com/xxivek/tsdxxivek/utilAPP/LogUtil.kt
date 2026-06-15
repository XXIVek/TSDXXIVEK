package com.xxivek.tsdxxivek.utilAPP

import android.util.Log
import com.xxivek.tsdxxivek.LOGING
import com.xxivek.tsdxxivek.msg_log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

//class LogUtil {
//    var m_log_tag=LOG_TAG
    fun createLog(LOG_TAG:String,LOG_message: String){
        if (LOGING){
            if (msg_log==null){
                msg_log="Начало логирования\n\n"
            }
            msg_log =msg_log+LOG_TAG+": "+LOG_message+"\n"
            Log.e(LOG_TAG, LOG_message)
        }
    }
    fun appendLog(tag: String?,msg: String?) {
        val logFile = File("sdcard/Download/logTSD.dat")
        val timeLog = SimpleDateFormat("dd.MM.yy hh:mm:ss").format(Date())
        if (LOGING) {
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                val buf = BufferedWriter(FileWriter(logFile, true))
                buf.append(timeLog + "(" + tag + ")" + msg)
                buf.newLine()
                buf.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }
//}