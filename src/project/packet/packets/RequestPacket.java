package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class RequestPacket extends Packet {

    /*
        Request Packet Structure

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

    protected static final int PAYLOAD_FIELD_LENGTH = 4;

    private byte[] payload;

    public RequestPacket() {
        super(PacketType.REQUEST);
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public byte[] build() throws NetworkException {
        if(this.payload == null) {
            throw new NetworkException("[REQUEST PACKET] trying to build a packet with null payload");
        }

        byte[] message = new byte[LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + PAYLOAD_FIELD_LENGTH];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(TYPE_FIELD_LENGTH + PAYLOAD_FIELD_LENGTH).array(), 0,
                message, 0, LENGTH_FIELD_LENGTH);

        // Set the packet type field
        message[4] = super.type.getTypeId();

        // Set the packet's payload data
        System.arraycopy(this.payload, 0, message, 5, PAYLOAD_FIELD_LENGTH);

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[4] != super.type.getTypeId()) {
            return false;
        }

        // TODO: make sure this is a correct way to parse
        this.payload = new byte[PAYLOAD_FIELD_LENGTH];
        System.arraycopy(payload, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH,
                this.payload, 0, PAYLOAD_FIELD_LENGTH);
        return true;
    }
}
