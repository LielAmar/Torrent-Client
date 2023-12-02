package project.message.InternalMessage.InternalMessages;

import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessageType;

public class ReceivedIntMes extends InternalMessage {
    private int pieceIndex;
    private byte[] pieceContent;
    private int srcPeerId;    
    public ReceivedIntMes(int pieceIndex, byte[] pieceContent, int srcPeerId)
    {
        super(InternalMessageType.RECEIVED);
        this.pieceContent = pieceContent;
        this.pieceIndex = pieceIndex;
        this.srcPeerId = srcPeerId;
    }

    public int GetPieceIndex()
    {
        return pieceIndex;
    }

    public byte[] GetPieceContent()
    {
        return pieceContent;
    }
    public int GetSourcePeerId()
    {
        return srcPeerId;
    }
}
