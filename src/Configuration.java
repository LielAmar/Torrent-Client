public class Configuration {

    private final int processPeerId;

    private final int numberOfPreferredNeighbors;
    private final int unchokingInterval;
    private final int optimisticUnchokingInterval;
    private final String fileName;
    private final int fileSize;
    private final int pieceSize;

    public Configuration(int processPeerId,
                         int numberOfPreferredNeighbors, int unchokingInterval,
                         int optimisticUnchokingInterval, String fileName, int fileSize, int pieceSize) {
        this.processPeerId = processPeerId;

        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
    }


    public int getProcessPeerId() {
        return processPeerId;
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
