package io.agora.scene.dreamFlow.service

interface IDreamFlowStateListener {

    fun onStatusChanged(status: DreamFlowService.ServiceStatus)

    fun onLoadingProgressChanged(progress: Int)

}