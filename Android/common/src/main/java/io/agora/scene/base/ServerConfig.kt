package io.agora.scene.base

import io.agora.scene.base.utils.SPUtil

object ServerConfig {

    private const val TOOLBOX_SERVER_HOST: String = "https://service.agora.io/toolbox-overseas"
    private const val TOOLBOX_SERVER_HOST_DEV: String = "https://service-staging.agora.io/toolbox-overseas"

    const val Env_Mode = "env_mode"

    @JvmStatic
    var envRelease: Boolean = SPUtil.getBoolean(Env_Mode, true)
        set(newValue) {
            field = newValue
            SPUtil.putBoolean(Env_Mode, newValue)
        }

    val toolBoxUrl: String
        get() {
            return if (envRelease) {
                TOOLBOX_SERVER_HOST
            } else {
                TOOLBOX_SERVER_HOST_DEV
            }
        }

    val roomManagerUrl: String
        get() {
            return toolBoxUrl.replace("toolbox", "room-manager")
        }
}