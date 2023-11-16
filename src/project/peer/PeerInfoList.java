package project.peer;

import project.connection.ConnectionState;

import java.util.ArrayList;

public class PeerInfoList {

    private final int localId;

    private final ArrayList<ConnectionState> connectedPeers;

    public PeerInfoList(int localId) {
        this.localId = localId;

        this.connectedPeers = new ArrayList<>();
    }

    public ConnectionState connectToNewPeer() {
        return this.connectToNewPeer(-1);
    }

    public ConnectionState connectToNewPeer(int peerId) {
        ConnectionState newConnection = new ConnectionState(localId, new Peer(peerId));
        this.connectedPeers.add(newConnection);
        return newConnection;
    }

    public ConnectionState getConnectionById(int peerId) {
        return this.connectedPeers
                .stream()
                .filter(con -> con.getPeerId() == peerId)
                .findFirst()
                .orElse(null);
    }
}
