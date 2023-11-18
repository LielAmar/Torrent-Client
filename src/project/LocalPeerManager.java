package project;

import project.connection.ConnectionState;
import project.connection.PeerConnectionManager;
import project.connection.piece.Piece;
import project.connection.piece.PieceStatus;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LocalPeerManager {

    private final int localId;

    private final Piece[] localPieces;
    private final ArrayList<PeerConnectionManager> connectedPeers;
    private final Lock choosePieceLock;


    public LocalPeerManager(int localId, int numberOfPieces) {
        this.localId = localId;
        this.localPieces = new Piece[numberOfPieces];
        this.connectedPeers = new ArrayList<>();
        this.choosePieceLock = new ReentrantLock();
    }


    public Piece[] getLocalPieces() {
        return this.localPieces;
    }

    public void setLocalPiece(int pieceId, PieceStatus status, byte[] content) {
        System.out.println("[LOCAL PEER] Setting piece to status " + status.name() + ((content == null) ? " without " : " with " + "content"));

        if(this.localPieces[pieceId] == null) {
            this.localPieces[pieceId] = new Piece(status, content);
        } else {
            this.localPieces[pieceId].setStatus(status);
            this.localPieces[pieceId].setContent(content);
        }
    }


    public PeerConnectionManager connectToNewPeer(Socket socket) {
        return this.connectToNewPeer(-1, socket);
    }

    public PeerConnectionManager connectToNewPeer(int remotePeerId, Socket socket) {
        System.out.println("[LOCAL PEER] Creating a connection manager with peer " + remotePeerId);

        ConnectionState state = new ConnectionState(this.localId, remotePeerId);
        PeerConnectionManager manager = new PeerConnectionManager(socket, state);
        this.connectedPeers.add(manager);
        return manager;
    }

    public PeerConnectionManager getConnectionManagerById(int peerId) {
        return this.connectedPeers
                .stream()
                .filter(manager -> manager.getConnectionState().getRemotePeerId() == peerId)
                .findFirst()
                .orElse(null);
    }

    public int choosePiece(Piece[] remotePieces) {
        this.choosePieceLock.lock();
        ArrayList<Integer> desired = new ArrayList<>();
        for (int i = 0; i < remotePieces.length; i++) {
            if(remotePieces[i].getStatus() == PieceStatus.HAVE  && this.localPieces[i].getStatus() == PieceStatus.NOT_HAVE){
                desired.add(i);
            }
        }
        Random random = new Random();
        int randomIndex = random.nextInt(desired.size());
        this.localPieces[desired.get(randomIndex)].setStatus(PieceStatus.REQUESTED);

        this.choosePieceLock.unlock();
        return desired.get(randomIndex);
    }
}
