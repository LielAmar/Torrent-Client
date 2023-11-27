package project.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.connection.piece.PieceStatus;

public class ConnectionState {

    private int remotePeerId;
    private PieceStatus[] pieces;

    private final AtomicBoolean connectionActive;
    private final AtomicBoolean sentBitfield;

    private final Lock handshakeLock;

    private AtomicBoolean localChoked;
    private AtomicBoolean remoteChoked;

    private AtomicBoolean interested;
    private AtomicBoolean localInterestedIn;
    private AtomicInteger downloadSpeed;

    // A variable used to tell if the local peer connected to this, remote peer, or vice versa.
    private boolean localConnectedToRemote;


    public ConnectionState(int remotePeerId) {
        this.remotePeerId = remotePeerId;
        this.pieces = new PieceStatus[0];

        // Assume that the local peer choked all remote peers, meaning local peer can't send anything to anyone.
        // Every K seconds, there's a reevaluation in which the local peer unchokes certain remote peers, and then its
        // able to start sending them data
        this.localChoked = new AtomicBoolean(true);
        // Assume that all remote peers did not choke local peer, meaning local peer is able to receive data from anyone.
        // Every K seconds, there's a reevaluation in which the remote peers unchoke or choke the local peer, and then the
        // local peer is either able or not able to receive data from the specific remote peer
        this.remoteChoked = new AtomicBoolean(false);

        this.interested = new AtomicBoolean(false);
        this.localInterestedIn = new AtomicBoolean(false);
        this.downloadSpeed = new AtomicInteger(0);

        this.connectionActive = new AtomicBoolean(true);
        this.sentBitfield = new AtomicBoolean(false);

        this.handshakeLock = new ReentrantLock();

        this.localConnectedToRemote = true;
    }


    public int getRemotePeerId() {
        return this.remotePeerId;
    }

    public void setRemotePeerId(int remotePeerId) {
        this.remotePeerId = remotePeerId;
    }


    public boolean isLocalChoked() {
        return this.localChoked.get();
    }

    public void setLocalChoked(boolean localChoked) {
        this.localChoked.set(localChoked);
    }


    public boolean isRemoteChoked() {
        return this.remoteChoked.get();
    }

    public void setRemoteChoke(boolean remoteChoke) {
        this.remoteChoked.set(remoteChoke);
    }


    public boolean isInterested() {
        return this.interested.get();
    }

    public void setInterested(boolean interested) {
        this.interested.set(interested);
    }

    public boolean isLocalInterestedIn() {
        return this.localInterestedIn.get();
    }

    public void setLocalInterestedIn(boolean interested) {
        this.localInterestedIn.set(interested);
    }

    public int getDownloadSpeed() {
        return this.downloadSpeed.get();
    }

    public void resetDownloadSpeed() {
        this.downloadSpeed.set(0);
    }

    public void increaseDownloadSpeed() {
        this.downloadSpeed.incrementAndGet();
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

    public boolean hasSentBitfield() {
        return this.sentBitfield.get();
    }

    public void setSentBitfield(boolean sent) {
        this.sentBitfield.set(sent);
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


    public boolean isLocalConnectedToRemote() {
        return this.localConnectedToRemote;
    }

    public void setLocalConnectedToRemote(boolean choice) {
        this.localConnectedToRemote = choice;
    }
}
