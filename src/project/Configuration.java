package project;

import java.util.BitSet;

public class Configuration {

    private final int numberOfPreferredNeighbors;
    private final int unchokingInterval;
    private final int optimisticUnchokingInterval;
    private final String fileName;
    private final int fileSize;
    private final int pieceSize;

    // TODO: move this somewhere else. This is the local bitset that indicates which file parts this peer already has
    private final PieceStatus[] localPieces; // TODO: Change it to an atomic bit set: https://stackoverflow.com/questions/12424633/atomicbitset-implementation-for-java
                                      // so all threads can use it safely

    public Configuration(int numberOfPreferredNeighbors, int unchokingInterval,
                         int optimisticUnchokingInterval, String fileName, int fileSize, int pieceSize) {

        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;

        System.out.println("fileSize: " +fileSize);
        System.out.println("piece size: " + pieceSize);
        System.out.println("number of pieces: " + (int) Math.ceil((double) this.fileSize / this.pieceSize));

        this.localPieces = new PieceStatus[(int) Math.ceil((double) this.fileSize / this.pieceSize)];
    }


    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }


    public PieceStatus[] getLocalPieces() {
        return this.localPieces;
    }

    public void setLocalPiece(int index, PieceStatus status) {
        this.localPieces[index] = status;
    }

    public BitSet piecesStatusToBitset() {
        BitSet bitSet = new BitSet(this.localPieces.length);

        int j = 0;
        for(int i = 0; i < this.localPieces.length; i++) {
            if(this.localPieces[i] == PieceStatus.HAVE) {
                bitSet.set(i);
                j++;
            }
        }

        System.out.println("pieces status to bitset: " + pieceSize + " in j times: " + j);

        return bitSet;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# Prefered Neighbors: " + this.numberOfPreferredNeighbors + "\n");
        sb.append("Unchoking Interval: " + this.unchokingInterval + "\n");
        sb.append("Optimistic Unchoking: " + this.optimisticUnchokingInterval + "\n");
        sb.append("File Name: " + this.fileName + "\n");
        sb.append("File Size: " + this.fileSize + "\n");
        sb.append("Piece Size: " + this.pieceSize + "\n");
        return sb.toString();
    }
}
