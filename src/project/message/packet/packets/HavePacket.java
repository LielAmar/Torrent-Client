package project.message.packet.packets;

import project.exceptions.NetworkException;
import project.message.packet.Packet;
import project.message.packet.PacketType;

import java.nio.ByteBuffer;

public class HavePacket extends Packet {

    /*
        Have Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - +
        | Packet Length | Packet Type | Packet Data |
        + - + - + - + - + - + - + - + - + - + - + - +

        Where:
        - Packet Length  = 4 bytes
        - Packet Type    = 1 byte
        - Packet Payload = 4 bytes
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;
    protected static final int PIECE_INDEX_FIELD_LENGTH = 4;

    private int pieceIndex;

    public HavePacket() {
        super(PacketType.HAVE);

        this.pieceIndex = -1;
    }


    public void setData(int pieceIndex) {
        this.pieceIndex = pieceIndex;
    }

    public int getPieceIndex() {
        return this.pieceIndex;
    }


    @Override
    public byte[] build() throws NetworkException {
        if(this.pieceIndex == -1) {
            throw new NetworkException("[HAVE PACKET] trying to build a packet with invalid data");
        }

        int messageLength = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH;
        int payloadLength = TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH;

        byte[] message = new byte[messageLength];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(
                ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(payloadLength).array(), 0,
                message, 0,
                LENGTH_FIELD_LENGTH);

        // Set next 1 byte: the packet type
        message[4] = super.type.getTypeId();

        // Set next 4 bytes: the piece index
        System.arraycopy(
                ByteBuffer.allocate(PIECE_INDEX_FIELD_LENGTH).putInt(this.pieceIndex).array(), 0,
                message, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH,
                PIECE_INDEX_FIELD_LENGTH);

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[0] != super.type.getTypeId()) {
            return false;
        }

        // Calculate the packet's length
        int dataLength = payload.length - TYPE_FIELD_LENGTH;

        // Parse the piece index
        ByteBuffer pieceIndexBuffer = ByteBuffer.allocate(PIECE_INDEX_FIELD_LENGTH)
                .put(payload, TYPE_FIELD_LENGTH, PIECE_INDEX_FIELD_LENGTH);
        pieceIndexBuffer.rewind();
        this.pieceIndex = pieceIndexBuffer.getInt();

        return true;
    }
}
