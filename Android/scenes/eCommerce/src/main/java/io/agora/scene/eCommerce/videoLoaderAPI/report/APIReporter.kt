package io.agora.scene.eCommerce.videoLoaderAPI.report

import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngine
import org.json.JSONObject
import java.util.HashMap

/**
 * A p i type
 *
 * @property value
 * @constructor Create empty A p i type
 */
enum class APIType(val value: Int) {
    /**
     * Ktv
     *
     * @constructor Create empty Ktv
     */
    KTV(1),

    /**
     * Call
     *
     * @constructor Create empty Call
     */
    CALL(2),

    /**
     * Beauty
     *
     * @constructor Create empty Beauty
     */
    BEAUTY(3),

    /**
     * Video Loader
     *
     * @constructor Create empty Video Loader
     */
    VIDEO_LOADER(4),

    /**
     * Pk
     *
     * @constructor Create empty Pk
     */
    PK(5),

    /**
     * Virtual Space
     *
     * @constructor Create empty Virtual Space
     */
    VIRTUAL_SPACE(6),

    /**
     * Screen Space
     *
     * @constructor Create empty Screen Space
     */
    SCREEN_SPACE(7),

    /**
     * Audio Scenario
     *
     * @constructor Create empty Audio Scenario
     */
    AUDIO_SCENARIO(8)
}

/**
 * Api event type
 *
 * @property value
 * @constructor Create empty Api event type
 */
enum class ApiEventType(val value: Int) {
    /**
     * Api
     *
     * @constructor Create empty Api
     */
    API(0),

    /**
     * Cost
     *
     * @constructor Create empty Cost
     */
    COST(1),

    /**
     * Custom
     *
     * @constructor Create empty Custom
     */
    CUSTOM(2)
}

object ApiEventKey {
    const val TYPE = "type"
    const val DESC = "desc"
    const val API_VALUE = "apiValue"
    const val TIMESTAMP = "ts"
    const val EXT = "ext"
}

object ApiCostEvent {
    const val CHANNEL_USAGE = "channelUsage"
    const val FIRST_FRAME_ACTUAL = "firstFrameActual"
    const val FIRST_FRAME_PERCEIVED = "firstFramePerceived"
}

/**
 * A p i reporter
 *
 * @property type
 * @property version
 * @property rtcEngine
 * @constructor Create empty A p i reporter
 */
class APIReporter(
    private val type: APIType,
    private val version: String,
    private val rtcEngine: RtcEngine
) {
    private val tag = "APIReporter"
    private val messageId = "agora:scenarioAPI"
    private val durationEventStartMap = HashMap<String, Long>()
    private val category = "${type.value}_Android_$version"

    init {
        configParameters()
    }

    /**
     * Report func event
     *
     * @param name
     * @param value
     * @param ext
     */
    fun reportFuncEvent(name: String, value: Map<String, Any>, ext: Map<String, Any>) {
        Log.d(tag, "reportFuncEvent: $name value: $value ext: $ext")
        val eventMap = mapOf(ApiEventKey.TYPE to ApiEventType.API.value, ApiEventKey.DESC to name)
        val labelMap = mapOf(ApiEventKey.API_VALUE to value, ApiEventKey.TIMESTAMP to getCurrentTs(), ApiEventKey.EXT to ext)
        val event = convertToJSONString(eventMap) ?: ""
        val label = convertToJSONString(labelMap) ?: ""
        rtcEngine.sendCustomReportMessage(messageId, category, event, label, 0)
    }

    /**
     * Start duration event
     *
     * @param name
     */
    fun startDurationEvent(name: String) {
        Log.d(tag, "startDurationEvent: $name")
        durationEventStartMap[name] = getCurrentTs()
    }

    /**
     * End duration event
     *
     * @param name
     * @param ext
     */
    fun endDurationEvent(name: String, ext: Map<String, Any>) {
        Log.d(tag, "endDurationEvent: $name")
        val beginTs = durationEventStartMap[name] ?: return
        durationEventStartMap.remove(name)
        val ts = getCurrentTs()
        val cost = (ts - beginTs).toInt()

        innerReportCostEvent(ts, name, cost, ext)
    }

    /**
     * Report cost event
     *
     * @param name
     * @param cost
     * @param ext
     */
    fun reportCostEvent(name: String, cost: Int, ext: Map<String, Any>) {
        durationEventStartMap.remove(name)
        innerReportCostEvent(
            ts = getCurrentTs(),
            name = name,
            cost = cost,
            ext = ext
        )
    }

    /**
     * Report custom event
     *
     * @param name
     * @param ext
     */
    fun reportCustomEvent(name: String, ext: Map<String, Any>) {
        Log.d(tag, "reportCustomEvent: $name ext: $ext")
        val eventMap = mapOf(ApiEventKey.TYPE to ApiEventType.CUSTOM.value, ApiEventKey.DESC to name)
        val labelMap = mapOf(ApiEventKey.TIMESTAMP to getCurrentTs(), ApiEventKey.EXT to ext)
        val event = convertToJSONString(eventMap) ?: ""
        val label = convertToJSONString(labelMap) ?: ""
        rtcEngine.sendCustomReportMessage(messageId, category, event, label, 0)
    }

    /**
     * Write log
     *
     * @param content
     * @param level
     */
    fun writeLog(content: String, level: Int) {
        //rtcEngine.writeLog(level, content)
    }

    /**
     * Clean cache
     *
     */
    fun cleanCache() {
        durationEventStartMap.clear()
    }

    // ---------------------- private ----------------------

    private fun configParameters() {
        //rtcEngine.setParameters("{\"rtc.qos_for_test_purpose\": true}") // test env
        rtcEngine.setParameters("{\"rtc.direct_send_custom_event\": true}")
        rtcEngine.setParameters("{\"rtc.log_external_input\": true}")
    }

    private fun getCurrentTs(): Long {
        return System.currentTimeMillis()
    }

    private fun innerReportCostEvent(ts: Long, name: String, cost: Int, ext: Map<String, Any>) {
        Log.d(tag, "reportCostEvent: $name cost: $cost ms ext: $ext")
        //writeLog("reportCostEvent: $name cost: $cost ms", Constants.LOG_LEVEL_INFO)
        val eventMap = mapOf(ApiEventKey.TYPE to ApiEventType.COST.value, ApiEventKey.DESC to name)
        val labelMap = mapOf(ApiEventKey.TIMESTAMP to ts, ApiEventKey.EXT to ext)
        val event = convertToJSONString(eventMap) ?: ""
        val label = convertToJSONString(labelMap) ?: ""
        rtcEngine.sendCustomReportMessage(messageId, category, event, label, cost)
    }

    private fun convertToJSONString(dictionary: Map<String, Any>): String? {
        return try {
            JSONObject(dictionary).toString()
        } catch (e: Exception) {
            //writeLog("[$tag]convert to json fail: $e dictionary: $dictionary", Constants.LOG_LEVEL_WARNING)
            null
        }
    }
}