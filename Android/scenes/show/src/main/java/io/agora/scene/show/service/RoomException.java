package io.agora.scene.show.service;

/**
 * The type Room exception.
 */
public class RoomException extends RuntimeException {

    /**
     * The Curr room no.
     */
    private String currRoomNo;

    /**
     * Instantiates a new Room exception.
     *
     * @param message    the message
     * @param currRoomNo the curr room no
     */
    public RoomException(String message, String currRoomNo) {
        super(message);
        this.currRoomNo = currRoomNo;
    }

    /**
     * Gets curr room no.
     *
     * @return the curr room no
     */
    public String getCurrRoomNo() {
        return currRoomNo;
    }

    /**
     * Sets curr room no.
     *
     * @param currRoomNo the curr room no
     * @return the curr room no
     */
    public RoomException setCurrRoomNo(String currRoomNo) {
        this.currRoomNo = currRoomNo;
        return this;
    }
}
