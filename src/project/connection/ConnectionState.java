package project.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.connection.piece.PieceStatus;

public class ConnectionState {

    private final int localPeerId;

    private int remotePeerId;
    private PieceStatus[] pieces;

    private boolean localChoked;
    private boolean remoteChoke;
    private boolean interested;
    private int downloadSpeed;

    private final AtomicBoolean connectionActive;
    private final Lock handshakeLock;


    public ConnectionState(int localPeerId, int remotePeerId) {
        this.localPeerId = localPeerId;

        this.remotePeerId = remotePeerId;
        this.pieces = new PieceStatus[0];

        // Assume that the local peer choked all remote peers, meaning local peer can't send anything to anyone.
        // Every K seconds, there's a reevaluation in which the local peer unchokes certain remote peers, and then its
        // able to start sending them data
        this.localChoked = true;
        // Assume that all remote peers did not choke local peer, meaning local peer is able to receive data from anyone.
        // Every K seconds, there's a reevaluation in which the remote peers unchoke or choke the local peer, and then the
        // local peer is either able or not able to receive data from the specific remote peer
        this.remoteChoke = false;
        this.interested = false;
        this.downloadSpeed = 0;

        this.connectionActive = new AtomicBoolean(true);
        this.handshakeLock = new ReentrantLock();
    }


    public int getLocalPeerId() {
        return this.localPeerId;
    }


    public int getRemotePeerId() {
        return this.remotePeerId;
    }

    public void setRemotePeerId(int remotePeerId) {
        this.remotePeerId = remotePeerId;
    }


    public boolean isLocalChoked() {
        return localChoked;
    }

    public void setLocalChoked(boolean localChoked) {
        this.localChoked = localChoked;
    }


    public boolean isRemoteChoke() {
        return remoteChoke;
    }

    public void setRemoteChoke(boolean remoteChoke) {
        this.remoteChoke = remoteChoke;
    }


    public boolean isInterested() {
        return interested;
    }

    public void setInterested(boolean interested) {
        this.interested = interested;
    }


    public int getDownloadSpeed(){
        return this.downloadSpeed;
    }

    public void resetDownloadSpeed(){
        this.downloadSpeed = 0;
    }

    public void addDownloadSpeed(){
        this.downloadSpeed++;
    }


    public PieceStatus[] getPieces() {
        return this.pieces;
    }

    public void setPieces(PieceStatus[] pieces) {
        this.pieces = pieces;
    }

    public void updatePiece(int ind) {
        this.pieces[ind] = PieceStatus.HAVE;
    }


    public boolean isConnectionActive() {
        return this.connectionActive.get();
    }

    public void setConnectionActive(boolean state) {
        this.connectionActive.set(state);
    }


    public void waitForHandshake() {
        this.handshakeLock.lock();
    }

    public void lockHandshake() {
        this.handshakeLock.lock();
    }

    public void unlockHandshake() {
        this.handshakeLock.unlock();
    }
}
