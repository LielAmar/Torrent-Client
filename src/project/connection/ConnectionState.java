package project.connection;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import project.peer.Peer;

public class ConnectionState {
    private final Peer peer;
    private final int localId;
    private AtomicBoolean connectionActive;
    // private AtomicBoolean handshakeRecieved;
    private Lock handshakeLock;

    public ConnectionState(int localId, Peer peer)
    {
        this.peer = peer;
        this.localId = localId;
        connectionActive = new AtomicBoolean(true);
        // handshakeRecieved = new AtomicBoolean(false);
        handshakeLock = new ReentrantLock();
    }
    
    public int getLocalId()
    {
        return localId;
    }

    public int getPeerId()
    {
        return peer.getPeerId();
    }

    public void setPeerId(int newId)
    {
        peer.setPeerId(newId);
    }

    public boolean getConnectionActive()
    {
        return connectionActive.get();
    }

    public void setConnectionActive(boolean state)
    {
        connectionActive.set(state);
    }

    // public boolean gethandshakeRecieved()
    // {
    //     return handshakeRecieved.get();
    // }

    // public void setHandshakeRecieved(boolean state)
    // {
    //     handshakeRecieved.set(state);
        
    // }

    public void waitForHandshake()
    {
        handshakeLock.lock();
    }

    public void lockHandshake()
    {
        handshakeLock.lock();
    }

    public void unlockHandshake()
    {
        handshakeLock.unlock();
    }
}
