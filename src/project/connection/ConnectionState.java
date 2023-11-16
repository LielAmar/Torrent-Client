package project.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.peer.Peer;

public class ConnectionState {

    private final Peer peer;
    private final int localId;

    private AtomicBoolean connectionActive;
    private Lock handshakeLock;

    public ConnectionState(int localId, Peer peer) {
        this.peer = peer;
        this.localId = localId;

        this.connectionActive = new AtomicBoolean(true);
        this.handshakeLock = new ReentrantLock();
    }

    public int getLocalId() {
        return this.localId;
    }

    public int getPeerId() {
        return this.peer.getPeerId();
    }

    public void setPeerId(int newId) {
        this.peer.setPeerId(newId);
    }

    public boolean getConnectionActive() {
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
