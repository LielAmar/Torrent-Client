package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class PiecePacket extends Packet {

    /*
        Piece Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - + -
        | Packet Length | Packet Type | Packet Payload |
        + - + - + - + - + - + - + - + - + - + - + - + -

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Payload = 4 + Piece size
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    private byte[] payload;

    public PiecePacket() {
        super(PacketType.PIECE);
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public byte[] build() throws NetworkException {
        if(this.payload == null) {
            throw new NetworkException("[PIECE PACKET] trying to build a packet with null payload");
        }

        byte[] message = new byte[LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + this.payload.length];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(TYPE_FIELD_LENGTH + this.payload.length).array(), 0,
                message, 0, LENGTH_FIELD_LENGTH);

        // Set the packet type field
        message[4] = super.type.getTypeId();

        // Set the packet's payload data
        System.arraycopy(this.payload, 0, message, 5, this.payload.length);

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[4] != super.type.getTypeId()) {
            return false;
        }

        ByteBuffer lengthBuffer = ByteBuffer.allocate(LENGTH_FIELD_LENGTH).put(payload,  0, LENGTH_FIELD_LENGTH);
        lengthBuffer.rewind();
        int length = lengthBuffer.getInt();
        int payloadLength = length - 1;

        // TODO: make sure this is a correct way to parse
        this.payload = new byte[payloadLength];
        System.arraycopy(payload, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH,
                this.payload, 0, payloadLength);
        return true;
    }
}
