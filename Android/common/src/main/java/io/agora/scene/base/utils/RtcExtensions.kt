package io.agora.scene.base.utils

import io.agora.rtc2.RtcEngine
import io.agora.scene.base.AgoraScenes
import io.agora.scene.base.BuildConfig
import org.json.JSONObject

enum class ReportAction(val value: Int) {
    Enter(0)
}

fun RtcEngine.reportRoom(
    userUid: String,
    scene: AgoraScenes,
    action: ReportAction = ReportAction.Enter,
    messageId: String = "agora:agoralive",
    appVersion: String = BuildConfig.APP_VERSION_NAME,
    currentTime: Long = TimeUtils.currentTimeMillis()
) {
    if (BuildConfig.DEBUG) {
        this.setParameters("{\"rtc.qos_for_test_purpose\": true}") // test env
    } else {
        this.setParameters("{\"rtc.direct_send_custom_event\": true}")
    }
    // Build category string
    val category = "${scene.value}_Android_$appVersion"
    // Prepare event and label maps
    val eventMap = mapOf("type" to action.value)
    val labelMap = mapOf("uid" to userUid, "ts" to currentTime)

    // Convert maps to JSON strings
    val event = eventMap.toJSONString() ?: ""
    val label = labelMap.toJSONString() ?: ""
    // Send the custom report message
    this.sendCustomReportMessage(messageId, category, event, label, 0)
}

// Helper extension function to convert a map to a JSON string
private fun Map<String, Any>.toJSONString(): String? {
    return try {
        JSONObject(this).toString()
    } catch (e: Exception) {
        null
    }
}
