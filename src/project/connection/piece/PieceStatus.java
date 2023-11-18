package project.connection.piece;

import java.util.BitSet;

public enum PieceStatus {

    HAVE,
    NOT_HAVE,
    REQUESTED;

    /**
     * Converts a list of pieces into a bitset
     *
     * @param pieces   Pieces list to convert
     * @return         Convert bitset
     */
    public static BitSet piecesToBitset(Piece[] pieces) {
        BitSet bitSet = new BitSet(pieces.length);

        for(int i = 0; i < pieces.length; i++) {
            if(pieces[i].getStatus() == PieceStatus.HAVE) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    /**
     * Converts a bitset into a list of pieces of size max(numberOfPieces, bitSet.length())
     *
     * @param bitSet   Bitset to convert
     * @return         Convert pieces list
     */
    public static PieceStatus[] bitsetToPiecesStatus(BitSet bitSet, int numberOfPieces) {
        numberOfPieces = Math.max(numberOfPieces, bitSet.length());

        PieceStatus[] pieces = new PieceStatus[numberOfPieces];

        for(int i = 0; i < bitSet.length(); i++) {
            pieces[i] = bitSet.get(i) ? PieceStatus.HAVE : PieceStatus.NOT_HAVE;
        }

        for(int i = bitSet.length(); i < pieces.length; i++) {
            pieces[i] = PieceStatus.NOT_HAVE;
        }
        return pieces;
    }
}
