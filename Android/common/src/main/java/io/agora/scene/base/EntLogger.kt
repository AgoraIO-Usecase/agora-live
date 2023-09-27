package io.agora.scene.base

import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.CsvFormatStrategy
import com.orhanobut.logger.DiskLogAdapter
import com.orhanobut.logger.DiskLogStrategy
import com.orhanobut.logger.LogcatLogStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import io.agora.scene.base.component.AgoraApplication
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Ent logger
 *
 * @property config
 * @constructor Create empty Ent logger
 */
class EntLogger(private val config: Config) {

    companion object {
        private val FILE_SIZE_STEP = 1024
        private val LogFolder = AgoraApplication.the().getExternalFilesDir("")!!.absolutePath
        private val LogFileWriteThread by lazy {
            HandlerThread("AndroidFileLogger.$LogFolder").apply {
                start()
            }
        }
    }



    /**
     * Config
     *
     * @property sceneName
     * @property fileSize
     * @property fileName
     * @constructor Create empty Config
     */
    data class Config(
        val sceneName: String,
        val fileSize: Int = 2 * FILE_SIZE_STEP * FILE_SIZE_STEP, // 2M，单位Byte
        val fileName: String = ("agora_ent_${sceneName}_Android_" +
                "${SimpleDateFormat("yyyy-MM-DD", Locale.US).format(Date())}_log").lowercase()
    )

    /**
     * Data format
     */
    private val dataFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        if (BuildConfig.DEBUG) {
            Logger.addLogAdapter(
                object: AndroidLogAdapter(
                    PrettyFormatStrategy.newBuilder()
                        .showThreadInfo(true)// (Optional) Whether to show thread info or not. Default true
                        .methodCount(1)// (Optional) How many method line to show. Default 2
                        .methodOffset(2)// (Optional) Hides internal method calls up to offset. Default 5
                        // (Optional) Changes the log strategy to print out. Default LogCat
                        .logStrategy(LogcatLogStrategy())
                        .tag(config.sceneName)// (Optional) Global tag for every log. Default PRETTY_LOGGER
                        .build()
                ){
                    override fun isLoggable(priority: Int, tag: String?): Boolean {
                        return tag == config.sceneName
                    }
                }
            )
        }
        Logger.addLogAdapter(
            object: DiskLogAdapter(
                CsvFormatStrategy
                    .newBuilder()
                    .logStrategy(DiskLogStrategy(WriteHandler(config)))
                    .tag(config.sceneName)
                    .build()
            ){
                override fun isLoggable(priority: Int, tag: String?): Boolean {
                    return tag == config.sceneName
                }
            }
        )
    }


    /**
     * I
     *
     * @param tag
     * @param message
     * @param args
     */
    fun i(tag: String? = null, message: String, vararg args: Any) {
        Logger.t(config.sceneName).i(formatMessage("INFO", tag, message), args)
    }


    /**
     * W
     *
     * @param tag
     * @param message
     * @param args
     */
    fun w(tag: String? = null, message: String, vararg args: Any) {
        Logger.t(config.sceneName).w(formatMessage("Warn", tag, message), args)
    }

    /**
     * D
     *
     * @param tag
     * @param message
     * @param args
     */
    fun d(tag: String? = null, message: String, vararg args: Any) {
        Logger.t(config.sceneName).d(formatMessage("Debug", tag, message), args)
    }

    /**
     * E
     *
     * @param tag
     * @param message
     * @param args
     */
    fun e(tag: String? = null, message: String, vararg args: Any) {
        Logger.t(config.sceneName).e(formatMessage("Error", tag, message), args)
    }

    /**
     * E
     *
     * @param tag
     * @param throwable
     * @param message
     * @param args
     */
    fun e(tag: String? = null, throwable: Throwable, message: String, vararg args: Any) {
        Logger.t(config.sceneName).e(throwable, formatMessage("Error", tag, message), args)
    }

    /**
     * Format message
     *
     * @param level
     * @param tag
     * @param message
     * @return
     */
    private fun formatMessage(level: String, tag: String?, message: String): String {
        val sb = StringBuilder("[Agora][${level}][${config.sceneName}]")
        tag?.let { sb.append("[${tag}]"); }
        sb.append(" : (${dataFormat.format(Date())}) : $message")
        return sb.toString()
    }


    /**
     * Write handler
     *
     * @constructor Create empty Write handler
     */
    private class WriteHandler(private val config: Config) : Handler(LogFileWriteThread.looper) {

        /**
         * Handle message
         *
         * @param msg
         */
        override fun handleMessage(msg: Message) {
            val content = msg.obj as String
            var fileWriter: FileWriter? = null
            val logFile = getLogFile(LogFolder, config.fileName)
            try {
                fileWriter = FileWriter(logFile, true)
                writeLog(fileWriter, content)
                fileWriter.flush()
                fileWriter.close()
            } catch (e: Exception) {
                if (fileWriter != null) {
                    try {
                        fileWriter.flush()
                        fileWriter.close()
                    } catch (e1: Exception) { /* fail silently */
                    }
                }
            }
        }

        /**
         * Write log
         *
         * @param fileWriter
         * @param content
         */
        @Throws(IOException::class)
        private fun writeLog(fileWriter: FileWriter, content: String) {
            var writeContent = content
            val agoraTag = writeContent.indexOf("[Agora]")
            if (agoraTag > 0) {
                writeContent = writeContent.substring(agoraTag)
            }
            fileWriter.append(writeContent)
        }

        /**
         * Get log file
         *
         * @param folderName
         * @param fileName
         * @return
         */
        private fun getLogFile(folderName: String, fileName: String): File {
            val folder = File(folderName)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            var newFileCount = 0
            var newFile: File
            var existingFile: File? = null
            newFile = File(folder, getLogFileFullName(fileName, newFileCount))
            while (newFile.exists()) {
                existingFile = newFile
                newFileCount++
                newFile = File(folder, getLogFileFullName(fileName, newFileCount))
            }
            if (existingFile != null && existingFile.length() < config.fileSize) {
                return existingFile
            } else {
                return newFile
            }
        }

        /**
         * Get log file full name
         *
         * @param fileName
         * @param count
         * @return
         */
        private fun getLogFileFullName(fileName: String, count: Int) : String{
            if(count == 0){
                return "${fileName}.txt"
            }
            return "${fileName}_${count}.txt"
        }
    }

}

