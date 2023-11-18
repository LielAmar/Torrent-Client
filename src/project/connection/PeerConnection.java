package project.connection;

import java.net.*;

public abstract class PeerConnection extends Thread {

    protected final Socket connection;

    protected volatile ConnectionState state;

    public PeerConnection(Socket connection, ConnectionState state) {
        this.connection = connection;

        this.state = state;
    }

    public ConnectionState getConnectionState() {
        return state;
    }
}
