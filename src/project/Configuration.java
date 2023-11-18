package project;

public class Configuration {

    private final int numberOfPreferredNeighbors;
    private final int unchokingInterval;
    private final int optimisticUnchokingInterval;
    private final String fileName;
    private final int fileSize;
    private final int pieceSize;

    private final int numberOfPieces;

    public Configuration(int numberOfPreferredNeighbors, int unchokingInterval,
                         int optimisticUnchokingInterval, String fileName, int fileSize, int pieceSize) {

        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;

        this.numberOfPieces = (int) Math.ceil((double) this.fileSize / this.pieceSize);
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

    public int getNumberOfPieces() {
        return numberOfPieces;
    }


    public String toString() {
        return "[CONFIGURATION] - Number of Preferred Neighbors: " + this.getNumberOfPreferredNeighbors() + "\n" +
                "[CONFIGURATION] - Unchoking Interval: " + this.getUnchokingInterval() + "\n" +
                "[CONFIGURATION] - Optimistic Unchoking Interval: " + this.getOptimisticUnchokingInterval() + "\n" +
                "[CONFIGURATION] - File Name: " + this.getFileName() + "\n" +
                "[CONFIGURATION] - File Size: " + this.getFileSize() + "\n" +
                "[CONFIGURATION] - Piece Size: " + this.getPieceSize() + "\n" +
                "[CONFIGURATION] - Number of Pieces: " + this.getNumberOfPieces() + "\n";
    }
}
