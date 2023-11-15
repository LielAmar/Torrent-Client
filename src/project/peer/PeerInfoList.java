package project.peer;

import project.connection.ConnectionState;

import java.util.ArrayList;

public class PeerInfoList {

    private ArrayList<ConnectionState> connectedPeers;
    private final int localId;
    public PeerInfoList(int localId)
    {
        this.localId = localId;
        this.connectedPeers = new ArrayList<ConnectionState>();
    }

    public ConnectionState NewPeer()
    {
        return this.NewPeer(-1);
    }
    public ConnectionState NewPeer(int peerId)
    {
        ConnectionState newConnection = new ConnectionState(localId, new Peer(peerId));
        connectedPeers.add(newConnection);
        return newConnection;
    }

    
}
