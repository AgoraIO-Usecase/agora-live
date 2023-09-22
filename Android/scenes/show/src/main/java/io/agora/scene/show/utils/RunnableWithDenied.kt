package io.agora.scene.show.utils

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