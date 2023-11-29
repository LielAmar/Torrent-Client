package project.message.InternalMessage;

public enum InternalMessageType {

    CHOKE_THREAD((byte) 0),
    UNCHOKE_THREAD((byte) 1),
    RECEIVED((byte) 2),
    TERMINATE((byte) 3),
    NEW_LOCAL_PIECE((byte) 4),
    UNKNOWN((byte) 10);


    private final byte typeId;

    InternalMessageType(byte type) {
        this.typeId = type;
    }

    public byte getTypeId() {
        return this.typeId;
    }


    /**
     * Given a packet type number, return a matching PacketType object
     *
     * @param type   Type of the packet
     * @return       PacketType object matching the given type
     */
    public static InternalMessageType fromPacketType(int type) {
        for (InternalMessageType messageType : InternalMessageType.values()) {
            if (messageType.typeId == type) {
                return messageType;
            }
        }

        return UNKNOWN;
    }
    
}
