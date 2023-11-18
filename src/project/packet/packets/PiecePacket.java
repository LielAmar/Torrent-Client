package project.packet.packets;

import project.exceptions.NetworkException;
import project.packet.Packet;
import project.packet.PacketType;

import java.nio.ByteBuffer;

public class PiecePacket extends Packet {

    /*
        Piece Packet Structure

        + - + - + - + - + - + - + - + - + - + - + - +
        | Packet Length | Packet Type | Packet Data |
        + - + - + - + - + - + - + - + - + - + - + - +

        Where:
        - Packet Length = 4 bytes
        - Packet Type   = 1 byte
        - Packet Data   = 4 + x bytes (Piece Index + Piece Content)
     */

    protected static final int LENGTH_FIELD_LENGTH = 4;
    protected static final int TYPE_FIELD_LENGTH = 1;
    protected static final int PIECE_INDEX_FIELD_LENGTH = 4;

    private int pieceIndex;
    private byte[] pieceContent;

    public PiecePacket() {
        super(PacketType.PIECE);

        this.pieceIndex = -1;
        this.pieceContent = null;
    }

    public void setData(int pieceIndex, byte[] pieceContent) {
        this.pieceIndex = pieceIndex;
        this.pieceContent = pieceContent;
    }

    public byte[] getPieceContent() {
        return this.pieceContent;
    }

    public int getPieceIndex() {
        return this.pieceIndex;
    }


    @Override
    public byte[] build() throws NetworkException {
        if(this.pieceContent == null || this.pieceIndex == -1) {
            throw new NetworkException("[PIECE PACKET] trying to build a packet with invalid data");
        }

        int messageLength = LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH + this.pieceContent.length;
        int payloadLength = TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH + this.pieceContent.length;

        byte[] message = new byte[messageLength];

        // Set first 4 bytes: the length of the payload + the packet type field
        System.arraycopy(
                ByteBuffer.allocate(LENGTH_FIELD_LENGTH).putInt(payloadLength).array(), 0,
                message, 0,
                LENGTH_FIELD_LENGTH);

        // Set next 1 byte: the packet type
        message[LENGTH_FIELD_LENGTH] = super.type.getTypeId();

        // Set next 4 bytes: the piece index
        System.arraycopy(
                ByteBuffer.allocate(PIECE_INDEX_FIELD_LENGTH).putInt(this.pieceIndex).array(), 0,
                message, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH,
                PIECE_INDEX_FIELD_LENGTH);

        // Set next x bytes: the piece content
        System.arraycopy(this.pieceContent, 0,
                message, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH,
                this.pieceContent.length);

        return message;
    }

    @Override
    public boolean parse(byte[] payload) {
        if(payload[0] != super.type.getTypeId()) {
            return false;
        }

        // Calculate the packet's length
        int dataLength = payload.length - TYPE_FIELD_LENGTH;
        int contentLength = dataLength - PIECE_INDEX_FIELD_LENGTH;

        // Parse the piece index
        ByteBuffer pieceIndexBuffer = ByteBuffer.allocate(PIECE_INDEX_FIELD_LENGTH)
                .put(payload,  LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH, PIECE_INDEX_FIELD_LENGTH);
        pieceIndexBuffer.rewind();
        this.pieceIndex = pieceIndexBuffer.getInt();

        // Parse the piece content
        this.pieceContent = new byte[contentLength];
        System.arraycopy(payload, LENGTH_FIELD_LENGTH + TYPE_FIELD_LENGTH + PIECE_INDEX_FIELD_LENGTH,
                this.pieceContent, 0, contentLength);

        return true;
    }
}
