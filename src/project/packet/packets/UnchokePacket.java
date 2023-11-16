package project.packet.packets;

import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class UnchokePacket extends Packet{

    /*
        Unchoke Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - + -
        | Packet Length | Packet Type | Packet Payload |
        + - + - + - + - + - + - + - + - + - + - + - + -

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Payload = 0
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    public UnchokePacket() {
        super(PacketType.UNCHOKE);
    }

    @Override
    public byte[] build() {
        byte[] message = new byte[LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH];

        // Set first 4 bytes: the length of the packet type field
        System.arraycopy(ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(TYPE_FIELD_LENGTH).array(), 0,
                message, 0, LENGTH_FIELD_LENGTH);

        // Set the packet type field
        message[4] = super.type.getTypeId();

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        return payload[0] == super.type.getTypeId();
    }
}
