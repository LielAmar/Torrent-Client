package project.connection;

import project.peer.Peer;

import java.net.*;

public abstract class PeerConnection extends Thread {

    protected final Socket connection;

    protected volatile Peer peer; // TODO: change to atomic {see TODO in Peer.java}

    public PeerConnection(Socket connection, Peer peer) {
        this.connection = connection;
        this.peer = peer;
    }
}
