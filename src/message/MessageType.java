package message;

import java.io.BufferedInputStream;

public enum MessageType {

    CHOKE(0, 0),
    UNCHOKE(1, 0),
    INTERESTED(2, 0),
    NOT_INTERESTED(3, 0),
    HAVE(4, 4),
    BITFIELD(5, -1),
    REQUEST(6, 4),
    PIECE(7, -1),
    UNKNOWN(10, -1);


    private final int typeId;
    private final int payloadSize;

    MessageType(int type, int payloadSize) {
        this.typeId = type;
        this.payloadSize = payloadSize;
    }

    public static MessageType fromValue(int value) {
        for(MessageType messageType : MessageType.values()) {
            if(messageType.typeId == value) {
                return messageType;
            }
        }

        return UNKNOWN;
    }

    public static MessageType fromRawData(byte[] rawData) {
        return fromValue(rawData[0]);
    }

    public int getTypeId() {
        return this.typeId;
    }
    public int getPayloadSize() {
        return this.payloadSize;
    }
}
