package project.packet.packets;

import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitFieldPacket extends Packet {

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;

    private BitSet payload;

    public BitFieldPacket() {
        super(PacketType.BITFIELD);
    }

    public void setPayload(BitSet payload) {
        this.payload = payload;
    }

    @Override
    public byte[] build() {
        byte[] message = new byte[LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + this.payload.length()];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(this.payload.length() + 1).array(), 0,
                message, 0, LENGTH_FIELD_LENGTH);

        // Set the packet type field
        message[4] = super.type.getTypeId();

        // Set the packet's payload data
        System.arraycopy(ByteBuffer.allocate(this.payload.length()).put(this.payload.toByteArray()).array(), 0,
                message, 5, this.payload.length());

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[4] != PacketType.BITFIELD.getTypeId()) {
            return false;
        }

        ByteBuffer lengthBuffer = ByteBuffer.allocate(LENGTH_FIELD_LENGTH).put(payload,  0, LENGTH_FIELD_LENGTH);
        lengthBuffer.rewind();
        int length = lengthBuffer.getInt();
        int payloadLength = length - 1;

        // TODO: make sure this is a correct way to parse
        this.payload = BitSet.valueOf(ByteBuffer.allocate(payloadLength).put(payload,
                LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + payloadLength).rewind());
        return true;
    }
}