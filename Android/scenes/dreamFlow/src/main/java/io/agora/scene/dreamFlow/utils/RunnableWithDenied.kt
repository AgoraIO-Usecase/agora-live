package io.agora.scene.dreamFlow.utils

/**
 * Runnable with denied
 *
 * @constructor Create empty Runnable with denied
 */
abstract class RunnableWithDenied : Runnable{
    /**
     * On denied
     *
     */
    abstract fun onDenied()
}