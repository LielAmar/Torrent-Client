package project.message.packet.packets;

import project.exceptions.NetworkException;
import project.message.packet.Packet;
import project.message.packet.PacketType;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitFieldPacket extends Packet {

    /*
        BitField Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - +
        | Packet Length | Packet Type | Packet Data |
        + - + - + - + - + - + - + - + - + - + - + - +

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Data    = x bytes
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    private BitSet bitfield;

    public BitFieldPacket() {
        super(PacketType.BITFIELD);

        this.bitfield = null;
    }


    public void setData(BitSet bitfield) {
        this.bitfield = bitfield;
    }

    public BitSet getBitfield() {
        return this.bitfield;
    }


    @Override
    public byte[] build() throws NetworkException {
        if(this.bitfield == null) {
            throw new NetworkException("[BITFIELD PACKET] trying to build a packet with invalid data");
        }

        byte[] bitfieldBytes = this.bitfield.toByteArray();

        int messageLength = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + bitfieldBytes.length;
        int payloadLength = TYPE_FIELD_LENGTH + bitfieldBytes.length;

        byte[] message = new byte[messageLength];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(
                ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(payloadLength).array(), 0,
                message, 0,
                LENGTH_FIELD_LENGTH);

        // Set next 1 byte: the packet type
        message[4] = super.type.getTypeId();

        // Set next x bytes: The piece content
        System.arraycopy(bitfieldBytes, 0,
                message, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH,
                bitfieldBytes.length);

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[0] != super.type.getTypeId()) {
            return false;
        }

        // Calculate the packet's length
        int dataLength = payload.length - TYPE_FIELD_LENGTH;

        // Parse the data
        this.bitfield = BitSet.valueOf(
                ByteBuffer.allocate(dataLength)
                        .put(payload, TYPE_FIELD_LENGTH, dataLength)
                        .rewind()
        );

        return true;
    }
}