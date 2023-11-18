package project.connection.piece;

public class Piece {

    private PieceStatus status;
    private byte[] content;

    public Piece(PieceStatus status, byte[] content) {
        this.status = status;
        this.content = content;
    }


    public PieceStatus getStatus() {
        return status;
    }

    public void setStatus(PieceStatus status) {
        this.status = status;
    }


    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
