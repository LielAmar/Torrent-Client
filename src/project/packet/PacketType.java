package project.packet;

public enum PacketType {

    CHOKE((byte) 0, 0),
    UNCHOKE((byte) 1, 0),
    INTERESTED((byte) 2, 0),
    NOT_INTERESTED((byte) 3, 0),
    HAVE((byte) 4, 4),
    BITFIELD((byte) 5, -1),
    REQUEST((byte) 6, 4),
    PIECE((byte) 7, -1),
    HANDSHAKE((byte) 8, 32),
    UNKNOWN((byte) 10, -1);


    private final byte typeId;
    private final int payloadSize;

    PacketType(byte type, int payloadSize) {
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

    public byte getTypeId() {
        return this.typeId;
    }
    public int getPayloadSize() {
        return this.payloadSize;
    }
}
