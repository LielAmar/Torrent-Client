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
    private final BitSet localBitSet; // TODO: Change it to an atomic bit set: https://stackoverflow.com/questions/12424633/atomicbitset-implementation-for-java
                                      // so all threads can use it safely

    public Configuration(int numberOfPreferredNeighbors, int unchokingInterval,
                         int optimisticUnchokingInterval, String fileName, int fileSize, int pieceSize) {

        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;

        this.localBitSet = new BitSet(this.fileSize / this.pieceSize);
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

    public BitSet getLocalBitSet() {
        return localBitSet;
    }

    public void setLocalBitSet() {
        this.localBitSet.set(0, this.fileSize / this.pieceSize);
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
