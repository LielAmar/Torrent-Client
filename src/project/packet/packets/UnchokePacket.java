package project.packet.packets;

import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class UnchokePacket extends Packet{

    /*
        Unchoke Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - +
        | Packet Length | Packet Type | Packet Data |
        + - + - + - + - + - + - + - + - + - + - + - +

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Data    = 0 bytes
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    public UnchokePacket() {
        super(PacketType.UNCHOKE);
    }

    @Override
    public byte[] build() {
        int messageLength = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH;
        int payloadLength = TYPE_FIELD_LENGTH;

        byte[] message = new byte[messageLength];

        // Set first 4 bytes: the length of the packet type field
        System.arraycopy(
                ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(payloadLength).array(), 0,
                message, 0,
                LENGTH_FIELD_LENGTH);

        // Set next 1 byte: the packet type
        message[4] = super.type.getTypeId();

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        return payload[0] == super.type.getTypeId();
    }
}
