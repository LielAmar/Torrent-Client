package project.message.InternalMessage.InternalMessages;

import project.message.InternalMessage.InternalMessage;
import project.message.InternalMessage.InternalMessageType;

public class NewLocalPeiceIntMes extends InternalMessage {
    private int pieceIndex;
    
    public NewLocalPeiceIntMes(int pieceIndex)
    {
        super(InternalMessageType.NEW_LOCAL_PIECE);
        this.pieceIndex = pieceIndex;
    }

    public int GetPieceIndex()
    {
        return pieceIndex;
    }
}
