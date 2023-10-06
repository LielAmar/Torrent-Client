package project.packet;

public enum PacketType {

    CHOKE(0, 0),
    UNCHOKE(1, 0),
    INTERESTED(2, 0),
    NOT_INTERESTED(3, 0),
    HAVE(4, 4),
    BITFIELD(5, -1),
    REQUEST(6, 4),
    PIECE(7, -1),
    HANDSHAKE(8, 32),
    UNKNOWN(10, -1);


    private final int typeId;
    private final int payloadSize;

    PacketType(int type, int payloadSize) {
        this.typeId = type;
        this.payloadSize = payloadSize;
    }

    public static PacketType fromValue(int value) {
        for(PacketType messageType : PacketType.values()) {
            if(messageType.typeId == value) {
                return messageType;
            }
        }

        return UNKNOWN;
    }

    public static PacketType fromPayload(byte[] payload) {
        return fromValue(payload[4]);
    }

    public int getTypeId() {
        return this.typeId;
    }
    public int getPayloadSize() {
        return this.payloadSize;
    }
}
