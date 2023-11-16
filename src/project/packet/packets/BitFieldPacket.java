package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitFieldPacket extends Packet {

    /*
        BitField Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - + -
        | Packet Length | Packet Type | Packet Payload |
        + - + - + - + - + - + - + - + - + - + - + - + -

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Payload = ~Number of pieces
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    private BitSet payload;

    public BitFieldPacket() {
        super(PacketType.BITFIELD);
    }


    public void setPayload(BitSet payload) {
        this.payload = payload;
    }

    public BitSet getPayload() {
        return this.payload;
    }


    @Override
    public byte[] build() throws NetworkException {
        if(this.payload == null) {
            throw new NetworkException("[BITFIELD PACKET] trying to build a packet with null payload");
        }

        // Build the message that would be sent over connection
        byte[] payloadBytes = this.payload.toByteArray();

        byte[] message = new byte[LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + payloadBytes.length];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(TYPE_FIELD_LENGTH + payloadBytes.length).array(), 0,
                message, 0, LENGTH_FIELD_LENGTH);

        // Set the packet type field
        message[4] = super.type.getTypeId();

        // Set the packet's payload data
        System.arraycopy(payloadBytes, 0, message, 5, payloadBytes.length);

        return message;
    }

    @Override
    public boolean parse(byte[] message) {
        // TODO: change all parses to assume payload doesn't have the length (first 4 bytes).
        // This means that the given payload[0] is the message type
        if(message[0] != super.type.getTypeId()) {
            return false;
        }

        int length = message.length;
        System.out.println("length: " + length);
        int payloadLength = length - 1;
        System.out.println("payload length: " + payloadLength);

        // TODO: make sure this is a correct way to parse
        this.payload = BitSet.valueOf(ByteBuffer.allocate(payloadLength).put(message,
                TYPE_FIELD_LENGTH, TYPE_FIELD_LENGTH + payloadLength).rewind());
        return true;
    }
}