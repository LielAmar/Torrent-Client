package project.connection;

import project.LocalPeerManager;

import java.net.*;

public abstract class PeerConnection extends Thread {

    protected final Socket connection;

    protected LocalPeerManager localPeerManager;
    protected ConnectionState state;

    public PeerConnection(Socket connection, LocalPeerManager localPeerManager, ConnectionState state) {
        this.connection = connection;

        this.localPeerManager = localPeerManager;
        this.state = state;
    }


    public LocalPeerManager getLocalPeerManager() {
        return this.localPeerManager;
    }

    public ConnectionState getConnectionState() {
        return this.state;
    }
}
