package project.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.connection.piece.PieceStatus;

public class ConnectionState {

    private final int localPeerId;

    private int remotePeerId;
    private boolean choked;
    private PieceStatus[] pieces;

    private final AtomicBoolean connectionActive;
    private final Lock handshakeLock;

    public ConnectionState(int localPeerId, int remotePeerId) {
        this.localPeerId = localPeerId;

        this.remotePeerId = remotePeerId;
        this.choked = false;
        this.pieces = new PieceStatus[0];

        this.connectionActive = new AtomicBoolean(true);
        this.handshakeLock = new ReentrantLock();
    }


    public int getLocalPeerId() {
        return this.localPeerId;
    }


    public int getRemotePeerId() {
        return this.remotePeerId;
    }

    public void setPeerId(int remotePeerId) {
        this.remotePeerId = remotePeerId;
    }


    public boolean isChoked() {
        return this.choked;
    }

    public void setChoked(boolean choked) {
        this.choked = choked;
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
