package io.agora.scene.ktv.live.bean;

/**
 * The type Net work event.
 */
public class NetWorkEvent {
    /**
     * The Tx quality.
     */
    public int txQuality;
    /**
     * The Rx quality.
     */
    public int rxQuality;

    /**
     * Instantiates a new Net work event.
     *
     * @param txQuality the tx quality
     * @param rxQuality the rx quality
     */
    public NetWorkEvent(int txQuality, int rxQuality) {
        this.txQuality = txQuality;
        this.rxQuality = rxQuality;
    }

}
