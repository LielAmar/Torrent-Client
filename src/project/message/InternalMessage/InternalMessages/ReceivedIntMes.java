package project.message.InternalMessage.InternalMessages;

import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessageType;

public class ReceivedIntMes extends InternalMessage {
    private int pieceIndex;
    private byte[] pieceContent;

    public ReceivedIntMes(int pieceIndex) {
        this(pieceIndex, null);
    }

    public ReceivedIntMes(int pieceIndex, byte[] pieceContent)
    {
        super(InternalMessageType.RECEIVED);
        this.pieceContent = pieceContent;
        this.pieceIndex = pieceIndex;
    }

    public int GetPieceIndex()
    {
        return pieceIndex;
    }

    public byte[] GetPieceContent()
    {
        return pieceContent;
    }
}
